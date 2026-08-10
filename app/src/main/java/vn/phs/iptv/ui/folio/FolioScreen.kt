@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.folio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import vn.phs.iptv.data.remote.dto.ScreenFolioDto
import vn.phs.iptv.data.remote.dto.ScreenResponse
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.common.toLocalDateTimeLabel
import vn.phs.iptv.ui.common.toVnd
import vn.phs.iptv.ui.i18n.LocalUiStrings
import vn.phs.iptv.ui.i18n.UiStrings
import vn.phs.iptv.ui.theme.ApplePillButton
import vn.phs.iptv.ui.theme.AppleSubHeader
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.HairlineOnDark
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.PivotScroll
import vn.phs.iptv.ui.theme.SurfaceTile1
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary

@Composable
fun FolioScreen(guest: GuestProfile, screenData: ScreenResponse? = null, onBack: () -> Unit) {
    BackHandler { onBack() }
    PhsAppTheme { FolioContent(guest, screenData, onBack) }
}

@Composable
private fun FolioContent(guest: GuestProfile, screenData: ScreenResponse?, onBack: () -> Unit) {
    val s = LocalUiStrings.current
    val back = remember { FocusRequester() }
    LaunchedEffect(Unit) { back.requestFocus() }

    PivotScroll(margin = 0.16f) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Dim.Side, end = Dim.Side, top = Dim.Top, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        item {
            AppleSubHeader(
                title = s.billTitle,
                trailing = "${s.roomWord} ${guest.roomNo}",
                onBack = onBack,
                backModifier = Modifier.focusRequester(back),
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.folioHeading, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                Text(
                    buildString {
                        append(guest.name)
                        screenData?.stay?.departure?.let {
                            append("  ·  ${s.checkOutLabel} ${it.toLocalDateTimeLabel()}")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge, color = TextSecondary,
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                colors = SurfaceDefaults.colors(containerColor = SurfaceTile1),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 36.dp, vertical = 28.dp)) {
                    val rows = screenData?.folio?.toRows(s).orEmpty()
                    if (rows.isEmpty()) {
                        Text(
                            s.folioUnavailable,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextSecondary,
                        )
                    }
                    rows.forEachIndexed { i, row ->
                        FolioLine(row)
                        if (i != rows.lastIndex) {
                            Spacer(Modifier.height(14.dp))
                            Hairline()
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                    screenData?.folio?.let { folio ->
                        Spacer(Modifier.height(22.dp))
                        Hairline()
                        Spacer(Modifier.height(22.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.balanceLabel, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                            Text(folio.total.toVnd(), style = MaterialTheme.typography.headlineLarge, color = GoldBright)
                        }
                    }
                }
            }
        }

        item {
            ApplePillButton(text = s.done, onClick = onBack, filled = false)
        }
    }
    }
}

@Composable
private fun FolioLine(row: FolioLineData) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(row.label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            if (row.deduction) "−${row.amount.toVnd()}" else row.amount.toVnd(),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.End,
        )
    }
}

private data class FolioLineData(val label: String, val amount: Long, val deduction: Boolean = false)

private fun ScreenFolioDto.toRows(s: UiStrings): List<FolioLineData> = buildList {
    add(FolioLineData(s.folioRoom, room))
    add(FolioLineData(s.folioServices, service))
    if (surcharge != 0L) add(FolioLineData(s.folioSurcharge, surcharge))
    if (discount != 0L) add(FolioLineData(s.folioDiscount, discount, deduction = true))
    if (deposit != 0L) add(FolioLineData(s.folioDeposit, deposit, deduction = true))
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(HairlineOnDark))
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun FolioPreview() {
    FolioScreen(GuestProfile("Nguyen Van An", "Mr.", "302", "VNM", false), onBack = {})
}
