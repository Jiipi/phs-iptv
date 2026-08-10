package vn.phs.iptv.ui.assistant

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.domain.AppScreen
import vn.phs.iptv.ui.apps.TvAppCatalog
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data object Listening : VoiceUiState
    data class Thinking(val question: String) : VoiceUiState
    data class Speaking(val question: String, val answer: String) : VoiceUiState
    data class Error(val message: String) : VoiceUiState
}

sealed interface AssistantAction {
    data class Navigate(val destination: AppScreen) : AssistantAction
    data class LaunchApp(val packageName: String, val appName: String) : AssistantAction
}

internal data class AssistantReply(
    val answer: String,
    val action: AssistantAction?,
)

/**
 * Fast, offline command routing for actions the TV can safely perform itself.
 * Hotel Q&A and write operations remain server responsibilities; no API key belongs in the APK.
 */
internal object AssistantCommandRouter {
    private val marks = Regex("\\p{Mn}+")
    private val separators = Regex("[^a-z0-9а-яё]+")

    fun resolve(text: String, language: AppLanguage): AssistantReply {
        val command = normalize(text)

        if (containsAny(command, "trang chu", "home", "главн")) {
            return navigate(language, AppScreen.Home, "Đang về trang chủ.", "Going home.", "Открываю главный экран.")
        }

        TvAppCatalog.matchingVoiceApp(command)?.let { app ->
            return AssistantReply(
                answer = localized(
                    language,
                    "Đang mở ${app.title}.",
                    "Opening ${app.title}.",
                    "Открываю ${app.title}.",
                ),
                action = AssistantAction.LaunchApp(app.packageName, app.title),
            )
        }

        if (containsAny(command, "hoa don", "folio", "bill", "счет", "счёт")) {
            return navigate(language, AppScreen.Folio, "Đang mở hóa đơn phòng.", "Opening your room bill.", "Открываю счёт за номер.")
        }
        if (containsAny(command, "goi mon", "dat mon", "dich vu phong", "thuc don", "order", "coffee", "room service", "меню", "заказ")) {
            return navigate(language, AppScreen.Order, "Đang mở dịch vụ phòng.", "Opening room service.", "Открываю обслуживание номера.")
        }
        if (containsAny(command, "tien ich", "dich vu khach san", "an sang", "breakfast", "spa", "gym", "pool", "завтрак", "услуг")) {
            return navigate(language, AppScreen.Services, "Đang mở tiện ích khách sạn.", "Opening hotel services.", "Открываю услуги отеля.")
        }
        if (containsAny(command, "wifi", "wi fi", "mat khau", "le tan", "tra phong", "check out", "help", "reception", "пароль", "помощ")) {
            return navigate(language, AppScreen.Help, "Đang mở thông tin khách sạn và Wi-Fi.", "Opening hotel and Wi-Fi information.", "Открываю информацию об отеле и Wi-Fi.")
        }
        if (containsAny(command, "ngon ngu", "language", "язык")) {
            return navigate(language, AppScreen.Language, "Đang mở chọn ngôn ngữ.", "Opening language selection.", "Открываю выбор языка.")
        }

        return AssistantReply(
            answer = localized(
                language,
                "Hiện tôi có thể mở ứng dụng TV, hóa đơn, dịch vụ phòng, tiện ích, Wi-Fi và ngôn ngữ.",
                "I can currently open TV apps, your bill, room service, hotel services, Wi-Fi and language settings.",
                "Сейчас я могу открыть ТВ-приложения, счёт, обслуживание номера, услуги отеля, Wi-Fi и язык.",
            ),
            action = null,
        )
    }

    private fun navigate(
        language: AppLanguage,
        destination: AppScreen,
        vi: String,
        en: String,
        ru: String,
    ) = AssistantReply(localized(language, vi, en, ru), AssistantAction.Navigate(destination))

    private fun containsAny(command: String, vararg terms: String): Boolean =
        terms.any { term -> command.contains(term) }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(marks, "")
            .replace('đ', 'd')
            .replace(separators, " ")
            .trim()

    private fun localized(language: AppLanguage, vi: String, en: String, ru: String): String = when (language) {
        AppLanguage.VI -> vi
        AppLanguage.EN -> en
        AppLanguage.RU -> ru
    }
}

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()
    private var pendingAction: AssistantAction? = null

    fun startListening() {
        pendingAction = null
        _uiState.value = VoiceUiState.Listening
    }

    fun onSpeechResult(text: String, language: AppLanguage = AppLanguage.VI) {
        val reply = AssistantCommandRouter.resolve(text, language)
        pendingAction = reply.action
        _uiState.value = VoiceUiState.Speaking(text, reply.answer)
    }

    fun onSpeechError() {
        _uiState.value = VoiceUiState.Error("Không nhận được giọng nói. Thử lại?")
    }

    fun onSpeakingDone(): AssistantAction? {
        val action = pendingAction
        pendingAction = null
        _uiState.value = VoiceUiState.Idle
        return action
    }

    fun onPermissionDenied() {
        pendingAction = null
        _uiState.value = VoiceUiState.Error("Cần cấp quyền micro để dùng trợ lý giọng nói.")
    }

    fun onRecognitionUnavailable() {
        pendingAction = null
        _uiState.value = VoiceUiState.Error("Box này chưa có dịch vụ nhận dạng giọng nói.")
    }

    fun onActionFailed(appName: String) {
        pendingAction = null
        _uiState.value = VoiceUiState.Error("Không thể mở $appName. Hãy kiểm tra lại ứng dụng trên box.")
    }

    fun reset() {
        pendingAction = null
        _uiState.value = VoiceUiState.Idle
    }
}
