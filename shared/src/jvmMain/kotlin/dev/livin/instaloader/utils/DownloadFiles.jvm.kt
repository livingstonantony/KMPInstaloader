package dev.livin.instaloader.utils

// desktopMain

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getCurrentDateTimeString(): String {
    val sdf = SimpleDateFormat("yyyy_MM_dd HH_mm_ss", Locale.getDefault())
    return sdf.format(Date())
}

/**
 * Returns:
 * Pictures/InstaLoader
 *
 * Works on macOS / Linux / Windows
 */
private fun getPicturesDirectory(): File {
    val home = System.getProperty("user.home")
    val picturesDir = File(home, "Pictures/InstaLoader")

    if (!picturesDir.exists()) {
        picturesDir.mkdirs()
    }

    return picturesDir
}

/**
 * Saves image bytes into:
 * Pictures/InstaLoader
 *
 * Returns absolute file path
 */
actual fun saveImageToFile(
    bytes: ByteArray,
    fileName: String
): String {
    val file = File(
        getPicturesDirectory(),
        "$fileName.jpg"
    )

    file.writeBytes(bytes)
    return file.absolutePath
}

/**
 * Saves video bytes into:
 * Pictures/InstaLoader
 *
 * Returns absolute file path
 */
actual fun saveVideoToFile(
    bytes: ByteArray,
    fileName: String
): String {
    val file = File(
        getPicturesDirectory(),
        "$fileName.mp4"
    )

    file.writeBytes(bytes)
    return file.absolutePath
}