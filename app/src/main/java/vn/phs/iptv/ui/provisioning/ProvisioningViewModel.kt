package vn.phs.iptv.ui.provisioning

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
import vn.phs.iptv.data.IptvRepository
import vn.phs.iptv.data.remote.dto.MeResponse
import javax.inject.Inject

sealed interface ProvisioningUiState {
    data object Registering : ProvisioningUiState
    data class WaitingForAssignment(val displayCode: String) : ProvisioningUiState
    data class Assigned(val roomNo: String) : ProvisioningUiState  // transient — triggers nav
    data class Error(val message: String) : ProvisioningUiState
}

@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    private val repository: IptvRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProvisioningUiState>(ProvisioningUiState.Registering)
    val uiState: StateFlow<ProvisioningUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        register()
    }

    fun refresh() {
        register()
    }

    private fun register() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _uiState.value = ProvisioningUiState.Registering
            try {
                val initial = repository.preparePairing()
                if (!acceptAssignment(initial)) {
                    _uiState.value = ProvisioningUiState.WaitingForAssignment(initial.displayCode)
                    pollUntilAssigned(initial.displayCode)
                }
            } catch (e: Exception) {
                val apiError = repository.apiError(e)
                _uiState.value = ProvisioningUiState.Error(
                    apiError?.defaultMessage?.takeIf { it.isNotBlank() }
                        ?: e.message
                        ?: "Lỗi kết nối"
                )
            }
        }
    }

    private suspend fun pollUntilAssigned(initialDisplayCode: String) {
        var displayCode = initialDisplayCode
        while (viewModelScope.isActive) {
            try {
                val status = repository.checkPairing()
                displayCode = status.displayCode
                if (acceptAssignment(status)) return
                _uiState.value = ProvisioningUiState.WaitingForAssignment(displayCode)
            } catch (e: Exception) {
                val apiError = repository.apiError(e)
                if (apiError?.mess == "systemalert.iptv.device.invalid") {
                    _uiState.value = ProvisioningUiState.Error(
                        "Thiết bị chưa đăng ký, vui lòng liên hệ lễ tân."
                    )
                    return
                }
                // Network and temporary server errors keep the pairing code visible.
                _uiState.value = ProvisioningUiState.WaitingForAssignment(displayCode)
            }
            delay(5_000)
        }
    }

    private fun acceptAssignment(response: MeResponse): Boolean {
        if (response.status != "assigned" || response.deviceToken.isNullOrBlank()) return false
        _uiState.value = ProvisioningUiState.Assigned(response.roomNo.orEmpty())
        return true
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
