@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.welcome

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.theme.AmbientVideoBackground
import vn.phs.iptv.ui.theme.ActionBlue
import vn.phs.iptv.ui.theme.ApplePillButton
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.SkyLinkBlue
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.TvBackground
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

// Welcome (F1) — cinematic dark hero, auto-dismisses after 5s or on OK.
@Composable
fun WelcomeScreen(guest: GuestProfile, onDismissed: () -> Unit) {
    PhsAppTheme { WelcomeContent(guest, onDismissed) }
}

@Composable
private fun WelcomeContent(guest: GuestProfile, onDismissed: () -> Unit) {
    val skip = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        skip.requestFocus()
        delay(6_000)
        onDismissed()
    }

    Box(Modifier.fillMaxSize()) {
        AmbientVideoBackground(Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(TvBackground.copy(alpha = 0.45f), TvBackground.copy(alpha = 0.15f), TvBackground.copy(alpha = 0.6f)),
                ),
            ),
        )
        WelcomeForeground(guest, skip, onDismissed)
    }
}

@Composable
private fun WelcomeForeground(guest: GuestProfile, skip: FocusRequester, onDismissed: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 80.dp, end = 80.dp, top = 60.dp, bottom = 60.dp),
    ) {
        Text(
            "PHS",
            style = MaterialTheme.typography.headlineLarge,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.TopStart),
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Welcome",
                style = MaterialTheme.typography.headlineMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "${guest.title} ${guest.name}",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Room ${guest.roomNo}",
                style = MaterialTheme.typography.displaySmall,
                color = ActionBlue,
                textAlign = TextAlign.Center,
            )
            if (guest.isBirthday) {
                Spacer(Modifier.height(22.dp))
                Text(
                    "🎂  Happy birthday from all of us",
                    style = MaterialTheme.typography.titleLarge,
                    color = SkyLinkBlue,
                    textAlign = TextAlign.Center,
                )
            }
        }

        ApplePillButton(
            text = "Continue",
            onClick = onDismissed,
            filled = false,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .focusRequester(skip),
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun WelcomePreview() {
    WelcomeScreen(GuestProfile("Nguyen Van An", "Mr.", "302", "VNM", false), onDismissed = {})
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun WelcomeBirthdayPreview() {
    WelcomeScreen(GuestProfile("Tran Thi Bich", "Ms.", "501", "VNM", true), onDismissed = {})
}
