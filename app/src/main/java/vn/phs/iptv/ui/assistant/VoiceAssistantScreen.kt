@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.domain.AppScreen
import vn.phs.iptv.ui.apps.TvAppCatalog
import vn.phs.iptv.ui.demo.Demo
import vn.phs.iptv.ui.theme.AppleBackButton
import vn.phs.iptv.ui.theme.ActionBlue
import vn.phs.iptv.ui.theme.FocusBlue
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.SkyLinkBlue
import vn.phs.iptv.ui.theme.SurfaceTile1
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.TextTertiary
import java.util.Locale

// Voice Assistant (F3) — tvOS dark. Idle → Listening → Thinking → Speaking → Idle.
@Composable
fun VoiceAssistantScreen(
    onBack: () -> Unit,
    language: AppLanguage = AppLanguage.VI,
    onNavigate: (AppScreen) -> Unit = {},
    viewModel: VoiceAssistantViewModel = hiltViewModel(),
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val micFocus = remember { FocusRequester() }
    val speechAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
    val speechLocale = remember(language) {
        Locale.forLanguageTag(
            when (language) {
                AppLanguage.VI -> "vi-VN"
                AppLanguage.EN -> "en-US"
                AppLanguage.RU -> "ru-RU"
            },
        )
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startListening() else viewModel.onPermissionDenied()
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context, speechLocale) {
        val instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = speechLocale
        }
        tts = instance
        onDispose { instance.stop(); instance.shutdown() }
    }

    val recognizer = remember(context, speechAvailable) {
        if (speechAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer, language) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(p: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(errorCode: Int) { viewModel.onSpeechError() }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) viewModel.onSpeechResult(text, language) else viewModel.onSpeechError()
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(p: Int, p1: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }

    LaunchedEffect(uiState, recognizer, tts, language) {
        when (val state = uiState) {
            is VoiceUiState.Listening -> {
                if (recognizer == null) {
                    viewModel.onRecognitionUnavailable()
                } else {
                    val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale.toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    try {
                        recognizer.startListening(intent)
                    } catch (_: SecurityException) {
                        viewModel.onPermissionDenied()
                    }
                }
            }
            is VoiceUiState.Speaking -> {
                val completeCommand = {
                    Handler(Looper.getMainLooper()).post {
                        when (val action = viewModel.onSpeakingDone()) {
                            is AssistantAction.Navigate -> onNavigate(action.destination)
                            is AssistantAction.LaunchApp -> {
                                if (!TvAppCatalog.launch(context, action.packageName)) {
                                    viewModel.onActionFailed(action.appName)
                                }
                            }
                            null -> Unit
                        }
                    }
                    Unit
                }
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { completeCommand() }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { completeCommand() }
                })
                if (tts?.speak(state.answer, TextToSpeech.QUEUE_FLUSH, null, "phs_tts") == TextToSpeech.ERROR) {
                    completeCommand()
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) { micFocus.requestFocus() }

    VoiceContent(
        uiState = uiState,
        micFocus = micFocus,
        onBack = onBack,
        onMicPress = {
            when (uiState) {
                is VoiceUiState.Idle, is VoiceUiState.Error -> {
                    when {
                        !speechAvailable -> viewModel.onRecognitionUnavailable()
                        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ->
                            viewModel.startListening()
                        else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                is VoiceUiState.Speaking -> { tts?.stop(); viewModel.reset() }
                else -> {}
            }
        },
    )
}

@Composable
private fun VoiceContent(
    uiState: VoiceUiState,
    micFocus: FocusRequester,
    onBack: () -> Unit,
    onMicPress: () -> Unit,
) {
    val isListening = uiState is VoiceUiState.Listening
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulseScale",
    )

    Box(
        modifier = Modifier.fillMaxSize().padding(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppleBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (uiState) {
                    is VoiceUiState.Idle -> "Ask me anything"
                    is VoiceUiState.Listening -> "Listening…"
                    is VoiceUiState.Thinking -> "Thinking…"
                    is VoiceUiState.Speaking -> "Here's what I found"
                    is VoiceUiState.Error -> uiState.message
                },
                style = MaterialTheme.typography.displayMedium,
                color = if (uiState is VoiceUiState.Error) MaterialTheme.colorScheme.error else TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(44.dp))

            MicOrb(
                focused = false,
                listening = isListening,
                pulse = pulse,
                micFocus = micFocus,
                onPress = onMicPress,
            )

            Spacer(Modifier.height(40.dp))

            when (uiState) {
                is VoiceUiState.Idle, is VoiceUiState.Error -> {
                    Text("Try one of these", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                    Spacer(Modifier.height(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Demo.voiceSuggestions.forEach { s ->
                            Text("“$s”", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                        }
                    }
                }
                is VoiceUiState.Thinking -> QaCard(question = uiState.question, answer = null)
                is VoiceUiState.Speaking -> QaCard(question = uiState.question, answer = uiState.answer)
                else -> {}
            }
        }
    }
}

@Composable
private fun MicOrb(
    focused: Boolean,
    listening: Boolean,
    pulse: Float,
    micFocus: FocusRequester,
    onPress: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        // Ambient glow
        Box(
            Modifier
                .size(220.dp)
                .graphicsLayer { val s = if (listening) pulse else if (isFocused) 1.08f else 1f; scaleX = s; scaleY = s; alpha = if (listening) 0.5f else 0.3f }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(SkyLinkBlue, Color.Transparent))),
        )
        Surface(
            onClick = onPress,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = ActionBlue, focusedContainerColor = FocusBlue,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer { if (listening) { scaleX = pulse; scaleY = pulse } }
                .focusRequester(micFocus)
                .onFocusChanged { isFocused = it.isFocused },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🎙", style = MaterialTheme.typography.displayMedium)
            }
        }
    }
}

@Composable
private fun QaCard(question: String, answer: String?) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = SurfaceTile1),
        modifier = Modifier.widthIn(max = 820.dp),
    ) {
        Column(Modifier.padding(32.dp)) {
            Text(question, style = MaterialTheme.typography.titleLarge, color = TextSecondary)
            if (answer != null) {
                Spacer(Modifier.height(16.dp))
                Text(answer, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun VoiceIdlePreview() {
    PhsAppTheme {
        VoiceContent(VoiceUiState.Idle, remember { FocusRequester() }, onBack = {}, onMicPress = {})
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun VoiceSpeakingPreview() {
    PhsAppTheme {
        VoiceContent(
            VoiceUiState.Speaking("What time is breakfast?", "Breakfast is served from 6:30 to 10:00 AM in the Lotus restaurant on level 2."),
            remember { FocusRequester() }, onBack = {}, onMicPress = {},
        )
    }
}
