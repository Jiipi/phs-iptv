@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.help

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.ScreenResponse
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.common.resolveWifi
import vn.phs.iptv.ui.content.facilityLabel
import vn.phs.iptv.ui.i18n.LocalUiStrings
import vn.phs.iptv.ui.theme.ApplePillButton
import vn.phs.iptv.ui.theme.AppleSubHeader
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.Gold
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.PivotScroll
import vn.phs.iptv.ui.theme.SurfaceTile1
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.TextTertiary

@Composable
fun HelpScreen(
    guest: GuestProfile,
    screenData: ScreenResponse? = null,
    contentData: ContentResponse? = null,
    language: AppLanguage = AppLanguage.VI,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    PhsAppTheme { HelpContent(guest, screenData, contentData, language, onBack) }
}

@Composable
private fun HelpContent(
    guest: GuestProfile,
    screenData: ScreenResponse?,
    contentData: ContentResponse?,
    language: AppLanguage,
    onBack: () -> Unit,
) {
    val s = LocalUiStrings.current
    val hotel = contentData?.hotel
    val screenHotel = screenData?.hotel
    val hotelName = hotel?.name?.takeIf { it.isNotBlank() } ?: screenHotel?.name ?: "PHS"
    val phone = hotel?.phone?.takeIf { it.isNotBlank() } ?: screenHotel?.hotline.orEmpty()
    val wifi = resolveWifi(screenData, contentData)
    val wifiSsid = wifi.ssid
    val wifiPassword = wifi.password
    val facilities = hotel?.facilities.orEmpty().mapNotNull { facilityLabel(it, language) }.distinct()
    val roomType = contentData?.roomType
    val back = remember { FocusRequester() }
    val context = LocalContext.current
    val dialIntent = remember(phone) {
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
    }
    val canDial = remember(phone) {
        phone.isNotBlank() && dialIntent.resolveActivity(context.packageManager) != null
    }
    LaunchedEffect(Unit) { back.requestFocus() }

    PivotScroll(margin = 0.16f) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Dim.Side, end = Dim.Side, top = Dim.Top, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                AppleSubHeader(
                    title = s.helpTitle,
                    trailing = "${s.roomWord} ${guest.roomNo}",
                    onBack = onBack,
                    backModifier = Modifier.focusRequester(back),
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(hotelName, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                    hotel?.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                    }
                }
            }

            if (wifiSsid.isNotBlank()) {
                item { WifiCard(ssid = wifiSsid, password = wifiPassword) }
            }

            val hotelRows = buildList {
                hotel?.address?.takeIf { it.isNotBlank() }?.let { add(label(language, "Địa chỉ", "Address", "Адрес") to it) }
                phone.takeIf { it.isNotBlank() }?.let { add("Hotline" to it) }
                hotel?.email?.takeIf { it.isNotBlank() }?.let { add("Email" to it) }
                hotel?.checkInTime?.takeIf { it.isNotBlank() }?.let { add(label(language, "Nhận phòng", "Check-in", "Заезд") to it) }
                hotel?.checkOutTime?.takeIf { it.isNotBlank() }?.let { add(label(language, "Trả phòng", "Check-out", "Выезд") to it) }
                screenHotel?.breakfastTime?.takeIf { it.isNotBlank() }?.let { add(label(language, "Bữa sáng", "Breakfast", "Завтрак") to it) }
            }
            if (hotelRows.isNotEmpty()) item { InfoCard(hotelRows) }

            if (roomType != null) {
                val roomRows = buildList {
                    roomType.roomType.takeIf { it.isNotBlank() }?.let { add(label(language, "Loại phòng", "Room type", "Тип номера") to it) }
                    roomType.description.takeIf { it.isNotBlank() }?.let { add(label(language, "Mô tả", "Description", "Описание") to it) }
                    roomType.sizeSqm?.let { add(label(language, "Diện tích", "Size", "Площадь") to "${it.toInt()} m²") }
                    roomType.view.takeIf { it.isNotBlank() }?.let { add(label(language, "Hướng nhìn", "View", "Вид") to it) }
                    val amenities = roomType.amenities.mapNotNull { facilityLabel(it, language) }
                    if (amenities.isNotEmpty()) add(label(language, "Tiện nghi", "Amenities", "Удобства") to amenities.joinToString(" · "))
                }
                if (roomRows.isNotEmpty()) item { InfoCard(roomRows) }
            }

            if (facilities.isNotEmpty()) {
                item {
                    InfoCard(listOf(label(language, "Tiện ích", "Facilities", "Услуги") to facilities.joinToString(" · ")))
                }
            }

            hotel?.policies.orEmpty().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { policies ->
                item {
                    InfoCard(listOf(label(language, "Chính sách", "Policies", "Правила") to policies.joinToString("\n")))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (canDial) {
                        ApplePillButton(
                            text = s.callReception,
                            leading = "📞",
                            onClick = { context.startActivity(dialIntent) },
                        )
                    }
                    ApplePillButton(text = s.done, onClick = onBack, filled = false)
                }
            }
        }
    }
}

@Composable
private fun WifiCard(ssid: String, password: String) {
    val s = LocalUiStrings.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceTile1),
        border = Border(BorderStroke(1.5.dp, Gold.copy(alpha = 0.55f)), shape = RoundedCornerShape(22.dp)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 40.dp, vertical = 32.dp)) {
            Text(s.wifiNetwork, style = MaterialTheme.typography.labelLarge, color = TextTertiary)
            Text(ssid, style = MaterialTheme.typography.displaySmall, color = TextPrimary)
            if (password.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(s.wifiPassword, style = MaterialTheme.typography.labelLarge, color = TextTertiary)
                Text(
                    password,
                    style = MaterialTheme.typography.displaySmall.copy(
                        letterSpacing = TextUnit(2f, TextUnitType.Sp),
                    ),
                    color = GoldBright,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceTile1),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 32.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { (key, value) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(key, style = MaterialTheme.typography.bodyMedium, color = TextTertiary, modifier = Modifier.width(150.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun label(language: AppLanguage, vi: String, en: String, ru: String): String = when (language) {
    AppLanguage.VI -> vi
    AppLanguage.EN -> en
    AppLanguage.RU -> ru
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun HelpPreview() {
    HelpScreen(GuestProfile("Nguyen Van An", "Mr.", "302", "VNM", false), onBack = {})
}
