package vn.phs.iptv.ui.assistant

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data object Listening : VoiceUiState
    data class Thinking(val question: String) : VoiceUiState
    data class Speaking(val question: String, val answer: String) : VoiceUiState
    data class Error(val message: String) : VoiceUiState
}

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    fun startListening() {
        _uiState.value = VoiceUiState.Listening
    }

    fun onSpeechResult(text: String) {
        _uiState.value = VoiceUiState.Error(
            "Trợ lý giọng nói chưa khả dụng trên phiên bản IPTV này."
        )
    }

    fun onSpeechError() {
        _uiState.value = VoiceUiState.Error("Không nhận được giọng nói. Thử lại?")
    }

    fun onSpeakingDone() {
        _uiState.value = VoiceUiState.Idle
    }

    fun reset() {
        _uiState.value = VoiceUiState.Idle
    }
}
