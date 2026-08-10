package vn.phs.iptv.ui.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import vn.phs.iptv.domain.AppLanguage

/** A deliberately small allow-list of guest-facing TV apps. */
data class TvApp(
    val title: String,
    val packageName: String,
    val accent: Color,
    val categoryVi: String,
    val categoryEn: String,
    val categoryRu: String,
    val voiceAliases: Set<String>,
) {
    fun category(language: AppLanguage): String = when (language) {
        AppLanguage.VI -> categoryVi
        AppLanguage.EN -> categoryEn
        AppLanguage.RU -> categoryRu
    }
}

object TvAppPackages {
    const val YOUTUBE = "com.google.android.youtube.tv"
    const val TV360 = "com.viettel.tv360.tv"
    const val THVLI = "vn.thvli.androidtv"
    const val FPT_PLAY = "net.fptplay.ottbox"
    const val VIEON = "com.vieon.tv"
    const val NETFLIX = "com.netflix.mediaclient"
    const val SPOTIFY = "com.spotify.tv.android"
    const val ZING_MP3 = "vng.zing.mp3"
}

object TvAppCatalog {
    val recommended = listOf(
        TvApp("YouTube", TvAppPackages.YOUTUBE, Color(0xFFFF0033), "Video & âm nhạc", "Video & music", "Видео и музыка", setOf("youtube", "you tube")),
        TvApp("TV360", TvAppPackages.TV360, Color(0xFFE31837), "Truyền hình & phim", "TV & movies", "ТВ и фильмы", setOf("tv360", "tv 360", "truyen hinh")),
        TvApp("THVLi", TvAppPackages.THVLI, Color(0xFF007A4D), "Truyền hình Việt Nam", "Vietnamese TV", "Вьетнамское ТВ", setOf("thvli", "thvl")),
        TvApp("FPT Play", TvAppPackages.FPT_PLAY, Color(0xFFF36F21), "Truyền hình & phim", "TV & movies", "ТВ и фильмы", setOf("fpt play", "fpt")),
        TvApp("VieON", TvAppPackages.VIEON, Color(0xFF00AEEF), "Truyền hình & phim", "TV & movies", "ТВ и фильмы", setOf("vieon", "vie on")),
        TvApp("Netflix", TvAppPackages.NETFLIX, Color(0xFFE50914), "Phim & chương trình", "Movies & shows", "Фильмы и сериалы", setOf("netflix", "net flix")),
        TvApp("Spotify", TvAppPackages.SPOTIFY, Color(0xFF1DB954), "Âm nhạc", "Music", "Музыка", setOf("spotify")),
        TvApp("Zing MP3", TvAppPackages.ZING_MP3, Color(0xFF7B2CBF), "Âm nhạc Việt Nam", "Vietnamese music", "Вьетнамская музыка", setOf("zing mp3", "zing")),
    )

    /** Only known apps with a real launch activity are allowed onto the hotel home screen. */
    fun installed(context: Context): List<TvApp> = recommended.filter { app ->
        launchIntent(context, app.packageName) != null
    }

    fun launch(context: Context, packageName: String): Boolean {
        val intent = launchIntent(context, packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun matchingVoiceApp(normalizedCommand: String): TvApp? = recommended.firstOrNull { app ->
        app.voiceAliases.any { alias -> normalizedCommand.contains(alias) }
    }

    private fun launchIntent(context: Context, packageName: String): Intent? =
        context.packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: context.packageManager.getLaunchIntentForPackage(packageName)
}
