@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.home

import android.view.LayoutInflater
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import vn.phs.iptv.BuildConfig
import vn.phs.iptv.R
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.GlassRadiusXl
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.SurfaceTile1

/**
 * The greeting panel that sits beside the arrival card on Home. It has two faces, and which
 * one it wears depends entirely on whether the branch actually uploaded a hotel film:
 *
 *  - [videoUrl] set → a living billboard: the film loops muted behind the greeting and the
 *    "▶ [playLabel]" row invites the guest to open it full-screen with sound. Focusable.
 *  - [videoUrl] null/blank → a still room portrait ([imageUrl]) with the greeting and **no**
 *    play affordance, and not focusable. Without this split the panel offers a button that
 *    does nothing: `IntroVideoScreen` calls `onEnded()` immediately on a missing URL, so the
 *    guest presses OK and gets bounced straight back to Home (INTEGRATION.md §10).
 *
 * The caller sizes it — Home gives it `Dim.HeroW` × `Dim.ArrivalH` so it matches the height of
 * the stay card next to it.
 */
import vn.phs.iptv.ui.video.resolveVideoUri

@Composable
fun AppleVideoBillboard(
    eyebrow: String,
    title: String,
    body: String,
    playLabel: String,
    videoUrl: String? = null,
    imageUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val film = remember(videoUrl) { resolveVideoUri(context, videoUrl) }
    FilmBillboard(eyebrow, title, body, playLabel, film, imageUrl, onClick, modifier)
}

@Composable
private fun FilmBillboard(
    eyebrow: String,
    title: String,
    body: String,
    playLabel: String,
    videoUrl: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val mediaUri = videoUrl
    val player = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }

    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(GlassRadiusXl)),
        colors = ClickableSurfaceDefaults.colors(containerColor = SurfaceTile1, focusedContainerColor = SurfaceTile1),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, GoldBright), shape = RoundedCornerShape(GlassRadiusXl)),
        ),
        modifier = modifier,
    ) {
        BillboardFace(eyebrow, title, body, imageUrl, playLabel) {
            // Inflated from XML so it uses a TextureView and composites inline (a SurfaceView
            // would render in its own layer behind the window and stay invisible).
            if (player != null) {
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.ambient_player, null) as PlayerView).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Artwork (or film) → scrim → greeting, bottom-aligned. The scrim runs top-to-bottom rather
 * than left-to-right because the panel is now narrow enough that the copy spans its full
 * width — a side gradient would leave the end of every line unreadable.
 * [playLabel] null hides the play row entirely.
 */
@Composable
private fun BillboardFace(
    eyebrow: String,
    title: String,
    body: String,
    imageUrl: String?,
    playLabel: String?,
    film: @Composable () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        imageUrl?.takeIf { it.isNotBlank() }?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        film()
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.58f), Color.Black.copy(alpha = 0.90f)),
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .fillMaxWidth(),
        ) {
            if (eyebrow.isNotBlank()) {
                Text(
                    eyebrow.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (body.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (playLabel != null) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        playLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GoldBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun AppleVideoBillboardPreview() {
    PhsAppTheme {
        Row(Modifier.padding(Dim.Side)) {
            // No film uploaded — the common case on a fresh branch: still portrait, no play row.
            AppleVideoBillboard(
                eyebrow = "Đơn Cao Cấp",
                title = "Chào buổi chiều, Phòng 114",
                body = "",
                playLabel = "Xem giới thiệu khách sạn",
                videoUrl = null,
                imageUrl = null,
                onClick = {},
                modifier = Modifier.width(Dim.HeroW).height(Dim.ArrivalH),
            )
            Spacer(Modifier.width(24.dp))
            AppleVideoBillboard(
                eyebrow = "Đơn Cao Cấp",
                title = "Chào buổi chiều, Phòng 114",
                body = "Chúc quý khách một kỳ nghỉ thật dễ chịu.",
                playLabel = "Xem giới thiệu khách sạn",
                videoUrl = "https://example.com/intro.mp4",
                imageUrl = null,
                onClick = {},
                modifier = Modifier.width(Dim.HeroW).height(Dim.ArrivalH),
            )
        }
    }
}
