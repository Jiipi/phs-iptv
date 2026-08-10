package vn.phs.iptv.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import vn.phs.iptv.R
import vn.phs.iptv.ui.video.resolveVideoUri

/**
 * A muted, looping full-bleed ambient video used behind hero/idle screens to give the
 * cinematic, "alive" 5-star feel. Falls back to the bundled res/raw/ambient.mp4 when the
 * PMS has no clip of its own, cropped to fill.
 */
@Composable
fun AmbientVideoBackground(
    modifier: Modifier = Modifier,
    videoUrl: String? = null,
) {
    val context = LocalContext.current
    val mediaUri = remember(videoUrl) { resolveVideoUri(context, videoUrl) }
    val player = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    Box(modifier) {
        AndroidView(
            factory = { ctx ->
                // Inflated from XML so the view uses a TextureView (surface_type), which
                // composites inline behind the Compose UI — a SurfaceView would render in
                // its own layer behind the window and stay invisible.
                (LayoutInflater.from(ctx).inflate(R.layout.ambient_player, null) as PlayerView).apply {
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
