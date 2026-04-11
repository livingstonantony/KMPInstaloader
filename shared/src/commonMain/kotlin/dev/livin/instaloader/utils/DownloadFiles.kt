package dev.livin.instaloader.utils

// commonMain

expect fun saveImageToFile(
    bytes: ByteArray,
    fileName: String
): String

expect fun getCurrentDateTimeString(): String