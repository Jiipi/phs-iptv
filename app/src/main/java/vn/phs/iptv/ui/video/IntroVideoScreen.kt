@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import vn.phs.iptv.R
import vn.phs.iptv.ui.theme.PhsAppTheme

// PRD §10.4 — IntroVideoScreen (F2)
// Context: full-bleed black. Media3 ExoPlayer full screen.
// Idempotency: AppStateMachine.onWelcomeLanguageSelected() skips this screen if the stayId
// already played it.
//
// [videoUrl] is the hotel's own film from the PMS `content` endpoint. Branches that have not
// uploaded one send an empty string, so we fall back to the bundled res/raw/ambient.mp4 —
// a guest arriving in the room should always be met by moving footage, never a black frame.
@Composable
fun IntroVideoScreen(
    videoUrl: String?,
    onEnded: () -> Unit,
) {
    BackHandler { onEnded() }

    val context = LocalContext.current
    val mediaUri = videoUrl?.takeIf { it.isNotBlank() }
        ?: RawResourceDataSource.buildRawResourceUri(R.raw.ambient).toString()
    val exoPlayer = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = true
        }
    }

    var showHint by remember { mutableStateOf(true) }
    val skipFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        skipFocus.requestFocus()   // own focus so OK/Enter (DPAD_CENTER) reaches us
        delay(3_000)
        showHint = false
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onEnded()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // The hint promises "Back or OK to skip" — honour OK/Enter too, not just Back.
            .focusRequester(skipFocus)
            .focusable()
            .onKeyEvent { e ->
                if (e.key == Key.DirectionCenter || e.key == Key.Enter) {
                    // Consume the complete press. If KeyDown escapes, the matching KeyUp can
                    // arrive after navigation and activate Home's first focused tile.
                    if (e.type == KeyEventType.KeyUp) onEnded()
                    true
                } else {
                    false
                }
            },
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    // Inflated from XML for surface_type="texture_view": a plain PlayerView
                    // defaults to a SurfaceView, which renders in its own layer behind the
                    // window and stays invisible under Compose (see AmbientVideo.kt).
                    (LayoutInflater.from(ctx).inflate(R.layout.ambient_player, null) as PlayerView).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // "Press Back/OK to skip" overlay — fades after 3s (PRD §10.4)
            AnimatedVisibility(
                visible = showHint,
                exit    = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text      = stringResource(R.string.video_skip_hint),
                    style     = MaterialTheme.typography.labelLarge,
                    color     = Color.White.copy(alpha = 0.7f),
                    modifier  = Modifier
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun IntroVideoPreview() {
    PhsAppTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = stringResource(R.string.video_skip_hint),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
