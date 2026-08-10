package vn.phs.iptv.ui.common

import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.ScreenResponse
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val viLocale = Locale("vi", "VN")
private val dateTimeOutput = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", viLocale)
private val dateOutput = DateTimeFormatter.ofPattern("dd/MM/yyyy", viLocale)

fun Long.toVnd(): String = "${NumberFormat.getIntegerInstance(viLocale).format(this)} ₫"

fun String.toLocalDateTimeLabel(): String = runCatching {
    LocalDateTime.parse(this).format(dateTimeOutput)
}.getOrDefault(this)

fun String.toLocalDateLabel(): String = runCatching {
    LocalDate.parse(this).format(dateOutput)
}.getOrDefault(this)

/**
 * Guest Wi-Fi, resolved across both endpoints.
 *
 * The PMS exposes the credentials twice — `content.hotel.wifi` (the hotel record, richer and
 * edited by staff) and `screen.hotel.wifiSsid/Password` (a per-room snapshot). Either can be
 * blank or absent depending on how the branch was configured, so Content wins when it has a
 * value and Screen is the fallback. Idle and Help already did this; Home read only Screen and
 * so hid the Wi-Fi line for any branch that fills in the hotel record instead.
 */
data class WifiCredentials(val ssid: String, val password: String) {
    val isPresent: Boolean get() = ssid.isNotBlank()
}

fun resolveWifi(screen: ScreenResponse?, content: ContentResponse?): WifiCredentials {
    val contentWifi = content?.hotel?.wifi
    return WifiCredentials(
        ssid = contentWifi?.ssid?.takeIf { it.isNotBlank() }
            ?: screen?.hotel?.wifiSsid.orEmpty(),
        password = contentWifi?.password?.takeIf { it.isNotBlank() }
            ?: screen?.hotel?.wifiPassword.orEmpty(),
    )
}
