@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

// ── tvOS canvas ─────────────────────────────────────────────────────────────────
// tvOS is dark by default — a deep, slightly-warm near-black, never pure #000 except
// in true void (video). Text rides white at 100 / 62 / 30 % opacity. Accent: Action Blue.

// Deep, premium dark with a faint cool undertone and a top-down gradient for depth —
// rich rather than flat gray, so photographic posters and the accent blue pop.
import androidx.compose.runtime.CompositionLocalProvider

// Deep, premium dark with a faint cool undertone and a top-down gradient for depth —
// rich rather than flat gray, so photographic posters and the accent blue pop.
val TvBackground   = Color(0xFF0C0D11)   // canvas (deep)
val TvBackgroundTop = Color(0xFF1A1D26)  // lighter, faintly blue top — gives the gradient depth
val TextPrimaryDark = Color(0xFFFFFFFF)                    // 100% — primary label on dark
val TextSecondaryDark = Color(0xFFFFFFFF).copy(alpha = 0.60f) // 60% — secondary label on dark
val TextPrimaryLight = Color(0xFF1D1D1F)                   // Apple Ink Charcoal on light
val TextSecondaryLight = Color(0xFF1D1D1F).copy(alpha = 0.60f) // 60% — secondary label on light

// Dynamic text color accessor depending on theme mode
val TextPrimary: Color
    @Composable get() = if (LocalAppThemeMode.current == AppThemeMode.LIGHT) TextPrimaryLight else TextPrimaryDark

val TextSecondary: Color
    @Composable get() = if (LocalAppThemeMode.current == AppThemeMode.LIGHT) TextSecondaryLight else TextSecondaryDark

val TextTertiary: Color
    @Composable get() = if (LocalAppThemeMode.current == AppThemeMode.LIGHT) Color(0xFF1D1D1F).copy(alpha = 0.35f) else Color(0xFFFFFFFF).copy(alpha = 0.30f)

val TextQuaternary: Color
    @Composable get() = if (LocalAppThemeMode.current == AppThemeMode.LIGHT) Color(0xFF1D1D1F).copy(alpha = 0.20f) else Color(0xFFFFFFFF).copy(alpha = 0.18f)

// Research §1, §7: focus changes animate over ~0.15s ease-in-out (the UI's heartbeat).
val FocusEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)  // Apple ease-in-out
const val FocusDurationMs = 150

private val TvDarkScheme = darkColorScheme(
    primary            = Gold,
    onPrimary          = OnGold,
    primaryContainer   = GoldBright,
    onPrimaryContainer = OnGold,
    secondary          = GoldBright,
    onSecondary        = OnGold,
    background         = TvBackground,
    onBackground       = TextPrimaryDark,
    surface            = SurfaceTile1,
    onSurface          = TextPrimaryDark,
    surfaceVariant     = DarkCardRaised,
    onSurfaceVariant   = TextSecondaryDark,
    border             = HairlineOnDark,
    error              = Color(0xFFFF453A),
)

// Light "parchment" tile — Apple Light theme (design.md alternation).
private val ParchmentScheme = lightColorScheme(
    primary            = GoldDeep,
    onPrimary          = Color.White,
    primaryContainer   = Gold,
    onPrimaryContainer = OnGold,
    secondary          = GoldDeep,
    onSecondary        = Color.White,
    background         = Parchment,
    onBackground       = TextPrimaryLight,
    surface            = Canvas,
    onSurface          = TextPrimaryLight,
    surfaceVariant     = Parchment,
    onSurfaceVariant   = TextSecondaryLight,
    border             = Hairline,
    error              = Color(0xFFB3261E),
)

@Composable
fun PhsAppTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val isLight = themeMode == AppThemeMode.LIGHT
    val scheme = if (isLight) ParchmentScheme else TvDarkScheme
    val bgTop = if (isLight) Color(0xFFFFFFFF) else TvBackgroundTop
    val bgBottom = if (isLight) Parchment else TvBackground

    CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
        MaterialTheme(colorScheme = scheme, typography = PhsTypography) {
            Surface(modifier = Modifier.fillMaxSize(), colors = surfaceColorsBackground()) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.0f to bgTop,
                            0.45f to bgBottom,
                            1.0f to bgBottom,
                        ),
                    ),
                ) { content() }
            }
        }
    }
}

// Hero screens (Welcome / Idle) keep the same cinematic dark canvas in the tvOS look.
@Composable
fun PhsHeroTheme(content: @Composable () -> Unit) = PhsAppTheme(themeMode = AppThemeMode.DARK, content = content)

// Bright parchment section, when a screen wants the light-tile half of the rhythm.
@Composable
fun PhsParchmentTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ParchmentScheme, typography = PhsTypography) {
        Surface(modifier = Modifier.fillMaxSize(), colors = surfaceColorsBackground()) { content() }
    }
}

@Composable
private fun surfaceColorsBackground() = androidx.tv.material3.SurfaceDefaults.colors(
    containerColor = MaterialTheme.colorScheme.background,
    contentColor   = MaterialTheme.colorScheme.onBackground,
)

// ── tvOS focus motion ───────────────────────────────────────────────────────────
// The signature tvOS behaviour: a focused element scales up and lifts. We expose the
// raw scale driver here; ApplePoster/ApplePillButton (AppleComponents.kt) layer the
// shadow + specular rim on top. Use for bespoke focusables; TV Material Card/Button
// already animate their own focus.
@Composable
fun Modifier.tvFocusScale(focusedScale: Float = 1.10f, onFocusChanged: (Boolean) -> Unit = {}): Modifier {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (focused) focusedScale else 1f,
        animationSpec = tween(FocusDurationMs, easing = FocusEasing),
        label         = "tvFocusScale",
    )
    return this
        .scale(scale)
        .onFocusChanged { focused = it.isFocused; onFocusChanged(it.isFocused) }
}

/** Animates a color toward [focused]/[unfocused] on the tvOS focus curve. */
@Composable
fun animateFocusColor(focused: Boolean, focusedColor: Color, unfocusedColor: Color): Color {
    val c by animateColorAsState(
        targetValue   = if (focused) focusedColor else unfocusedColor,
        animationSpec = tween(FocusDurationMs, easing = FocusEasing),
        label         = "tvFocusColor",
    )
    return c
}
