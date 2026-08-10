package vn.phs.iptv.ui.theme

import androidx.compose.runtime.compositionLocalOf

enum class AppThemeMode {
    DARK,
    LIGHT,
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.DARK }
