@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.services

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.ContentServiceDto
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.common.ContentStatePanel
import vn.phs.iptv.ui.content.forLang
import vn.phs.iptv.ui.content.resolvedHours
import vn.phs.iptv.ui.content.resolvedImageUrl
import vn.phs.iptv.ui.i18n.LocalUiStrings
import vn.phs.iptv.ui.theme.AppleSubHeader
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.Gold
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.PivotScroll
import vn.phs.iptv.ui.theme.SurfaceTile1
import vn.phs.iptv.ui.theme.SurfaceTile2
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.TextTertiary

@Composable
fun ServicesScreen(
    guest: GuestProfile,
    contentData: ContentResponse? = null,
    language: AppLanguage = AppLanguage.VI,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    PhsAppTheme { ServicesContent(guest, contentData, language, onBack) }
}

@Composable
private fun ServicesContent(
    guest: GuestProfile,
    contentData: ContentResponse?,
    language: AppLanguage,
    onBack: () -> Unit,
) {
    val s = LocalUiStrings.current
    val back = remember { FocusRequester() }
    val services = contentData?.services.orEmpty()
    var expandedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { back.requestFocus() }

    PivotScroll(margin = 0.16f) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Dim.Side, end = Dim.Side, top = Dim.Top, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AppleSubHeader(
                    title = s.servicesTitle,
                    trailing = "${s.roomWord} ${guest.roomNo}",
                    onBack = onBack,
                    backModifier = Modifier.focusRequester(back),
                )
            }
            item { Text(s.servicesTitle, style = MaterialTheme.typography.displayMedium, color = TextPrimary) }

            if (contentData == null) {
                item {
                    ContentStatePanel(
                        title = localText(language, "Đang tải nội dung khách sạn…", "Loading hotel content…", "Загрузка…"),
                    )
                }
            } else if (services.isEmpty()) {
                item {
                    ContentStatePanel(
                        title = localText(
                            language,
                            "Khách sạn chưa cấu hình dịch vụ trên PMS.",
                            "Hotel services have not been configured in PMS.",
                            "Услуги отеля ещё не настроены.",
                        ),
                    )
                }
            } else {
                itemsIndexed(services) { index, service ->
                    val id = "$index:${service.title.vi}:${service.title.en}"
                    ServiceRow(
                        service = service,
                        language = language,
                        expanded = expandedId == id,
                        onClick = { expandedId = if (expandedId == id) null else id },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(
    service: ContentServiceDto,
    language: AppLanguage,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = service.title.forLang(language)
    val description = service.description.forLang(language)
    val hours = service.resolvedHours()
    val imageUrl = service.resolvedImageUrl()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(150), label = "serviceScale")

    Column {
        Surface(
            onClick = onClick,
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = SurfaceTile1,
                focusedContainerColor = SurfaceTile2,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, GoldBright), shape = RoundedCornerShape(16.dp)),
            ),
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .onFocusChanged { focused = it.isFocused },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.width(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (description.isNotBlank()) {
                        Text(description, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
                if (hours.isNotBlank()) {
                    Spacer(Modifier.width(18.dp))
                    HoursChip(hours)
                }
            }
        }

        AnimatedVisibility(visible = expanded && (description.isNotBlank() || hours.isNotBlank())) {
            Column(Modifier.padding(start = 22.dp, top = 12.dp, end = 22.dp, bottom = 4.dp)) {
                if (description.isNotBlank()) {
                    Text(description, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                }
                if (hours.isNotBlank()) {
                    Text(hours, style = MaterialTheme.typography.bodyMedium, color = GoldBright)
                }
            }
        }
    }
}

@Composable
private fun HoursChip(text: String) {
    Box(
        Modifier.clip(RoundedCornerShape(100.dp)).background(Gold.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = TextUnit(0.5f, TextUnitType.Sp),
            ),
            color = GoldBright,
        )
    }
}

private fun localText(language: AppLanguage, vi: String, en: String, ru: String): String = when (language) {
    AppLanguage.VI -> vi
    AppLanguage.EN -> en
    AppLanguage.RU -> ru
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun ServicesPreview() {
    ServicesScreen(GuestProfile("Nguyen Van An", "Mr.", "302", "VNM", false), onBack = {})
}
