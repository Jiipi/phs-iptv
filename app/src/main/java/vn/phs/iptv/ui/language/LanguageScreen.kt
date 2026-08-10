@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.language

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.theme.AmbientVideoBackground
import vn.phs.iptv.ui.theme.AppThemeMode
import vn.phs.iptv.ui.theme.FocusDurationMs
import vn.phs.iptv.ui.theme.FocusEasing
import vn.phs.iptv.ui.theme.flagRes
import vn.phs.iptv.ui.theme.Gold
import vn.phs.iptv.ui.theme.GoldBright
import vn.phs.iptv.ui.theme.GoldDeep
import vn.phs.iptv.ui.theme.LocalAppThemeMode
import vn.phs.iptv.ui.theme.OnGold
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary
import vn.phs.iptv.ui.theme.TextTertiary
import vn.phs.iptv.ui.theme.TvBackground

/**
 * Combined welcome + language picker — the first thing a guest sees. It now shares the
 * Idle screen's living backdrop: the looping ambient hotel footage, calmed by a single
 * clean scrim so text and cards stay legible. The composition is deliberately spare —
 * a quiet brand lockup, the guest's name, and three frosted-glass cards — so it reads as
 * five-star restraint rather than decoration. The focused card lifts behind a gold ring
 * while its siblings recede. When [guest] is supplied (post check-in) it greets by name,
 * folding the old standalone Welcome screen into this one; when null it is the bare
 * re-select picker reached from Home.
 *
 * [backdropVideoUrl] is the hotel's own intro film when the PMS has uploaded one; the
 * ambient loop bundled in res/raw stands in when it hasn't.
 */
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn

@Composable
fun LanguageScreen(
    onSelected: (AppLanguage) -> Unit,
    guest: GuestProfile? = null,
    backdropVideoUrl: String? = null,
    themeMode: AppThemeMode = AppThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    BackHandler { onBack?.invoke() }
    PhsAppTheme(themeMode = themeMode) { LanguageContent(onSelected, guest, backdropVideoUrl, themeMode, onToggleTheme) }
}

@Composable
private fun LanguageContent(
    onSelected: (AppLanguage) -> Unit,
    guest: GuestProfile?,
    backdropVideoUrl: String?,
    themeMode: AppThemeMode,
    onToggleTheme: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }

    // Which card holds focus — lets the others dim so the choice pops.
    var focusedIndex by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        AmbientVideoBackground(Modifier.fillMaxSize(), videoUrl = backdropVideoUrl)
        // One calm cinematic scrim — darker at the edges, lifts in the middle so the
        // footage breathes while the lockup and cards stay perfectly legible.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to TvBackground.copy(alpha = 0.55f),
                    0.5f to TvBackground.copy(alpha = 0.30f),
                    1f to TvBackground.copy(alpha = 0.70f),
                ),
            ),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 80.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Brand lockup with a hairline gold rule ──────────────────────────
            Text(
                "PHS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = TextUnit(3f, TextUnitType.Sp),
                ),
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .width(54.dp)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "FIVE-STAR HOTEL · HANOI",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = TextUnit(4.5f, TextUnitType.Sp), fontWeight = FontWeight.Medium,
                ),
                color = Gold.copy(alpha = 0.9f),
            )

            if (guest != null) {
                Spacer(Modifier.height(22.dp))
                Text(
                    "${guest.title} ${guest.name}",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Welcome  ·  Chào mừng  ·  Добро пожаловать",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    textAlign = TextAlign.Center,
                )
                if (guest.isBirthday) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "🎂  Happy birthday from all of us",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldBright,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Spacer(Modifier.height(22.dp))
                Text(
                    "Select your language",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Chọn ngôn ngữ  ·  Выберите язык",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.70f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                AppLanguage.entries.forEachIndexed { index, lang ->
                    LanguageCard(
                        language = lang,
                        recede = focusedIndex != index,
                        onClick = { onSelected(lang) },
                        onFocused = { focusedIndex = index },
                        modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            ThemeTogglePill(
                themeMode = themeMode,
                onToggleTheme = onToggleTheme,
            )
        }
    }
}

@Composable
private fun ThemeTogglePill(
    themeMode: AppThemeMode,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = themeMode == AppThemeMode.LIGHT
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "themePill",
    )
    val pillBg = if (isLight) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.55f)
    val pillText = if (isLight) Color(0xFF111113) else Color.White

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(100.dp))
            .background(if (focused) GoldBright else pillBg)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) GoldBright else Color.White.copy(alpha = 0.25f)), RoundedCornerShape(100.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onToggleTheme),
    ) {
        Box(Modifier.heightIn(min = 44.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Chế độ giao diện: ${if (isLight) "Sáng (Light)" else "Tối (Dark)"}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (focused) OnGold else pillText,
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    recede: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "scale")
    val alpha by animateFloatAsState(if (recede && !focused) 0.85f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "alpha")

    val cardShape = RoundedCornerShape(26.dp)
    val cardBg = if (isLight) {
        if (focused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.72f)
    } else {
        if (focused) Color(0xFF1E222D).copy(alpha = 0.85f) else Color(0xFF12141C).copy(alpha = 0.65f)
    }
    val cardBorder = if (focused) GoldBright else (if (isLight) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f))
    val endonymColor = if (isLight) Color(0xFF111113) else Color.White
    val subtitleColor = if (isLight) GoldDeep else (if (focused) GoldBright else Gold.copy(alpha = 0.90f))

    Box(
        modifier = modifier
            .size(width = 244.dp, height = 268.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(cardShape)
            .background(cardBg)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, cardBorder), cardShape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(language.flagRes()),
                contentDescription = language.english,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(2.dp, if (focused) GoldBright else Color.White.copy(alpha = 0.30f)),
                        CircleShape,
                    ),
            )

            Spacer(Modifier.height(20.dp))
            Text(
                language.endonym,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = endonymColor,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                language.english.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = TextUnit(1.5f, TextUnitType.Sp), fontWeight = FontWeight.Bold),
                color = subtitleColor,
            )
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun LanguagePreview() {
    LanguageScreen(onSelected = {}, guest = GuestProfile("Nguyen Van An", "Mr.", "302", "VNM", false))
}
