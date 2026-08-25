package com.example.minimusic.data

import java.nio.charset.Charset

private val Windows1252 = Charset.forName("windows-1252")

/**
 * Repairs only high-confidence UTF-8 text that was decoded as Windows-1252.
 * Combining marks and all other Unicode characters are preserved exactly.
 */
fun repairLikelyMojibake(text: String): String {
    if (!text.containsMojibakeSignal()) return text
    val repaired = runCatching {
        String(text.toByteArray(Windows1252), Charsets.UTF_8)
    }.getOrNull() ?: return text
    if (repaired.contains('\uFFFD')) return text
    return repaired
}

private fun String.containsMojibakeSignal(): Boolean =
    contains("â€™") || contains("â€œ") || contains("â€") ||
        contains("â€“") || contains("â€”") || contains("Ã©") ||
        contains("Ã±") || contains("Â©") || contains("Â·") ||
        contains("ðŸ")
