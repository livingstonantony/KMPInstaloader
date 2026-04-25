package dev.livin.instaloader.utils

// commonMain

expect fun saveImageToFile(
    bytes: ByteArray,
    fileName: String
): String

expect fun saveVideoToFile(
    bytes: ByteArray,
    fileName: String
): String

expect fun getCurrentDateTimeString(): String


fun saveImagesToFiles(files: List<ByteArray?>): List<String> {
    val savedPaths = mutableListOf<String>()
    val baseName = getCurrentDateTimeString() // SAME format, single call

    files.forEachIndexed { index, bytes ->
        if (bytes != null) {
            val fileName = "${baseName}_${index + 1}"
            val path = saveImageToFile(bytes, fileName)
            savedPaths.add(path)
        }
    }

    return savedPaths
}
