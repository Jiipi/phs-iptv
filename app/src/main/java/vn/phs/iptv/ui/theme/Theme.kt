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
val TvBackground   = Color(0xFF0C0D11)   // canvas (deep)
val TvBackgroundTop = Color(0xFF1A1D26)  // lighter, faintly blue top — gives the gradient depth
val TextPrimary    = Color(0xFFFFFFFF)                    // 100% — primary label
val TextSecondary  = Color(0xFFFFFFFF).copy(alpha = 0.60f) // 60% — secondary label
val TextTertiary   = Color(0xFFFFFFFF).copy(alpha = 0.30f) // 30% — tertiary label
val TextQuaternary = Color(0xFFFFFFFF).copy(alpha = 0.18f) // 18% — quaternary

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
    onBackground       = TextPrimary,
    surface            = SurfaceTile1,
    onSurface          = TextPrimary,
    surfaceVariant     = DarkCardRaised,
    onSurfaceVariant   = TextSecondary,
    border             = HairlineOnDark,
    error              = Color(0xFFFF453A),
)

// Light "parchment" tile — used for the rare bright section (design.md alternation).
private val ParchmentScheme = lightColorScheme(
    primary            = ActionBlue,
    onPrimary          = Color.White,
    background         = Parchment,
    onBackground       = Ink,
    surface            = Canvas,
    onSurface          = Ink,
    surfaceVariant     = Parchment,
    onSurfaceVariant   = InkMuted80,
    border             = Hairline,
    error              = Color(0xFFB3261E),
)

@Composable
fun PhsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TvDarkScheme, typography = PhsTypography) {
        Surface(modifier = Modifier.fillMaxSize(), colors = surfaceColorsBackground()) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.0f to TvBackgroundTop,
                        0.45f to TvBackground,
                        1.0f to TvBackground,
                    ),
                ),
            ) { content() }
        }
    }
}

// Hero screens (Welcome / Idle) keep the same cinematic dark canvas in the tvOS look.
@Composable
fun PhsHeroTheme(content: @Composable () -> Unit) = PhsAppTheme(content)

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
