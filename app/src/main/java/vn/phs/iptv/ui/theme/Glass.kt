package vn.phs.iptv.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// ════════════════════════════════════════════════════════════════════════════════
//  Glass surface kit — design.md applied to a dark 10-foot UI.
//
//  design.md is written for a light web canvas with a blue accent; PRD §9 fixes this
//  app to a dark canvas with a gold accent. What carries over is the STRUCTURE: one
//  radius ladder, one accent, one drop-shadow (reserved for imagery), and elevation
//  expressed as a change of surface rather than as chrome.
//
//  Cards here are translucent white over the blurred backdrop, not opaque fills, so
//  the room photography behind them still tints the UI. Depth comes from opacity.
// ════════════════════════════════════════════════════════════════════════════════

// ── Radius ladder ───────────────────────────────────────────────────────────────
// Replaces the ad-hoc 14 / 16 / 18 / 22dp values that had accumulated across screens.
val GlassRadiusSm = 8.dp    // chips, inline imagery
val GlassRadiusMd = 12.dp   // nav rail rows, posters
val GlassRadiusLg = 18.dp   // action tiles, QR panel
val GlassRadiusXl = 24.dp   // the large arrival cards
val GlassRadiusPill = 100.dp

// ── Glass tones ─────────────────────────────────────────────────────────────────
// White at low alpha over the backdrop. The focused step is roughly double the resting
// alpha — enough to read as "lifted" across a room without needing a shadow.
val GlassFill = Color.White.copy(alpha = 0.07f)
val GlassFillFocused = Color.White.copy(alpha = 0.14f)
val GlassBorder = Color.White.copy(alpha = 0.12f)
val GlassBorderFocused = Color.White.copy(alpha = 0.22f)

/** The top-down sheen that makes a flat translucent panel read as a pane of glass. */
private val GlassSheen = Brush.verticalGradient(
    0.0f to Color.White.copy(alpha = 0.10f),
    0.55f to Color.Transparent,
)

/**
 * The standard glass panel: translucent fill + sheen + hairline rim, all clipped to
 * [shape]. Pass [focused] to brighten the pane instead of adding a shadow — per
 * design.md, shadow belongs to photography alone.
 *
 * Apply this INSTEAD of a `containerColor`; the hosting Surface should be transparent.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    focused: Boolean = false,
): Modifier {
    val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT

    // ── Light Mode: frosted cream card with drop shadow ─────────────────────
    // Dark Mode: translucent white at low alpha (unchanged)
    val fillAlpha by animateFloatAsState(
        targetValue = if (isLight) {
            if (focused) 0.92f else 0.85f       // near-opaque frosted cream
        } else {
            if (focused) 0.14f else 0.07f       // translucent dark glass
        },
        animationSpec = tween(FocusDurationMs, easing = FocusEasing),
        label = "glassFill",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isLight) {
            if (focused) 0.70f else 0.55f       // visible Apple System Gray 4
        } else {
            if (focused) 0.22f else 0.12f       // subtle white hairline
        },
        animationSpec = tween(FocusDurationMs, easing = FocusEasing),
        label = "glassBorder",
    )
    // Light: elevated shadow gives depth that opacity alone cannot provide on a bright canvas.
    val shadowElevation by animateFloatAsState(
        targetValue = if (isLight) (if (focused) 12f else 6f) else 0f,
        animationSpec = tween(FocusDurationMs, easing = FocusEasing),
        label = "glassShadow",
    )

    val baseFillColor = if (isLight) LightCardFill else Color.White
    val borderColor = if (focused) Gold else (if (isLight) LightCardBorder.copy(alpha = borderAlpha) else Color.White.copy(alpha = borderAlpha))
    val glassSheen = if (isLight) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.45f),   // strong top sheen
            0.35f to Color.White.copy(alpha = 0.08f),   // fast fade
            0.55f to Color.Transparent,
        )
    } else GlassSheen

    return this
        .graphicsLayer {
            this.shadowElevation = shadowElevation
            this.shape = shape as androidx.compose.ui.graphics.Shape
            this.clip = false
            this.spotShadowColor = Color(0x26000000)   // 15% black shadow
            this.ambientShadowColor = Color(0x14000000) // 8% ambient
        }
        .clip(shape)
        .background(baseFillColor.copy(alpha = fillAlpha))
        .background(glassSheen)
        .border(BorderStroke(if (isLight) 1.dp else 1.dp, borderColor), shape)
}

/**
 * PRD §9.5 focus treatment in one place: scale 1.06 and a 3dp gold ring, animated over
 * [FocusDurationMs]. Every D-pad target must carry it.
 *
 * The scale lives here rather than on the TV Material Surface because `focusedScale`
 * scales the ring with the card, which thickens the stroke; a graphicsLayer scale under
 * a separately-drawn border keeps the ring at a true 3dp at every scale.
 */
@Composable
fun Modifier.tvFocusRing(
    focused: Boolean,
    shape: Shape,
    scaleTo: Float = 1.06f,
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (focused) scaleTo else 1f,
        animationSpec = tween(FocusDurationMs, easing = FocusEasing),
        label = "focusRing",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (focused) Modifier.border(BorderStroke(3.dp, GoldBright), shape) else Modifier)
}
