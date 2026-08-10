@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RestaurantMenu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import vn.phs.iptv.ui.theme.AppThemeMode
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.GlassRadiusLg
import vn.phs.iptv.ui.theme.Gold
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.GoldDeep
import vn.phs.iptv.ui.theme.LightAccent
import vn.phs.iptv.ui.theme.LocalAppThemeMode
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.glassSurface
import vn.phs.iptv.ui.theme.tvFocusRing

/** One live widget card on the Home arrival hub. */
data class HomeAction(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String?,
    val badge: String? = null,
    val highlightValue: String? = null,
    val onClick: () -> Unit,
)

/**
 * The row of live status widgets under the arrival card. Displays live hotel metrics and status
 * (folio total, Wi-Fi info, room service status) as rich widgets rather than duplicate buttons.
 */
@Composable
fun HomeActionRow(
    actions: List<HomeAction>,
    firstFocus: FocusRequester,
    railFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        actions.forEachIndexed { index, action ->
            ActionTile(
                action = action,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (index == 0) {
                            Modifier
                                .focusRequester(firstFocus)
                                .focusProperties { left = railFocus }
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                                        railFocus.requestFocus(); true
                                    } else false
                                }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun ActionTile(action: HomeAction, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(GlassRadiusLg)
    val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT

    Surface(
        onClick = action.onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = TextPrimary,
            focusedContentColor = TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        modifier = modifier
            .height(Dim.ActionTileH)
            .tvFocusRing(focused, shape)
            .glassSurface(shape, focused)
            .onFocusChanged { focused = it.isFocused },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                tint = if (isLight) LightAccent else (if (focused) GoldBright else Gold),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun HomeActionRowPreview() {
    PhsAppTheme {
        Column(Modifier.padding(Dim.Side), verticalArrangement = Arrangement.Center) {
            HomeActionRow(
                actions = listOf(
                    HomeAction("service", Icons.Rounded.RestaurantMenu, "Dịch vụ phòng", "Quét để gọi món") {},
                    HomeAction("bill", Icons.Rounded.ReceiptLong, "Hóa đơn", "200.000 ₫") {},
                ),
                firstFocus = remember { FocusRequester() },
                railFocus = remember { FocusRequester() },
            )
        }
    }
}
