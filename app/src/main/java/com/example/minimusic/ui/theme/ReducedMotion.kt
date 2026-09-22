package com.example.minimusic.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * The first characteristic the M3 transitions guide asks of any transition:
 * it should *"follow accessibility settings"*.
 *
 * > *"Most platforms have a reduced animation setting to help users with a
 * > sensitivity to motion. If that setting is on, transitions should: use subtle
 * > fades instead of intense sliding or scaling animations, and disable
 * > decorative effects like parallax or shape morphing."*
 *
 * Compose honours the platform's animator duration scale for the animations it
 * owns — a scale of zero collapses a tween to an instant jump — but two kinds of
 * motion here escape that: transitions that are only *reduced* rather than
 * removed, and motion the app drives itself in the view system (the queue's
 * smooth-scroll glide). Those need to know the setting, so it is read once and
 * published here.
 *
 * Screens branch to a fade (or to an instant placement) when this is true; see
 * NavGraph, LibraryScreen, PlayerScreen, LyricsScreen and QueueDrawer.
 */
val LocalMiniMusicReducedMotion = staticCompositionLocalOf { false }

/**
 * Reads the platform's animator duration scale and keeps up with changes to it
 * while the app is running (the observer covers the user turning the setting on
 * from the notification shade's quick settings).
 *
 * A missing or unreadable setting is treated as "animations are on", which is
 * the platform default.
 */
@Composable
fun rememberMiniMusicReducedMotion(): Boolean {
    val context = LocalContext.current
    var scale by remember(context) { mutableStateOf(animatorDurationScale(context)) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scale = animatorDurationScale(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale <= 0f
}

private fun animatorDurationScale(context: Context): Float =
    runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)
