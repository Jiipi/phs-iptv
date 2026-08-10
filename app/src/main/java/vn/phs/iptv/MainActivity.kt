package vn.phs.iptv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import vn.phs.iptv.domain.AppScreen
import vn.phs.iptv.domain.AppStateMachine
import vn.phs.iptv.ui.navigation.AppNavigation
import vn.phs.iptv.ui.theme.PhsAppTheme

// Kiosk host — single Activity, Compose-only.
// LEANBACK_LAUNCHER: this Activity IS the Android TV home for this device.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val stateMachine: AppStateMachine by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Kiosk rule: BACK never leaves the app. Screens that have somewhere to go install
        // their own BackHandler and consume the event before it reaches here; anything that
        // reaches this callback would otherwise finish() the Activity, and since this app is
        // the device's launcher there is nothing behind it — the guest lands on a black
        // screen or the bare system UI with no way back in. Swallowing it is the whole point.
        onBackPressedDispatcher.addCallback(this) { /* consume */ }

        setContent {
            val themeMode by stateMachine.themeMode.collectAsStateWithLifecycle()
            PhsAppTheme(themeMode = themeMode) {
                AppNavigation(stateMachine = stateMachine)
            }
        }
    }

    // The remote's HOME button does not arrive as a key event: this Activity is registered as
    // CATEGORY_HOME with launchMode="singleTask", so the system delivers a fresh MAIN intent
    // to the already-running instance instead. Without this override the press did nothing at
    // all — the intent was received and dropped.
    //
    // It lands on the language picker, not the hub: HOME is the guest's "start over" key, and
    // starting over on a hotel TV means re-picking a language (the picker leads to Home from
    // there). Guarded on an active session so a press during provisioning or an empty room
    // cannot jump into a guest screen.
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME) &&
            stateMachine.currentGuest.value != null
        ) {
            stateMachine.navigateTo(AppScreen.Language)
        }
    }
}
