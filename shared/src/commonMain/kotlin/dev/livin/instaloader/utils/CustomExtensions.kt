package dev.livin.instaloader.utils

import kotlin.math.roundToInt

fun String.getInstagramShortCode(): String? {

    // Regex to detect shortcode inside URL
    val regex = Regex("""instagram\.com/(?:p|reel|tv)/([^/?]+)""")
    val match = regex.find(this)

    return when {
        match != null -> match.groupValues[1]   // Extract from URL
        this.matches(Regex("^[A-Za-z0-9_-]{5,}$")) -> this // Already a shortcode
        else -> null
    }
}


 fun isValidInstagramUrl(url: String): Boolean {
    val cleanUrl = url.trim().lowercase()

    return cleanUrl.startsWith("https://www.instagram.com/") ||
            cleanUrl.startsWith("https://instagram.com/")
}

/**
 * Formats ByteArray size as readable text.
 *
 * Examples:
 * 120 Bytes
 * 25.43 KB
 * 3.78 MB
 *
 * Works in KMP (Android / iOS / Desktop)
 */
fun ByteArray.formatSize(): String {
    val bytes = this.size.toDouble()

    return when {
        bytes >= 1024 * 1024 -> {
            val mb = bytes / (1024.0 * 1024.0)
            "${((mb * 100).roundToInt() / 100.0)} MB"
        }

        bytes >= 1024 -> {
            val kb = bytes / 1024.0
            "${((kb * 100).roundToInt() / 100.0)} KB"
        }

        else -> "${bytes.toInt()} Bytes"
    }
}