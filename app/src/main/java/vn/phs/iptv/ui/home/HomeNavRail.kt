@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import vn.phs.iptv.ui.theme.FocusDurationMs
import vn.phs.iptv.ui.theme.FocusEasing
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.GoldDeep
import vn.phs.iptv.ui.theme.OnGold
import vn.phs.iptv.ui.theme.GlassRadiusMd
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.TvBackground

/** One quick-access destination in the Home navigation rail. */
data class NavRailItem(
    val id: String,
    val icon: ImageVector,       // Material Symbols vector (tintable, crisp at any size)
    val label: String,           // localized; only shown when the rail is expanded
    val onSelect: () -> Unit,
)

private val RailCollapsed = 72.dp     // fits inside the existing Dim.Side = 80 left gutter
private val RailExpanded = 248.dp

/**
 * Left navigation rail for the Home hub. Collapsed (icons only) by default; when any item
 * takes focus the whole rail animates open to reveal labels (Google-TV / Netflix idiom).
 * It OVERLAYS the content (drawn last, aligned to the start edge) so the content column
 * never reflows as the rail width animates — only a left scrim grows under the labels.
 *
 * Focus wiring: [railFocusRequester] is placed on the first item so D-pad LEFT from the
 * content can target the rail deterministically; every row redirects D-pad RIGHT back to
 * [contentFocusRequester] (the billboard) so the guest always returns where they came from.
 */
@Composable
fun HomeNavRail(
    items: List<NavRailItem>,
    selectedId: String,
    railFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    onRailFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var railFocused by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        if (railFocused) RailExpanded else RailCollapsed,
        tween(FocusDurationMs, easing = FocusEasing), label = "railWidth",
    )

        val isLight = vn.phs.iptv.ui.theme.LocalAppThemeMode.current == vn.phs.iptv.ui.theme.AppThemeMode.LIGHT
        val railBase = if (isLight) vn.phs.iptv.ui.theme.Parchment else TvBackground
        Column(
            modifier = modifier
                .width(width)
                .fillMaxHeight()
                .onFocusChanged {
                    if (it.hasFocus != railFocused) {
                        railFocused = it.hasFocus
                        onRailFocusChanged(it.hasFocus)
                    }
                }
                .focusGroup()
                .background(
                    Brush.horizontalGradient(
                        0f to railBase.copy(alpha = if (isLight) 0.70f else 0.93f),
                        0.7f to railBase.copy(alpha = if (isLight) 0.45f else 0.80f),
                        1f to Color.Transparent,
                    ),
                )
                .background(
                    Brush.horizontalGradient(
                        0f to (if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)),
                        0.8f to Color.Transparent,
                    ),
                )
                .padding(start = 4.dp, end = 4.dp, top = 28.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            items.forEachIndexed { i, item ->
                NavRailRow(
                    item = item,
                    selected = item.id == selectedId,
                    expanded = railFocused,
                    modifier = Modifier
                        .then(if (i == 0) Modifier.focusRequester(railFocusRequester) else Modifier)
                        .focusProperties { right = contentFocusRequester },
                )
            }
        }
}

@Composable
private fun NavRailRow(
    item: NavRailItem,
    selected: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val isLight = vn.phs.iptv.ui.theme.LocalAppThemeMode.current == vn.phs.iptv.ui.theme.AppThemeMode.LIGHT
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "railRow",
    )
    val shape = RoundedCornerShape(GlassRadiusMd)
    val selBg = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f)
    val selContent = if (isLight) GoldDeep else GoldBright
    Surface(
        onClick = item.onSelect,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) selBg else Color.Transparent,
            focusedContainerColor = GoldBright,
            contentColor = if (selected) selContent else TextSecondary,
            focusedContentColor = OnGold,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(2.dp, GoldBright), shape = shape),
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused },
    ) {
        Row(
            Modifier.fillMaxHeight().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(26.dp))
            }
            if (expanded) {
                Spacer(Modifier.width(12.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
