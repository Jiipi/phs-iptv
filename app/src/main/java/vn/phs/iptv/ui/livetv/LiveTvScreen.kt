@file:OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package vn.phs.iptv.ui.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import vn.phs.iptv.ui.demo.Channel
import vn.phs.iptv.ui.demo.Demo
import vn.phs.iptv.ui.theme.AppleHeroBillboard
import vn.phs.iptv.ui.theme.ApplePoster
import vn.phs.iptv.ui.theme.AppleSubHeader
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.PivotScroll
import vn.phs.iptv.ui.theme.TextPrimary

@Composable
fun LiveTvScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    PhsAppTheme { LiveTvContent(onBack = onBack) }
}

@Composable
private fun LiveTvContent(onBack: () -> Unit) {
    var featured by remember { mutableStateOf(Demo.channels.first()) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }

    PivotScroll(margin = 0.16f) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Dim.Side, end = Dim.Side, top = Dim.Top, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        item {
            AppleSubHeader(title = vn.phs.iptv.ui.i18n.LocalUiStrings.current.liveTv, trailing = "${Demo.channels.size} channels", onBack = onBack)
        }
        item {
            AppleHeroBillboard(
                eyebrow = "Now playing · Ch ${featured.number}",
                title = featured.name,
                body = featured.nowPlaying,
                imageUrl = featured.imageUrl,
                accent = featured.accent,
                onClick = {},
            )
        }
        item { Text("All channels", style = MaterialTheme.typography.headlineLarge, color = TextPrimary) }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Demo.channels.forEachIndexed { i, ch ->
                    ChannelLockup(
                        channel = ch,
                        onFocus = { featured = ch },
                        modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ChannelLockup(channel: Channel, onFocus: () -> Unit, modifier: Modifier = Modifier) {
    ApplePoster(
        title = "${channel.number}  ${channel.name}",
        subtitle = channel.nowPlaying,
        imageUrl = channel.imageUrl,
        accent = channel.accent,
        badge = "LIVE",
        onClick = {},
        width = 280.dp,
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() },
    )
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun LiveTvPreview() {
    LiveTvScreen(onBack = {})
}
