@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.order

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import vn.phs.iptv.data.remote.dto.ScreenResponse
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.common.QrCodeImage
import vn.phs.iptv.ui.i18n.LocalUiStrings
import vn.phs.iptv.ui.theme.ApplePillButton
import vn.phs.iptv.ui.theme.AppleSubHeader
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.PivotScroll
import vn.phs.iptv.ui.theme.SurfaceTile1
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary

@Composable
fun OrderScreen(
    guest: GuestProfile,
    screenData: ScreenResponse? = null,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    PhsAppTheme { OrderContent(guest, screenData, onBack) }
}

@Composable
private fun OrderContent(
    guest: GuestProfile,
    screenData: ScreenResponse?,
    onBack: () -> Unit,
) {
    val s = LocalUiStrings.current
    val back = remember { FocusRequester() }
    val qrUrl = screenData?.qrUrl?.takeIf { it.isNotBlank() }
    LaunchedEffect(Unit) { back.requestFocus() }

    PivotScroll(margin = 0.16f) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Dim.Side, end = Dim.Side, top = Dim.Top, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                AppleSubHeader(
                    title = s.orderTitle,
                    trailing = "${s.roomWord} ${guest.roomNo}",
                    onBack = onBack,
                    backModifier = Modifier.focusRequester(back),
                )
            }

            item {
                Text(s.orderTitle, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            }

            if (qrUrl != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        colors = SurfaceDefaults.colors(containerColor = SurfaceTile1),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 48.dp, vertical = 40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(44.dp),
                        ) {
                            QrCodeImage(
                                content = qrUrl,
                                contentDescription = s.qrContentDesc,
                                size = 300.dp,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    s.orderScanHeading,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    s.orderScanBody.format(guest.roomNo),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextSecondary,
                                )
                                Spacer(Modifier.width(1.dp))
                                ApplePillButton(text = s.done, onClick = onBack, filled = false)
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        colors = SurfaceDefaults.colors(containerColor = SurfaceTile1),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.padding(48.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Text(
                                s.orderUnavailableHeading,
                                style = MaterialTheme.typography.displaySmall,
                                color = TextPrimary,
                            )
                            Text(
                                s.orderUnavailableBody,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextSecondary,
                            )
                            ApplePillButton(text = s.done, onClick = onBack, filled = false)
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun OrderPreview() {
    OrderScreen(GuestProfile("Nguyen Van An", "Mr.", "302", "VNM", false), onBack = {})
}
