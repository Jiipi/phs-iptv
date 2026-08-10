@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import vn.phs.iptv.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** Decode size for the backdrop wash — 16:9, small enough that upscaling *is* the blur. */
private const val BackdropSampleW = 32
private const val BackdropSampleH = 18

// ════════════════════════════════════════════════════════════════════════════════
//  Apple tvOS component kit  (numbers verified in research §1–§8)
//  Focus signature = scale 1.10× + large soft shadow + brighten + specular sheen,
//  all over 0.15 s ease-in-out (FocusDurationMs). Real photography fills posters.
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Dynamic Apple-TV backdrop: the focused artwork blurred + darkened behind everything,
 * giving the home screen colour and depth (never a flat black panel). Crossfades softly.
 *
 * The blur is done by DOWNSAMPLING, not by `Modifier.blur`: that modifier needs a
 * RenderEffect and silently draws the image sharp below API 31, and the hotel boxes we
 * ship to run Android 10. Decoding at [BackdropSampleW]x[BackdropSampleH] and letting
 * ContentScale.Crop stretch it to 1920x1080 gives the same soft wash on every API level,
 * and costs less than a real blur because the bitmap genuinely is that small.
 */
@Composable
fun AppleDynamicBackdrop(imageUrl: String?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Crossfade(targetState = imageUrl, animationSpec = tween(700), label = "backdrop") { url ->
            if (url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .size(BackdropSampleW, BackdropSampleH)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // Bilinear on upscale — without it the enlarged pixels stay square.
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Darken vertically for legibility + depth. Lighter than it used to be: the
        // backdrop is genuinely soft now, so less scrim is needed to read over it, and
        // Scrim layer: Dark scrim in Dark Mode, lighter warm wash in Light Mode
        // so the photo still tints the canvas with color and warmth.
        val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT
        val scrimBase = if (isLight) Color(0xFFF2F2F5) else TvBackground
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to scrimBase.copy(alpha = if (isLight) 0.38f else 0.46f),
                    0.55f to scrimBase.copy(alpha = if (isLight) 0.62f else 0.76f),
                    1f to scrimBase.copy(alpha = if (isLight) 0.82f else 0.96f),
                ),
            ),
        )
        // Left scrim so the headline column reads cleanly
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to scrimBase.copy(alpha = if (isLight) 0.35f else 0.5f), 0.65f to Color.Transparent),
            ),
        )
    }
}

/**
 * tvOS "lockup" — a photographic poster with the title (+ subtitle) below. The whole
 * unit is the focus target. On focus the poster lifts, glosses, and the title brightens.
 */
@Composable
fun ApplePoster(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    height: Dp = 169.dp,           // 16:9
    badge: String? = null,
    progress: Float? = null,
    dimmed: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.10f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "scale",
    )
    val elevation by animateDpAsState(
        if (focused) 24.dp else 0.dp, tween(FocusDurationMs, easing = FocusEasing), label = "elev",
    )
    val sheen by animateFloatAsState(
        if (focused) 0.20f else 0f, tween(FocusDurationMs, easing = FocusEasing), label = "sheen",
    )
    val titleColor = animateFocusColor(focused, TextPrimary, TextSecondary)

    Column(modifier = modifier.width(width)) {
        Surface(
            onClick = onClick,
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(GlassRadiusMd)),
            colors = ClickableSurfaceDefaults.colors(containerColor = SurfaceTile1, focusedContainerColor = SurfaceTile1),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, GoldBright), shape = RoundedCornerShape(GlassRadiusMd)),
            ),
            modifier = Modifier
                .size(width, height)
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    shadowElevation = elevation.toPx()
                    spotShadowColor = Color.Black; ambientShadowColor = Color.Black
                }
                .onFocusChangedTo { focused = it },
        ) {
            Box(Modifier.fillMaxSize()) {
                PhotoFill(imageUrl, accent, dimmed)
                // Brighten on focus (research §1) + bottom scrim for any in-art legibility
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f))),
                    ),
                )
                if (focused) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer { alpha = sheen }.background(
                            Brush.linearGradient(listOf(Color.White, Color.Transparent)),
                        ),
                    )
                }
                if (badge != null) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart).padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                if (progress != null) {
                    Box(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp)
                            .background(Color.Black.copy(alpha = 0.45f)),
                    ) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(GoldBright))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            title, style = MaterialTheme.typography.titleMedium,
            color = if (dimmed) TextTertiary else titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Featured hero billboard — large photographic banner with title overlay. */
@Composable
fun AppleHeroBillboard(
    eyebrow: String,
    title: String,
    body: String,
    imageUrl: String?,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        if (focused) 30.dp else 6.dp, tween(FocusDurationMs, easing = FocusEasing), label = "heroElev",
    )
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(GlassRadiusXl)),
        colors = ClickableSurfaceDefaults.colors(containerColor = SurfaceTile1, focusedContainerColor = SurfaceTile1),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, GoldBright), shape = RoundedCornerShape(GlassRadiusXl)),
        ),
        modifier = modifier
            .fillMaxWidth().height(248.dp)
            .graphicsLayer { shadowElevation = elevation.toPx(); spotShadowColor = Color.Black }
            .onFocusChangedTo { focused = it },
    ) {
        Box(Modifier.fillMaxSize()) {
            PhotoFill(imageUrl, accent, dimmed = false)
            // Left-to-right scrim so the headline reads over the photo
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.30f), Color.Transparent)),
                ),
            )
            Column(Modifier.align(Alignment.CenterStart).padding(start = 56.dp).fillMaxWidth(0.62f)) {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.displayMedium, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.82f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Photo background with a graceful accent-gradient fallback while loading / when null. */
@Composable
private fun PhotoFill(imageUrl: String?, accent: Color, dimmed: Boolean) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(accent.copy(alpha = if (dimmed) 0.22f else 0.9f), accent.copy(alpha = if (dimmed) 0.10f else 0.45f))),
        ),
    )
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (dimmed) 0.4f else 1f },
        )
    }
}

/** Signature Action Blue pill (design.md button-primary) — content perfectly centred. */
@Composable
fun ApplePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: String? = null,
    filled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "pill",
    )
    val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(100.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (filled) Gold else (if (isLight) LightNavRailBg else Color.White.copy(alpha = 0.12f)),
            focusedContainerColor = GoldBright,
            contentColor = if (filled) OnGold else TextPrimary,
            focusedContentColor = OnGold,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        interactionSource = remember { MutableInteractionSource() },
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChangedTo { focused = it },
    ) {
        Box(
            modifier = Modifier.heightIn(min = 58.dp).padding(horizontal = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    Text(leading, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

/** Back affordance for sub-pages — a circular chevron at top-left. */
@Composable
fun AppleBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.08f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "back",
    )
    val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(100.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isLight) LightNavRailBg else Color.White.copy(alpha = 0.12f),
            focusedContainerColor = GoldBright,
            contentColor = TextPrimary, focusedContentColor = OnGold,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = if (isLight) Border(BorderStroke(1.dp, LightCardBorder), shape = RoundedCornerShape(100.dp)) else Border.None,
            focusedBorder = Border(BorderStroke(2.dp, GoldBright), shape = RoundedCornerShape(100.dp)),
        ),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChangedTo { focused = it },
    ) {
        Box(Modifier.heightIn(min = 52.dp).padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    vn.phs.iptv.ui.i18n.LocalUiStrings.current.back,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/**
 * Premium guest header — an eyebrow greeting + the guest's name set large, with a
 * "key-card" room chip and the live clock/date on the right. The hero of the home page.
 */
@Composable
fun AppleGuestHeader(
    eyebrow: String,
    name: String,
    subtitle: String,
    roomNo: String,
    clock: String,
    date: String,
    language: vn.phs.iptv.domain.AppLanguage = vn.phs.iptv.domain.AppLanguage.EN,
    onLanguage: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    languageModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = Gold,
            )
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.displaySmall, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
        }

        // Right cluster — language pill plus metadata
        Column(horizontalAlignment = Alignment.End) {
            LanguagePill(language = language, onClick = onLanguage, modifier = languageModifier)
            Spacer(Modifier.height(18.dp))
            Text(
                "${vn.phs.iptv.ui.i18n.LocalUiStrings.current.roomWord} $roomNo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Gold,
            )
            Spacer(Modifier.height(3.dp))
            Text("$clock  ·  $date", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

/**
 * Compact language switcher pill — globe + current language, with the gold focus ring.
 * Lives at the top-right of the guest header; opens the full language picker on select.
 */
/**
 * Bundled flag artwork for a language — local drawables so flags always render, even on
 * a locked-down hotel network or an Android TV whose system font lacks flag emoji glyphs.
 */
fun vn.phs.iptv.domain.AppLanguage.flagRes(): Int = when (this) {
    vn.phs.iptv.domain.AppLanguage.VI -> R.drawable.flag_vn
    vn.phs.iptv.domain.AppLanguage.EN -> R.drawable.flag_gb
    vn.phs.iptv.domain.AppLanguage.RU -> R.drawable.flag_ru
}

@Composable
fun LanguagePill(
    language: vn.phs.iptv.domain.AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "langPill",
    )
    val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(100.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isLight) LightNavRailBg else Color.White.copy(alpha = 0.10f),
            focusedContainerColor = GoldBright,
            contentColor = TextPrimary, focusedContentColor = OnGold,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (isLight) LightCardBorder else Color.White.copy(alpha = 0.16f)), shape = RoundedCornerShape(100.dp)),
            focusedBorder = Border(BorderStroke(2.dp, GoldBright), shape = RoundedCornerShape(100.dp)),
        ),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChangedTo { focused = it },
    ) {
        Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(language.flagRes()),
                    contentDescription = language.english,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    language.endonym,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.width(10.dp))
                Text("⌄", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ThemeTogglePill(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = LocalAppThemeMode.current == AppThemeMode.LIGHT
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.06f else 1f, tween(FocusDurationMs, easing = FocusEasing), label = "themePill",
    )
    val pillBg = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.10f)
    val pillBorder = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.16f)

    Surface(
        onClick = onToggleTheme,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(100.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = pillBg,
            focusedContainerColor = GoldBright,
            contentColor = TextPrimary,
            focusedContentColor = OnGold,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, pillBorder), shape = RoundedCornerShape(100.dp)),
            focusedBorder = Border(BorderStroke(2.dp, GoldBright), shape = RoundedCornerShape(100.dp)),
        ),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChangedTo { focused = it },
    ) {
        Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isLight) "☀️ Light" else "🌙 Dark",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/** Flat top bar: wordmark left, live clock right. */
@Composable
fun AppleTopBar(leading: String, trailing: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(leading, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        Text(trailing, style = MaterialTheme.typography.titleLarge, color = TextSecondary)
    }
}

/** Left-aligned shelf header. */
@Composable
fun AppleSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineLarge, color = TextPrimary, modifier = modifier)
}

// ── Layout system (single source of truth — best-practice 10-foot spacing) ───────
object Dim {
    val Side = 80.dp        // overscan-safe left/right inset — identical on every screen
    val Top = 56.dp         // top inset
    val Bottom = 56.dp
    val ShelfGap = 36.dp    // vertical gap between shelves
    val HeaderGap = 16.dp   // gap between a shelf header and its row
    val PosterGap = 24.dp   // gap between posters in a row
    val FocusPad = 26.dp    // vertical breathing room so the focus scale never clips
    val PosterW = 300.dp
    val PosterH = 169.dp    // 16:9
    val PortraitW = 200.dp
    val PortraitH = 300.dp  // 2:3

    // Home "arrival" block — header + hero/stay row + action row must all fit the 540dp-tall
    // 1080p viewport. Measured on the box: 24 top + 97 header + 20 + 230 + 20 + 110 = 501dp,
    // leaving ~39dp so a guest name that wraps to two lines still cannot push the action row
    // under the bottom edge.
    val HeroW = 300.dp        // room-type / intro-film panel beside the stay card
    val ArrivalH = 230.dp     // shared height of that row
    val ActionTileH = 110.dp  // one action tile
}

/**
 * tvOS-style focus scrolling done right. Two failure modes to avoid:
 *  • Compose's default scrolls the *minimum* amount → item parks flush against the edge.
 *  • Re-centring on every move → the whole UI visibly jitters up/down each keypress.
 * This spec keeps a comfortable [margin] band at both edges and **does nothing while the
 * focused item already sits inside that band** — so navigation is stable (no jitter), yet
 * an item nearing an edge is nudged just enough to keep the margin (never hugs the edge).
 * Works on both axes: wrap a LazyColumn → vertical; a LazyRow → horizontal.
 */
@OptIn(ExperimentalFoundationApi::class)
private class EdgeMarginBringIntoView(private val marginFraction: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val margin = containerSize * marginFraction
        val leading = offset
        val trailing = offset + size
        val safeTrailing = containerSize - margin
        // Item taller/wider than the comfort band: just keep it covering the viewport.
        if (size >= containerSize - 2 * margin) {
            return when {
                leading > margin -> leading - margin
                trailing < safeTrailing -> trailing - safeTrailing
                else -> 0f
            }
        }
        return when {
            leading >= margin && trailing <= safeTrailing -> 0f   // comfortably inside → hold still
            leading < margin -> leading - margin                  // near leading edge → nudge in
            else -> trailing - safeTrailing                       // near trailing edge → nudge in
        }
    }
}

/**
 * Centres the focused item in the viewport — the Apple-TV "the row you're on sits in the
 * middle" feel. To avoid the jitter that plagues naive centring (re-scrolling a hair on
 * every keypress), it ignores movements while the item is already within [deadbandFraction]
 * of dead-centre: real row-to-row jumps re-centre, tiny re-focuses (e.g. moving left/right
 * inside the row) hold perfectly still.
 *
 * [topAnchor] px are pinned: a request whose target already sits fully below the top of the
 * viewport is never allowed to scroll the column *up*. Home relies on this — the request
 * that arrives is the small focused tile, not the block it belongs to, so plain centring
 * would happily hoist a 110dp action tile to mid-screen and take the guest's name, room and
 * check-out dates off the top with it. Downward scrolling (bringing something below the fold
 * into view) is untouched.
 */
@OptIn(ExperimentalFoundationApi::class)
private class CenterBringIntoView(
    private val deadbandFraction: Float,
    private val topAnchor: Boolean = false,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (size >= containerSize) {
            // Taller than the viewport: just keep it covering, never centre.
            return when {
                offset > 0f -> offset
                offset + size < containerSize -> offset + size - containerSize
                else -> 0f
            }
        }
        val desiredLeading = (containerSize - size) / 2f
        val delta = offset - desiredLeading
        val deadband = containerSize * deadbandFraction
        if (kotlin.math.abs(delta) <= deadband) return 0f
        // delta > 0 scrolls content up (target moves toward the top). Suppress it while the
        // target is already fully visible — nothing is gained and the block above scrolls away.
        if (topAnchor && delta > 0f && offset >= 0f && offset + size <= containerSize) return 0f
        return delta
    }
}

/**
 * Provides a bring-into-view policy to every scrollable inside [content].
 *  • Default: edge-margin (keeps a comfort band at the edges; used for horizontal rows).
 *  • [centerDeadband] non-null: centre the focused item with that dead-band (used for the
 *    vertical column so the focused section sits mid-screen without jittering).
 *  • [topAnchor]: never scroll up past an already-visible target — see [CenterBringIntoView].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PivotScroll(
    margin: Float = 0.12f,
    centerDeadband: Float? = null,
    topAnchor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val spec = remember(margin, centerDeadband, topAnchor) {
        if (centerDeadband != null) CenterBringIntoView(centerDeadband, topAnchor) else EdgeMarginBringIntoView(margin)
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

/** One lockup in a shelf. */
data class ShelfItem(
    val key: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val accent: Color,
    val badge: String? = null,
    val progress: Float? = null,
    val dimmed: Boolean = false,
    val onClick: () -> Unit = {},
)

/**
 * A complete content shelf — header + a horizontally-scrolling row of lockups.
 * Focus scrolling is handled by [PivotScroll]: the focused poster glides toward the
 * viewport centre (slightly left-biased, Apple-TV style) so it never slams against an
 * edge. Reports row focus (kept for callers) and the focused artwork (dynamic backdrop).
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun AppleShelf(
    title: String,
    items: List<ShelfItem>,
    portrait: Boolean,
    onRowFocused: () -> Unit = {},
    onItemFocused: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val firstItem = remember { FocusRequester() }
    Column(
        modifier = modifier.onFocusChanged { if (it.hasFocus) onRowFocused() },
        verticalArrangement = Arrangement.spacedBy(Dim.HeaderGap),
    ) {
        AppleSectionHeader(title, Modifier.padding(start = Dim.Side))
        PivotScroll(margin = 0.07f) {
            LazyRow(
                // Entering the row always lands on the first item (or the last one used) —
                // never the geometry-nearest middle item. Kills the "skip one item" feel.
                modifier = Modifier.focusRestorer { firstItem },
                contentPadding = PaddingValues(horizontal = Dim.Side, vertical = Dim.FocusPad),
                horizontalArrangement = Arrangement.spacedBy(Dim.PosterGap),
            ) {
                itemsIndexed(items, key = { _, it -> it.key }) { index, it ->
                    ApplePoster(
                        title = it.title,
                        subtitle = it.subtitle,
                        imageUrl = it.imageUrl,
                        accent = it.accent,
                        badge = it.badge,
                        progress = it.progress,
                        dimmed = it.dimmed,
                        width = if (portrait) Dim.PortraitW else Dim.PosterW,
                        height = if (portrait) Dim.PortraitH else Dim.PosterH,
                        onClick = it.onClick,
                        modifier = Modifier
                            .then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier)
                            .onFocusChanged { f -> if (f.isFocused) onItemFocused(it.imageUrl) },
                    )
                }
            }
        }
    }
}

/** Consistent sub-page header: a back chevron, the screen name, and a trailing label —
 *  all on one line at the standard inset, so nothing ever hugs the screen edge. */
@Composable
fun AppleSubHeader(
    title: String,
    trailing: String,
    onBack: () -> Unit,
    backModifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppleBackButton(onClick = onBack, modifier = backModifier)
        Spacer(Modifier.width(28.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        Spacer(Modifier.weight(1f))
        Text(trailing, style = MaterialTheme.typography.titleLarge, color = TextSecondary)
    }
}

private fun Modifier.onFocusChangedTo(block: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { block(it.isFocused) }

/** tvOS overscan-safe content inset (research §5: 80pt sides, 60pt top/bottom). */
val ScreenInset = PaddingValues(horizontal = 80.dp, vertical = 60.dp)
