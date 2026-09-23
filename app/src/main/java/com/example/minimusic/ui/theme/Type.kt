package com.example.minimusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.minimusic.R

/**
 * MiniMusic's bundled Google Sans Flex family. The font files live in
 * res/font, so typography never needs a network request or Google Play
 * Services at runtime; this keeps the app genuinely offline.
 *
 * The bold file is also registered for ExtraBold so Compose has a real local
 * face for the requested 800-weight role instead of falling back to a device
 * font or attempting downloadable-font resolution.
 */
private val GoogleSansFlexFamily = FontFamily(
    Font(resId = R.font.google_sans_flex_regular, weight = FontWeight.Normal),
    Font(resId = R.font.google_sans_flex_medium, weight = FontWeight.Medium),
    Font(resId = R.font.google_sans_flex_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.google_sans_flex_bold, weight = FontWeight.Bold),
    Font(resId = R.font.google_sans_flex_bold, weight = FontWeight.ExtraBold)
)

/**
 * The Android half of M3's typesetting model.
 *
 * The spec splits typesetting by platform: web and iOS place text with
 * bounding boxes and padding, while Android — "or platform-agnostic specs" —
 * measures from the **baseline**, defining line height as the distance between
 * the baselines of consecutive lines and expressing vertical centering as an
 * *alignment* rather than a measured offset.
 *
 * Center alignment is exactly that: half-leading is split evenly above and
 * below the line box, so a line sits on its baseline inside a box the height
 * of its own line height instead of hanging off the top of it. Trim stays None
 * because the app's own padding owns all outer spacing.
 */
private val BaselineLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * One role of the M3 type scale: family, weight, size, line height, tracking.
 *
 * Line heights follow the spec's readability ratios — *"around 1.5 times the
 * type size"* for body and label, *"a line height ratio of 1.2"* for the
 * larger title, headline, and display styles — and tracking carries the
 * scale's per-role optical spacing, which this app's scale previously omitted
 * entirely (every role sat at zero tracking, so small text ran tight and large
 * text ran loose).
 */
private fun role(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Double
) = TextStyle(
    fontFamily = GoogleSansFlexFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = BaselineLineHeight
)

val MiniMusicTypography = Typography(
    displayLarge = role(FontWeight.Bold, 57, 64, -0.25),
    displayMedium = role(FontWeight.Bold, 45, 52, 0.0),
    displaySmall = role(FontWeight.Bold, 36, 44, 0.0),
    headlineLarge = role(FontWeight.Bold, 32, 40, 0.0),
    headlineMedium = role(FontWeight.Bold, 28, 36, 0.0),
    headlineSmall = role(FontWeight.Bold, 24, 30, 0.0),
    titleLarge = role(FontWeight.Medium, 20, 26, 0.0),
    titleMedium = role(FontWeight.SemiBold, 16, 22, 0.15),
    titleSmall = role(FontWeight.SemiBold, 14, 20, 0.1),
    bodyLarge = role(FontWeight.Normal, 16, 24, 0.5),
    bodyMedium = role(FontWeight.Normal, 14, 20, 0.25),
    bodySmall = role(FontWeight.Normal, 12, 16, 0.4),
    labelLarge = role(FontWeight.Medium, 14, 20, 0.1),
    labelMedium = role(FontWeight.Medium, 12, 16, 0.5),
    labelSmall = role(FontWeight.Medium, 11, 16, 0.5)
)

/**
 * Typography helpers that are about *applying* the scale rather than
 * redefining it.
 */
object MiniMusicType {
    /**
     * Tabular figures for any text whose digits change or align in a column.
     *
     * The spec asks for this by name: *"Use tabular figures (also known as
     * monospaced numbers) rather than proportional digits in tables or places
     * where values may change often, such as clocks"*, illustrated with both a
     * clock and **a music player's timecode**. Proportional digits change
     * width as they tick, so a running playhead drags its own layout around
     * and a value column never lines up.
     *
     * `tnum` is a font feature, not a different family: digits keep the
     * app's Google Sans Flex shapes and only their advance widths are
     * equalised. A font without the feature simply ignores it.
     */
    fun tabular(style: TextStyle): TextStyle =
        style.copy(fontFeatureSettings = "tnum")

    /**
     * Compact single-line label for fixed-width controls (the library's
     * Songs / Artists / Albums pill). Same family as the rest of the app —
     * earlier this control overrode `fontFamily` to `SansSerif`, so the pill
     * alone rendered in the system font — with a line height that keeps the
     * label's ratio near 1.5 at its smaller size.
     */
    val compactLabel: TextStyle = role(FontWeight.Medium, 13, 19, 0.1)
}
