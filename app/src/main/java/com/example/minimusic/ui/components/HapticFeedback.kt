package com.example.minimusic.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.compositionLocalOf

/** App-level haptic preference shared by every interactive surface. */
val LocalMiniMusicHaptics = compositionLocalOf { true }

fun View.performMiniMusicHaptic() {
    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}
