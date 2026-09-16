package com.example.minimusic.data

/**
 * The M3 dynamic color scheme derived from the album-art seed. Mirrors
 * PixelPlayer's album-art palette styles: the scheme decides how far the
 * secondary/tertiary hues drift from the source color (complementary
 * palettes per Material 3's dynamic color guidance), while the UI keeps
 * constant role mappings across all schemes.
 */
enum class PaletteStyle(val label: String) {
    TONAL_SPOT("Tonal Spot"),
    VIBRANT("Vibrant"),
    EXPRESSIVE("Expressive"),
    FRUIT_SALAD("Fruit Salad");

    companion object {
        fun fromString(value: String?): PaletteStyle =
            entries.firstOrNull { it.name == value } ?: VIBRANT
    }
}
