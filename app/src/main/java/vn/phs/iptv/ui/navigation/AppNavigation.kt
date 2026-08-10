@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import vn.phs.iptv.ui.i18n.LocalUiStrings
import vn.phs.iptv.ui.i18n.stringsFor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import vn.phs.iptv.domain.AppScreen
import vn.phs.iptv.domain.AppStateMachine
import vn.phs.iptv.domain.GuestProfile
import vn.phs.iptv.ui.assistant.VoiceAssistantScreen
import vn.phs.iptv.ui.folio.FolioScreen
import vn.phs.iptv.ui.help.HelpScreen
import vn.phs.iptv.ui.home.HomeScreen
import vn.phs.iptv.ui.idle.IdleScreen
import vn.phs.iptv.ui.language.LanguageScreen
import vn.phs.iptv.ui.livetv.LiveTvScreen
import vn.phs.iptv.ui.order.OrderScreen
import vn.phs.iptv.ui.services.ServicesScreen
import vn.phs.iptv.ui.provisioning.ProvisioningScreen
import vn.phs.iptv.ui.provisioning.ProvisioningViewModel
import vn.phs.iptv.ui.video.IntroVideoScreen

private object Routes {
    const val LOADING      = "loading"
    const val PROVISIONING = "provisioning"
    const val IDLE         = "idle"
    const val WELCOME      = "welcome"
    const val INTRO_VIDEO  = "intro_video"
    const val LANGUAGE     = "language"
    const val HOME         = "home"
    const val LIVE_TV      = "live_tv"
    const val FOLIO        = "folio"
    const val ORDER        = "order"
    const val VOICE        = "voice"
    const val HELP         = "help"
    const val SERVICES     = "services"
    const val HOTEL_INTRO  = "hotel_intro"
}

@Composable
fun AppNavigation(stateMachine: AppStateMachine) {
    val navController = rememberNavController()
    val currentScreen by stateMachine.screen.collectAsStateWithLifecycle()
    val language by stateMachine.language.collectAsStateWithLifecycle()
    val screenData by stateMachine.screenData.collectAsStateWithLifecycle()
    val contentData by stateMachine.contentData.collectAsStateWithLifecycle()

    LaunchedEffect(currentScreen) {
        val route = when (currentScreen) {
            AppScreen.Loading      -> return@LaunchedEffect
            AppScreen.Provisioning -> Routes.PROVISIONING
            AppScreen.Idle         -> Routes.IDLE
            AppScreen.Welcome      -> Routes.WELCOME
            AppScreen.IntroVideo   -> Routes.INTRO_VIDEO
            AppScreen.Language     -> Routes.LANGUAGE
            AppScreen.Home         -> Routes.HOME
            AppScreen.LiveTv       -> Routes.LIVE_TV
            AppScreen.Folio        -> Routes.FOLIO
            AppScreen.Order        -> Routes.ORDER
            AppScreen.Voice        -> Routes.VOICE
            AppScreen.Help         -> Routes.HELP
            AppScreen.Services     -> Routes.SERVICES
            AppScreen.HotelIntro   -> Routes.HOTEL_INTRO
        }
        navController.navigate(route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    CompositionLocalProvider(LocalUiStrings provides stringsFor(language)) {
    NavHost(navController = navController, startDestination = Routes.LOADING) {
        composable(Routes.LOADING) {}

        composable(Routes.PROVISIONING) {
            val vm: ProvisioningViewModel = hiltViewModel()
            ProvisioningScreen(viewModel = vm, onProvisioned = { stateMachine.onProvisioned() })
        }

        composable(Routes.IDLE) {
            IdleScreen(screenData = screenData, contentData = contentData, language = language)
        }

        composable(Routes.WELCOME) {
            // Combined welcome + language picker (streamlined onboarding). The hotel's own
            // film plays behind it from the first frame, so the guest is greeted by moving
            // footage while they pick a language rather than after.
            val guest by stateMachine.currentGuest.collectAsStateWithLifecycle()
            // A null guest here means the session was cleared while this route was showing
            // (check-out, or a Screen poll that came back unoccupied). Rendering nothing
            // would strand the guest on a blank screen with no focusable element, so fall
            // back to the picker without a name rather than to an empty composable.
            LanguageScreen(
                guest = guest,
                backdropVideoUrl = contentData?.video?.introUrl,
                onSelected = { stateMachine.onWelcomeLanguageSelected(it) },
            )
        }

        composable(Routes.INTRO_VIDEO) {
            val videoUrl by stateMachine.introVideoUrl.collectAsStateWithLifecycle()
            IntroVideoScreen(videoUrl = videoUrl, onEnded = { stateMachine.onIntroVideoEnded() })
        }

        composable(Routes.LANGUAGE) {
            // Reached from the hub's rail and from the remote's HOME key. BACK returns to the
            // hub rather than dead-ending, since the guest already has a language set.
            LanguageScreen(
                onSelected = { stateMachine.onLanguageSelected(it) },
                onBack = { stateMachine.navigateTo(AppScreen.Home) },
            )
        }

        composable(Routes.HOME) {
            WithGuest(stateMachine) { g ->
                HomeScreen(
                    guest = g,
                    screenData = screenData,
                    contentData = contentData,
                    onVoice = { stateMachine.navigateTo(AppScreen.Voice) },
                    onBill = { stateMachine.navigateTo(AppScreen.Folio) },
                    onService = { stateMachine.navigateTo(AppScreen.Order) },
                    onLiveTv = { stateMachine.navigateTo(AppScreen.LiveTv) },
                    onLanguage = { stateMachine.navigateTo(AppScreen.Language) },
                    onHelp = { stateMachine.navigateTo(AppScreen.Help) },
                    onServices = { stateMachine.navigateTo(AppScreen.Services) },
                    onHotelIntro = { stateMachine.navigateTo(AppScreen.HotelIntro) },
                    language = language,
                )
            }
        }

        composable(Routes.LIVE_TV) {
            LiveTvScreen(onBack = { stateMachine.navigateTo(AppScreen.Home) })
        }

        composable(Routes.FOLIO) {
            WithGuest(stateMachine) {
                FolioScreen(
                    guest = it,
                    screenData = screenData,
                    onBack = { stateMachine.navigateTo(AppScreen.Home) },
                )
            }
        }

        composable(Routes.ORDER) {
            WithGuest(stateMachine) {
                OrderScreen(
                    guest = it,
                    screenData = screenData,
                    onBack = { stateMachine.navigateTo(AppScreen.Home) },
                )
            }
        }

        composable(Routes.VOICE) {
            VoiceAssistantScreen(onBack = { stateMachine.navigateTo(AppScreen.Home) })
        }

        composable(Routes.HELP) {
            WithGuest(stateMachine) {
                HelpScreen(
                    guest = it,
                    screenData = screenData,
                    contentData = contentData,
                    language = language,
                    onBack = { stateMachine.navigateTo(AppScreen.Home) },
                )
            }
        }

        composable(Routes.SERVICES) {
            WithGuest(stateMachine) {
                ServicesScreen(
                    guest = it,
                    contentData = contentData,
                    language = language,
                    onBack = { stateMachine.navigateTo(AppScreen.Home) },
                )
            }
        }

        composable(Routes.HOTEL_INTRO) {
            IntroVideoScreen(
                videoUrl = contentData?.video?.introUrl?.takeIf { it.isNotBlank() },
                onEnded = { stateMachine.navigateTo(AppScreen.Home) },
            )
        }
    }
    }
}

/**
 * Renders [content] only while a guest session exists, and steers back to Idle when it does not.
 *
 * Every guest-facing route needs a `GuestProfile`, and the session can vanish underneath any of
 * them: a check-out lands on the 20 s Screen poll and calls `clearGuestSession()` regardless of
 * which screen is showing. The old `guest?.let { }` simply rendered nothing in that window — a
 * black screen with no focusable element, which on a kiosk with no back stack is unrecoverable
 * until the next state change happens to fire.
 *
 * `AppScreen.Idle` is the correct destination rather than Home, because Home would immediately
 * hit this same null guard and bounce right back.
 */
@Composable
private fun WithGuest(
    stateMachine: AppStateMachine,
    content: @Composable (GuestProfile) -> Unit,
) {
    val guest by stateMachine.currentGuest.collectAsStateWithLifecycle()
    val g = guest
    if (g != null) {
        content(g)
    } else {
        LaunchedEffect(Unit) { stateMachine.navigateTo(AppScreen.Idle) }
    }
}
