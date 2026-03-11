package dev.livin.instaloader.utils


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