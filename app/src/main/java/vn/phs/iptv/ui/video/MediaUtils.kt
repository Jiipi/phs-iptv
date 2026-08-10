package vn.phs.iptv.ui.video

import android.content.Context
import vn.phs.iptv.BuildConfig
import vn.phs.iptv.R

/**
 * Resolves a video URL or resource path into a valid, playable URI string for Media3 ExoPlayer.
 *
 * Prevents:
 * 1. Deprecated `RawResourceDataSource.buildRawResourceUri` failure on Android TV
 * 2. Unresolved relative paths from PMS API
 * 3. Blank/null video fallbacks
 */
fun resolveVideoUri(context: Context, rawUrl: String?): String {
    val url = rawUrl?.trim()
    if (url.isNullOrBlank()) {
        return "android.resource://${context.packageName}/${R.raw.ambient}"
    }
    return when {
        url.startsWith("http://") || url.startsWith("https://") ||
        url.startsWith("android.resource://") || url.startsWith("file://") ||
        url.startsWith("content://") -> url

        url.startsWith("/") -> {
            val base = BuildConfig.API_BASE.trimEnd('/')
            "$base$url"
        }

        else -> {
            val base = BuildConfig.API_BASE.trimEnd('/')
            "$base/$url"
        }
    }
}
