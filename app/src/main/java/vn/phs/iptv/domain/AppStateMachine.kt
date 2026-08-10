package vn.phs.iptv.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import vn.phs.iptv.BuildConfig
import vn.phs.iptv.data.IptvRepository
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.ScreenResponse
import javax.inject.Inject

// Drives top-level navigation from the Screen poll; FCM is intentionally out of v1.
@HiltViewModel
class AppStateMachine @Inject constructor(
    private val iptvRepository: IptvRepository,
) : ViewModel() {

    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Loading)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _currentGuest = MutableStateFlow<GuestProfile?>(null)
    val currentGuest: StateFlow<GuestProfile?> = _currentGuest.asStateFlow()

    private val _currentStayId = MutableStateFlow<String?>(null)
    val currentStayId: StateFlow<String?> = _currentStayId.asStateFlow()

    private val _introVideoUrl = MutableStateFlow<String?>(null)
    val introVideoUrl: StateFlow<String?> = _introVideoUrl.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.EN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _screenData = MutableStateFlow<ScreenResponse?>(null)
    val screenData: StateFlow<ScreenResponse?> = _screenData.asStateFlow()

    private val _contentData = MutableStateFlow<ContentResponse?>(null)
    val contentData: StateFlow<ContentResponse?> = _contentData.asStateFlow()

    private var screenPollJob: Job? = null
    private var contentPollJob: Job? = null
    private var playedIntroStayId: String? = null

    init {
        viewModelScope.launch {
            if (iptvRepository.hasDeviceToken()) {
                _screen.value = AppScreen.Idle
                startRunningPolls()
            } else {
                _screen.value = AppScreen.Provisioning
            }
        }
    }

    // ── Transition callbacks ──────────────────────────────────────────────────

    fun onProvisioned() {
        _screen.value = AppScreen.Idle
        startRunningPolls()
    }

    private fun startRunningPolls() {
        startScreenPolling()
        startContentPolling()
    }

    private fun startScreenPolling() {
        screenPollJob?.cancel()
        screenPollJob = viewModelScope.launch {
            while (isActive) {
                try {
                    applyScreen(iptvRepository.screen())
                } catch (e: Exception) {
                    when (iptvRepository.apiError(e)?.mess) {
                        "systemalert.iptv.auth.denied" -> {
                            resetToProvisioning()
                            return@launch
                        }
                        "systemalert.iptv.device.invalid" -> {
                            resetToProvisioning()
                            return@launch
                        }
                        "systemalert.iptv.disabled" -> {
                            clearGuestSession()
                            _screen.value = AppScreen.Idle
                        }
                        // Timeout/offline/server errors keep the current safe UI.
                        else -> Unit
                    }
                }
                delay(20_000)
            }
        }
    }

    private fun startContentPolling() {
        contentPollJob?.cancel()
        contentPollJob = viewModelScope.launch {
            while (isActive) {
                try {
                    applyContent(iptvRepository.content())
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Content poll failed: ${e.javaClass.simpleName}")
                    }
                    when (iptvRepository.apiError(e)?.mess) {
                        "systemalert.iptv.auth.denied",
                        "systemalert.iptv.device.invalid" -> {
                            resetToProvisioning()
                            return@launch
                        }
                        // Screen owns disabled/occupancy state. Network and 5xx errors
                        // keep the last in-memory Content and retry after ten minutes.
                        else -> Unit
                    }
                }
                delay(10 * 60_000L)
            }
        }
    }

    private fun applyContent(response: ContentResponse) {
        if (_contentData.value?.version == response.version) return
        _contentData.value = response
        if (BuildConfig.DEBUG) Log.d(TAG, "Content updated: version=${response.version}")
        if (_currentGuest.value != null) {
            _introVideoUrl.value = response.video.introUrl.takeIf { it.isNotBlank() }
        }
    }

    private suspend fun resetToProvisioning() {
        iptvRepository.clearDeviceToken()
        contentPollJob?.cancel()
        _contentData.value = null
        clearGuestSession()
        _screen.value = AppScreen.Provisioning
    }

    private fun applyScreen(response: ScreenResponse) {
        val wasOccupied = _screenData.value?.occupied == true
        val stay = response.stay
        if (!response.occupied || stay == null) {
            _screenData.value = response.copy(stay = null, folio = null, qrUrl = null)
            clearGuestSession(clearScreenData = false)
            _screen.value = AppScreen.Idle
            return
        }

        _screenData.value = response
        _currentGuest.value = GuestProfile(
            name = stay.guestName,
            title = "",
            roomNo = response.roomNo,
            nationality = "",
            isBirthday = false,
        )
        _currentStayId.value = "${response.roomNo}:${stay.arrival}:${stay.departure}"
        _introVideoUrl.value = _contentData.value?.video?.introUrl?.takeIf { it.isNotBlank() }

        if (!wasOccupied || _screen.value == AppScreen.Idle || _screen.value == AppScreen.Loading) {
            _screen.value = AppScreen.Welcome
        }
    }

    private fun clearGuestSession(clearScreenData: Boolean = true) {
        _currentGuest.value = null
        _currentStayId.value = null
        _introVideoUrl.value = null
        playedIntroStayId = null
        if (clearScreenData) _screenData.value = null
    }

    // Called from the combined Welcome+Language screen (post check-in) once the guest
    // picks a language. Streamlined flow: greet+pick is one screen, then the intro video
    // plays only on the first check-in of a stay (idempotent per PRD §10.4) — a re-check-in
    // goes straight to the hub.
    //
    // A blank introUrl no longer skips the screen: IntroVideoScreen falls back to the
    // bundled ambient film, so every arriving guest gets an intro regardless of whether the
    // branch has uploaded its own.
    fun onWelcomeLanguageSelected(language: AppLanguage) {
        _language.value = language
        val stayId = _currentStayId.value
        val shouldPlay = !stayId.isNullOrEmpty() && stayId != playedIntroStayId
        _screen.value = if (shouldPlay) AppScreen.IntroVideo else AppScreen.Home
    }

    // Called when intro video ends or Back/OK skips it.
    // Marks stayId played so the video won't replay this stay, then enters the hub.
    fun onIntroVideoEnded() {
        playedIntroStayId = _currentStayId.value
        _screen.value = AppScreen.Home
    }

    // Called when the guest re-selects a language from Home — remember it and return.
    fun onLanguageSelected(language: AppLanguage) {
        _language.value = language
        _screen.value = AppScreen.Home
    }

    private val _themeMode = MutableStateFlow(vn.phs.iptv.ui.theme.AppThemeMode.DARK)
    val themeMode: StateFlow<vn.phs.iptv.ui.theme.AppThemeMode> = _themeMode.asStateFlow()

    fun toggleTheme() {
        _themeMode.value = if (_themeMode.value == vn.phs.iptv.ui.theme.AppThemeMode.DARK) {
            vn.phs.iptv.ui.theme.AppThemeMode.LIGHT
        } else {
            vn.phs.iptv.ui.theme.AppThemeMode.DARK
        }
    }

    fun navigateTo(screen: AppScreen) {
        _screen.value = screen
    }

    override fun onCleared() {
        screenPollJob?.cancel()
        contentPollJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "AppStateMachine"
    }
}
