@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.RoomService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import kotlinx.coroutines.delay
import vn.phs.iptv.BuildConfig
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.ScreenResponse
import vn.phs.iptv.domain.AppLanguage
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.common.toVnd
import vn.phs.iptv.ui.demo.Demo
import vn.phs.iptv.ui.demo.Img
import vn.phs.iptv.ui.demo.Media
import vn.phs.iptv.ui.content.asContentTile
import vn.phs.iptv.ui.content.firstMediaUrl
import vn.phs.iptv.ui.content.forLang
import vn.phs.iptv.ui.content.tileLabelKey
import vn.phs.iptv.ui.i18n.LocalUiStrings
import vn.phs.iptv.ui.theme.AppleDynamicBackdrop
import vn.phs.iptv.ui.theme.AppleGuestHeader
import vn.phs.iptv.ui.theme.AppleShelf
import vn.phs.iptv.ui.theme.Dim
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.PivotScroll
import vn.phs.iptv.ui.theme.ShelfItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The four destinations promoted out of the shelves and onto the arrival block. A guest with a
 * remote does not go looking for features, so anything they might need in the first minute has
 * to be one keypress away and labelled in words — see [HomeActionRow].
 */
private val ActionTileKeys = setOf("service", "bill", "services", "help")

@Composable
fun HomeScreen(
    guest: GuestProfile,
    screenData: ScreenResponse? = null,
    contentData: ContentResponse? = null,
    onVoice: () -> Unit,
    onBill: () -> Unit = {},
    onService: () -> Unit = {},
    onLiveTv: () -> Unit = {},
    onLanguage: () -> Unit = {},
    onHelp: () -> Unit = {},
    onServices: () -> Unit = {},
    onHotelIntro: () -> Unit = {},
    language: AppLanguage = AppLanguage.EN,
) {
    PhsAppTheme { HomeContent(guest, screenData, contentData, onVoice, onBill, onService, onLiveTv, onLanguage, onHelp, onServices, onHotelIntro, language) }
}

@Composable
private fun HomeContent(
    guest: GuestProfile,
    screenData: ScreenResponse?,
    contentData: ContentResponse?,
    onVoice: () -> Unit,
    onBill: () -> Unit,
    onService: () -> Unit,
    onLiveTv: () -> Unit,
    onLanguage: () -> Unit,
    onHelp: () -> Unit,
    onServices: () -> Unit,
    onHotelIntro: () -> Unit,
    language: AppLanguage,
) {
    val s = LocalUiStrings.current
    val firstTile = remember { FocusRequester() }
    val railFirst = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstTile.requestFocus() }

    val defaultBackdrop = contentData?.roomType?.media?.firstMediaUrl()
        ?: contentData?.hotel?.media?.firstMediaUrl()
        ?: Img.hotelSuite
    var backdrop by remember(defaultBackdrop) { mutableStateOf(defaultBackdrop) }
    val setBackdrop: (String?) -> Unit = { if (it != null) backdrop = it }

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(10_000); now = LocalTime.now() } }
    val clock = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val greeting = when (now.hour) {
        in 5..11 -> s.greetingMorning
        in 12..17 -> s.greetingAfternoon
        else -> s.greetingEvening
    }
    val hotelSubtitle = contentData?.hotel?.name
        ?.takeIf { it.isNotBlank() }
        ?.let { hotelName ->
            when (language) {
                AppLanguage.VI -> "Chào mừng trở lại $hotelName"
                AppLanguage.EN -> "Welcome back to $hotelName"
                AppLanguage.RU -> "С возвращением в $hotelName"
            }
        }
        ?: s.welcomeBack
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)) }
    // PMS often leaves the honorific empty, and "${title} ${name}" then renders a leading space.
    val guestName = remember(guest) {
        listOf(guest.title, guest.name).filter { it.isNotBlank() }.joinToString(" ")
    }

    // Shelf data (mapped to the shared ShelfItem model so every row lays out identically)
    val cont = Demo.continueWatching.toShelf(onLiveTv)
    val movies = Demo.movies.toShelf(onLiveTv)
    val series = Demo.series.toShelf(onLiveTv)
    val sports = Demo.sports.toShelf(onLiveTv)
    val relax = Demo.relax.toShelf(onLiveTv)
    val docu = Demo.documentary.toShelf(onLiveTv)
    val channels = Demo.channels.map {
        ShelfItem(it.number, "${it.number}  ${it.name}", it.nowPlaying, it.imageUrl, it.accent, badge = "LIVE", onClick = onLiveTv)
    }
    val fallbackServices = Demo.homeTiles.filter { BuildConfig.DEBUG || it.id == "bill" }.map { t ->
        val (title, subtitle) = s.tile(t.id, t.title, t.subtitle)
        t.id to ShelfItem(
            key = t.id, title = title, subtitle = subtitle,
            imageUrl = t.imageUrl, accent = t.accent, dimmed = t.comingSoon,
            onClick = {
                when (t.id) {
                    "tv" -> onLiveTv(); "bill" -> onBill(); "service" -> onService(); "voice" -> onVoice()
                    else -> {}
                }
            },
        )
    }
    val contentServices = contentData?.tiles.orEmpty().mapIndexedNotNull { index, element ->
        val tile = element.asContentTile(language) ?: return@mapIndexedNotNull null
        // Bare-key tiles carry no text/art: borrow the localized label and artwork
        // already defined for the matching built-in tile. An unknown key has no
        // label to fall back on — drop it rather than print the raw code (§7).
        val labelKey = tile.key.tileLabelKey()
        val builtIn = Demo.homeTiles.firstOrNull { it.id == labelKey }
        val (fallbackTitle, fallbackSub) = s.tile(labelKey, "", "")
        val title = tile.title.ifBlank { fallbackTitle }
        if (title.isBlank()) return@mapIndexedNotNull null
        labelKey to ShelfItem(
            key = tile.key.ifBlank { "content_tile_$index" },
            title = title,
            subtitle = tile.subtitle.ifBlank { fallbackSub },
            imageUrl = tile.imageUrl
                .ifBlank { builtIn?.imageUrl.orEmpty() }
                .ifBlank { tileArtwork(labelKey) },
            accent = builtIn?.accent ?: Color(0xFF0A84FF),
            onClick = {
                when (tile.target) {
                    "tv", "live_tv", "livetv" -> onLiveTv()
                    "bill", "folio" -> onBill()
                    "order", "room_service", "roomservice" -> onService()
                    "services", "hotel_services", "facilities" -> onServices()
                    "help", "hotel", "info", "hotel_info" -> onHelp()
                    "voice", "assistant" -> onVoice()
                    "room", "your_room" -> onHelp()
                }
            },
        )
    }
    // The shelf keeps only what the action row does NOT already show, so a branch never sees
    // the same destination twice. With the usual PMS payload (`["bill","services"]`) that
    // empties the shelf entirely and it disappears — no half-empty row at the bottom.
    val leftoverTiles = contentServices.ifEmpty { fallbackServices }
        .filterNot { (labelKey, _) -> labelKey in ActionTileKeys }
        .map { (_, item) -> item }

    // Primary navigation rail destinations (localized). "home" returns focus to content.
    val railItems = buildList {
        add(NavRailItem("home", Icons.Rounded.Home, s.navHome) { firstTile.requestFocus() })
        add(NavRailItem("livetv", Icons.Rounded.LiveTv, s.navLiveTv, onLiveTv))
        add(NavRailItem("menu", Icons.Rounded.RestaurantMenu, s.navMenu, onService))
        add(NavRailItem("services", Icons.Rounded.RoomService, s.navServices, onServices))
        add(NavRailItem("bill", Icons.Rounded.ReceiptLong, s.navBill, onBill))
        add(NavRailItem("help", Icons.Rounded.Info, s.navHelp, onHelp))
        if (BuildConfig.DEBUG) {
            add(NavRailItem("assistant", Icons.Rounded.Mic, s.navAssistant, onVoice))
        }
        add(NavRailItem("language", Icons.Rounded.Language, s.navLanguage, onLanguage))
    }

    val actions = homeActions(screenData, contentData, language, onService, onBill, onServices, onHelp)

    // BACK on Home returns to the language selection screen so the guest can change language at any time.
    BackHandler { onLanguage() }

    Box(Modifier.fillMaxSize()) {
        AppleDynamicBackdrop(backdrop)

        // topAnchor: the bring-into-view request comes from the focused *tile*, not from the
        // block it lives in, so plain centring would hoist a 110dp action tile to mid-screen
        // and drag the guest's name and check-out dates off the top with it.
        PivotScroll(centerDeadband = 0.06f, topAnchor = true) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 320.dp),
            verticalArrangement = Arrangement.spacedBy(Dim.ShelfGap),
        ) {
            // ONE item on purpose. The column centres whatever asks to be brought into view, so
            // header / stay card / action row as three separate items would each drag the column
            // ~180dp and push the guest's name off screen the moment focus moved. Fused into a
            // single block (with topAnchor above) they all stay put, and everything the guest
            // needs is on screen at power-on without a keypress.
            item(key = "arrival") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = Dim.Side)
                        .onFocusChanged { if (it.hasFocus) backdrop = defaultBackdrop },
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    AppleGuestHeader(
                        eyebrow = greeting,
                        name = guestName,
                        subtitle = hotelSubtitle,
                        roomNo = guest.roomNo,
                        clock = clock,
                        date = date,
                        language = language,
                        onLanguage = onLanguage,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        AppleVideoBillboard(
                            eyebrow = contentData?.roomType?.roomType?.takeIf { it.isNotBlank() } ?: s.heroEyebrow,
                            title = "$greeting, ${s.roomWord} ${guest.roomNo}",
                            body = contentData?.texts?.welcome?.forLang(language)?.takeIf { it.isNotBlank() }
                                ?: screenData?.hotel?.welcomeNote?.takeIf { it.isNotBlank() }
                                ?: "",
                            playLabel = s.watchIntro,
                            videoUrl = contentData?.video?.introUrl?.takeIf { it.isNotBlank() },
                            imageUrl = defaultBackdrop,
                            onClick = onHotelIntro,
                            modifier = if (screenData?.stay != null) {
                                Modifier.width(Dim.HeroW).height(Dim.ArrivalH)
                            } else {
                                Modifier.weight(1f).height(Dim.ArrivalH)
                            },
                        )
                        screenData?.let { data ->
                            GuestStayOverview(
                                screen = data,
                                content = contentData,
                                language = language,
                                onOrder = onService,
                                modifier = Modifier.weight(1f).height(Dim.ArrivalH),
                            )
                        }
                    }

                    HomeActionRow(
                        actions = actions,
                        firstFocus = firstTile,
                        railFocus = railFirst,
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                shelf(s.continueWatching, 2, cont, portrait = false, setBackdrop)
                shelf(s.movies, 3, movies, portrait = true, setBackdrop)
                shelf(s.series, 4, series, portrait = true, setBackdrop)
                shelf(s.sports, 5, sports, portrait = false, setBackdrop)
                shelf(s.relax, 6, relax, portrait = false, setBackdrop)
                shelf(s.documentary, 7, docu, portrait = false, setBackdrop)
                shelf(s.liveTv, 8, channels, portrait = false, setBackdrop)
            }
            if (leftoverTiles.isNotEmpty()) {
                shelf(s.yourStay, 9, leftoverTiles, portrait = false, setBackdrop)
            }
        }
        }

        // Left quick-access rail — overlays the content (drawn last) so the column never
        // reflows as the rail expands. It sits inside the existing Dim.Side left gutter.
        HomeNavRail(
            items = railItems,
            selectedId = "home",
            railFocusRequester = railFirst,
            contentFocusRequester = firstTile,
            onRailFocusChanged = {},
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

/**
 * The action row's contents. Each tile carries its live detail as a subtitle so the guest can
 * read the value — folio total, next service — without opening anything.
 *
 * "Bill" and "Info & Wi-Fi" are unconditional: a missing folio means the PMS charge engine
 * hiccuped, not that the feature is gone (INTEGRATION.md §10), and [vn.phs.iptv.ui.folio]
 * already renders a null folio safely. "Order" and "Services" disappear when the branch has
 * nothing behind them — a tile that opens an empty screen is worse than no tile.
 */
@Composable
private fun homeActions(
    screenData: ScreenResponse?,
    contentData: ContentResponse?,
    language: AppLanguage,
    onService: () -> Unit,
    onBill: () -> Unit,
    onServices: () -> Unit,
    onHelp: () -> Unit,
): List<HomeAction> {
    val s = LocalUiStrings.current
    val hasQr = !screenData?.qrUrl.isNullOrBlank()
    val firstService = contentData?.services.orEmpty()
        .firstNotNullOfOrNull { it.title.forLang(language).takeIf { title -> title.isNotBlank() } }
    return buildList {
        if (hasQr) {
            val (title, _) = s.tile("service", s.orderTitle, s.scanToOrder)
            add(HomeAction("service", Icons.Rounded.RestaurantMenu, title, s.scanToOrder, onService))
        }
        val (billTitle, billSub) = s.tile("bill", s.billTitle, "")
        add(
            HomeAction(
                "bill", Icons.Rounded.ReceiptLong, billTitle,
                screenData?.folio?.total?.toVnd() ?: billSub.takeIf { it.isNotBlank() },
                onBill,
            ),
        )
        if (firstService != null) {
            val (title, _) = s.tile("services", s.servicesTitle, "")
            add(HomeAction("services", Icons.Rounded.RoomService, title, firstService, onServices))
        }
        val (helpTitle, helpSub) = s.tile("help", s.helpTitle, s.hotelInfo)
        add(HomeAction("help", Icons.Rounded.Info, helpTitle, helpSub.takeIf { it.isNotBlank() }, onHelp))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.shelf(
    title: String,
    rowIndex: Int,
    items: List<ShelfItem>,
    portrait: Boolean,
    onItemFocused: (String?) -> Unit,
) {
    item(key = "shelf_$rowIndex") {
        AppleShelf(
            title = title,
            items = items,
            portrait = portrait,
            onItemFocused = onItemFocused,
        )
    }
}

private fun List<Media>.toShelf(onClick: () -> Unit) = map {
    ShelfItem(it.id, it.title, it.subtitle, it.imageUrl, it.accent, it.badge, it.progress, onClick = onClick)
}

/** Artwork for PMS tile keys that have no matching entry in `Demo.homeTiles`. */
private fun tileArtwork(labelKey: String): String = when (labelKey) {
    "services" -> Img.spa
    "help" -> Img.hotelSuite
    "room" -> Img.hotelSuite
    else -> Img.hotelSuite
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun HomePreview() {
    HomeScreen(GuestProfile("Nguyen Van An", "Mr.", "302", "VNM", false), onVoice = {})
}
