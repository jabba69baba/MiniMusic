package com.example.minimusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.minimusic.R

/**
 * MiniMusic's brand type family: Google Sans Flex, fetched at runtime through the
 * Google Fonts downloadable-fonts provider (Android's built-in mechanism for
 * fetching a specific font from Google Play services on first use, then caching
 * it — no font binary is bundled in this repo, and this isn't a network call this
 * app makes itself). Google released the font on Google Fonts under the SIL Open
 * Font License in November 2025, so it can be requested by name like any other
 * Google Font.
 *
 * Requires `res/values/font_certs.xml` (already included) to authorize the
 * provider — see that file's comment for what it is.
 *
 * On devices without Play services (or if the fetch fails for any reason), this
 * transparently falls back to the platform default sans — which on Pixel devices
 * already ships a Google Sans variant.
 */
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val googleSansFlex = GoogleFont(name = "Google Sans Flex")

private val GoogleSansFamily: FontFamily = FontFamily(
    Font(googleFont = googleSansFlex, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = googleSansFlex, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = googleSansFlex, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold)
)

val MiniMusicTypography = Typography(
    headlineSmall = TextStyle(fontFamily = GoogleSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = GoogleSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = GoogleSansFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)
