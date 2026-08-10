@file:OptIn(
    ExperimentalTvMaterial3Api::class,
    ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package vn.phs.iptv.ui.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.ui.theme.AppleSectionHeader
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.FocusDurationMs
import vn.phs.iptv.ui.theme.FocusEasing
import vn.phs.iptv.ui.theme.GlassRadiusMd
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.PivotScroll
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextTertiary

@Composable
fun TvAppShelf(
    apps: List<TvApp>,
    language: AppLanguage,
    onLaunch: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (language) {
        AppLanguage.VI -> "Ứng dụng TV"
        AppLanguage.EN -> "TV apps"
        AppLanguage.RU -> "ТВ-приложения"
    }
    val firstItem = remember { FocusRequester() }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dim.HeaderGap)) {
        AppleSectionHeader(title, Modifier.padding(start = Dim.Side))
        PivotScroll(margin = 0.07f) {
            LazyRow(
                modifier = Modifier.focusRestorer { firstItem },
                contentPadding = PaddingValues(horizontal = Dim.Side, vertical = Dim.FocusPad),
                horizontalArrangement = Arrangement.spacedBy(Dim.PosterGap),
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    TvAppCard(
                        app = app,
                        language = language,
                        onClick = { onLaunch(app) },
                        modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvAppCard(
    app: TvApp,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.09f else 1f,
        animationSpec = tween(FocusDurationMs, easing = FocusEasing),
        label = "tvAppScale",
    )
    val shape = RoundedCornerShape(GlassRadiusMd)

    Column(modifier.width(216.dp)) {
        Surface(
            onClick = onClick,
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = app.accent.copy(alpha = 0.18f),
                focusedContainerColor = app.accent.copy(alpha = 0.28f),
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, GoldBright), shape = shape),
            ),
            modifier = Modifier
                .size(width = 216.dp, height = 122.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .onFocusChanged { focused = it.isFocused },
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(app.accent.copy(alpha = 0.62f), Color.Black.copy(alpha = 0.62f)),
                    ),
                ),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = icon,
                    contentDescription = app.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(68.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = app.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = app.category(language),
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
