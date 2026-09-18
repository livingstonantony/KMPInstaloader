package dev.livin.instaloader.utils

import kotlin.math.roundToInt

/**
 * Extract shortcode from the Instagram URL.
 * @param this The Instagram URL.
 */
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

fun ByteArray.detectImageExtension(): String {
    val bytes = this
    return when {
        // JPEG: FF D8 FF
        bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte() -> {
            "jpg"
        }

        // PNG: 89 50 4E 47
        bytes.size >= 4 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte() -> {
            "png"
        }

        // WebP: RIFF....WEBP
        bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte() -> {
            "webp"
        }

        else -> "jpg"
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