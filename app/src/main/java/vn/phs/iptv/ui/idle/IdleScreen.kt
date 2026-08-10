@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.idle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.ScreenResponse
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.ui.common.resolveWifi
import vn.phs.iptv.ui.content.forLang
import vn.phs.iptv.ui.theme.AmbientVideoBackground
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.SkyLinkBlue
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.TextTertiary
import vn.phs.iptv.ui.theme.TvBackground
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Idle — paired, no guest. Cinematic dark, oversized clock.
@Composable
fun IdleScreen(
    screenData: ScreenResponse? = null,
    contentData: ContentResponse? = null,
    language: AppLanguage = AppLanguage.VI,
    isOnline: Boolean = true,
) {
    PhsAppTheme { IdleContent(screenData, contentData, language, isOnline) }
}

@Composable
private fun IdleContent(
    screenData: ScreenResponse?,
    contentData: ContentResponse?,
    language: AppLanguage,
    isOnline: Boolean,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = LocalTime.now() } }
    val time = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val date = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
    }

    Box(Modifier.fillMaxSize()) {
        AmbientVideoBackground(
            modifier = Modifier.fillMaxSize(),
            videoUrl = contentData?.video?.idleUrl?.takeIf { it.isNotBlank() },
        )
        // Cinematic scrim so the wordmark and clock stay legible over the footage
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(TvBackground.copy(alpha = 0.30f), TvBackground.copy(alpha = 0.05f), TvBackground.copy(alpha = 0.55f)),
                ),
            ),
        )
        IdleForeground(time, date, screenData, contentData, language, isOnline)
    }
}

@Composable
private fun IdleForeground(
    time: String,
    date: String,
    screenData: ScreenResponse?,
    contentData: ContentResponse?,
    language: AppLanguage,
    isOnline: Boolean,
) {
    val screenHotel = screenData?.hotel
    val contentHotel = contentData?.hotel
    val wifi = resolveWifi(screenData, contentData)
    val wifiSsid = wifi.ssid
    val wifiPassword = wifi.password
    val phone = contentHotel?.phone?.takeIf { it.isNotBlank() }
        ?: screenHotel?.hotline.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 80.dp, end = 80.dp, top = 60.dp, bottom = 60.dp),
    ) {
        // Oversized clock — top right
        Column(modifier = Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.displayLarge, color = TextPrimary)
            Text(date, style = MaterialTheme.typography.titleLarge, color = TextTertiary)
        }

        // Center wordmark
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                contentHotel?.name?.takeIf { it.isNotBlank() } ?: screenHotel?.name ?: "PHS",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                contentData?.texts?.idleNote?.forLang(language)?.takeIf { it.isNotBlank() }
                    ?: screenHotel?.welcomeNote?.takeIf { it.isNotBlank() }
                    ?: "Welcome",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            screenData?.roomNo?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text("Room $it", style = MaterialTheme.typography.titleMedium, color = SkyLinkBlue)
            }
        }

        if (wifiSsid.isNotBlank() || phone.isNotBlank()) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                if (wifiSsid.isNotBlank()) {
                    Text("Wi‑Fi: $wifiSsid", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    if (wifiPassword.isNotBlank()) {
                        Text("Password: $wifiPassword", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    }
                }
                if (phone.isNotBlank()) {
                    Text("Hotline: $phone", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                }
            }
        }

        if (!isOnline) {
            Text(
                "Offline",
                style = MaterialTheme.typography.labelLarge,
                color = TextTertiary,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }

    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun IdlePreview() {
    IdleScreen(isOnline = true)
}
