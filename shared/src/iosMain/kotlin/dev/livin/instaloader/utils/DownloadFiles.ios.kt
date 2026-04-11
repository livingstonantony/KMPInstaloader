package dev.livin.instaloader.utils

// iosMain

import platform.Foundation.*

import platform.Foundation.*

actual fun saveImageToFile(
    bytes: ByteArray,
    fileName: String
): String {
    val data = bytes.toNSData()

    val dir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).first() as String

    val path = "$dir/$fileName.jpg"
    NSFileManager.defaultManager.createFileAtPath(path, data, null)

    return path
}


actual fun getCurrentDateTimeString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy_MM_dd HH_mm_ss"
    return formatter.stringFromDate(NSDate())
}