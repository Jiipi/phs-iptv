package vn.phs.iptv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Application class — Hilt entry point.
// Firebase is NOT auto-initialized (FirebaseInitProvider removed in manifest)
// until google-services.json is present and the plugin is enabled.
@HiltAndroidApp
class PhsIptvApp : Application()
