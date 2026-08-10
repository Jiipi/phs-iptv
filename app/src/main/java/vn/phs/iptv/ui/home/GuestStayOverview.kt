@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import vn.phs.iptv.data.remote.dto.BreakfastDto
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.ScreenHotelDto
import vn.phs.iptv.data.remote.dto.ScreenResponse
import vn.phs.iptv.data.remote.dto.StayDto
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.ui.common.QrCodeImage
import vn.phs.iptv.ui.common.resolveWifi
import vn.phs.iptv.ui.common.toLocalDateLabel
import vn.phs.iptv.ui.common.toLocalDateTimeLabel
import vn.phs.iptv.ui.content.facilityLabel
import vn.phs.iptv.ui.i18n.LocalUiStrings
import vn.phs.iptv.ui.i18n.UiStrings
import vn.phs.iptv.ui.theme.ActionBlue
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.GlassRadiusLg
import vn.phs.iptv.ui.theme.GlassRadiusXl
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.glassSurface
import vn.phs.iptv.ui.theme.tvFocusRing

/**
 * The arrival card — everything a guest who has just walked into the room needs, without
 * pressing a single key: which room they booked, when they check out, and the QR that opens
 * the room-service menu on their phone.
 *
 * Every line is conditional: PMS deployments frequently leave Wi-Fi, room-type size or
 * breakfast unset, and a blank line reads as a broken screen (INTEGRATION.md §7/§10).
 * The folio total is deliberately NOT here — it lives on the "bill" action tile so this card
 * can give its width to the QR.
 */
@Composable
fun GuestStayOverview(
    screen: ScreenResponse,
    content: ContentResponse? = null,
    language: AppLanguage = AppLanguage.VI,
    onOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalUiStrings.current
    val stay = screen.stay ?: return
    val qrUrl = screen.qrUrl?.takeIf { it.isNotBlank() }

    Surface(
        shape = RoundedCornerShape(GlassRadiusXl),
        colors = SurfaceDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(GlassRadiusXl)),
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 30.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                content?.roomType?.let { roomType ->
                    val facts = buildList {
                        roomType.roomType.takeIf { it.isNotBlank() }?.let(::add)
                        roomType.sizeSqm?.let { add("${it.toInt()} m²") }
                        roomType.view.takeIf { it.isNotBlank() }?.let(::add)
                    }
                    if (facts.isNotEmpty()) {
                        Text(
                            facts.joinToString(" · "),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    occupancyLine(stay.adults, stay.children, stay.nights, s),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                )
                Text(
                    "${s.checkInLabel}  ${stay.arrival.toLocalDateTimeLabel()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
                Text(
                    "${s.checkOutLabel}  ${stay.departure.toLocalDateTimeLabel()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
                // PMS sends amenity CODES — facilityLabel drops the ones it can't localize
                // rather than printing raw codes on a guest screen (INTEGRATION.md §7).
                content?.roomType?.amenities.orEmpty()
                    .mapNotNull { facilityLabel(it, language) }
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        Text(
                            it.joinToString(" · "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                // Wi-Fi lives on either endpoint depending on branch setup — resolveWifi
                // checks both, so the line no longer disappears for hotels that fill in the
                // hotel record rather than the per-room snapshot.
                val wifi = resolveWifi(screen, content)
                if (wifi.isPresent) {
                    Text(
                        buildString {
                            append("${s.wifiNetwork}: ${wifi.ssid}")
                            if (wifi.password.isNotBlank()) append("  ·  ${wifi.password}")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                breakfastMessage(screen.breakfast, s)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ActionBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // qrUrl null = the branch has room-service ordering switched off → hide the whole
            // block (INTEGRATION.md §10). Re-encoded from screen.qrUrl on every poll.
            if (qrUrl != null) {
                OrderQrPanel(qrUrl = qrUrl, onOrder = onOrder)
            }
        }
    }
}

/** The QR, focusable: pressing OK opens the full-screen version for a guest across the room. */
@Composable
private fun OrderQrPanel(qrUrl: String, onOrder: () -> Unit) {
    val s = LocalUiStrings.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(GlassRadiusLg)
    Surface(
        onClick = onOrder,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = TextPrimary,
            focusedContentColor = TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        modifier = Modifier
            .tvFocusRing(focused, shape)
            .glassSurface(shape, focused)
            .onFocusChanged { focused = it.isFocused },
    ) {
        // Must fit Dim.ArrivalH (230) minus the row's 20dp vertical padding = 190dp, or the
        // Row's height constraint silently clips the caption: 12 + 136 + 6 + 18 + 12 = 184.
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QrCodeImage(qrUrl, contentDescription = s.qrContentDesc, size = 136.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                s.scanToOrder,
                style = MaterialTheme.typography.labelMedium,
                color = if (focused) GoldBright else TextSecondary,
                maxLines = 1,
            )
        }
    }
}

/** "2 adults · 1 child · 4 nights" — the children clause is dropped when there are none. */
private fun occupancyLine(adults: Int, children: Int, nights: Int, s: UiStrings): String =
    buildList {
        add("$adults ${s.adultsShort}")
        if (children > 0) add("$children ${s.childrenShort}")
        add("$nights ${s.nightsShort}")
    }.joinToString(" · ")

private fun breakfastMessage(breakfast: BreakfastDto, s: UiStrings): String? {
    if (!breakfast.enabled || !breakfast.eligible) return null
    val date = breakfast.serviceDate.toLocalDateLabel()
    return when {
        breakfast.alreadySelected -> s.breakfastSelected.format(date)
        breakfast.canOrder -> s.breakfastChoose.format(date, breakfast.cutoffTime)
        else -> s.breakfastClosed.format(date)
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun GuestStayOverviewPreview() {
    PhsAppTheme {
        Column(Modifier.padding(Dim.Side)) {
            GuestStayOverview(
                screen = ScreenResponse(
                    roomNo = "114",
                    occupied = true,
                    hotel = ScreenHotelDto(
                        name = "Menma", wifiSsid = "PHS-Guest", wifiPassword = "12345678",
                        hotline = "1900", breakfastTime = "06:30 – 10:00", welcomeNote = "",
                    ),
                    qrUrl = "https://phs247.com/order/114",
                    breakfast = BreakfastDto(
                        enabled = true, cutoffTime = "22:00", serviceDate = "2026-08-07",
                        eligible = true, alreadySelected = false, canOrder = true,
                    ),
                    stay = StayDto(
                        guestName = "Nguyễn Văn An", adults = 2, children = 1,
                        arrival = "2026-08-06T12:04:08", departure = "2026-08-10T05:34:41", nights = 4,
                    ),
                ),
                onOrder = {},
                modifier = Modifier.height(Dim.ArrivalH),
            )
        }
    }
}
