package dev.livin.instaloader.utils

// androidMain

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.net.Uri

lateinit var appContext: Context

actual fun saveImageToFile(
    bytes: ByteArray,
    fileName: String
): String {

    val resolver = appContext.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/InstaLoader")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val uri: Uri? = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    uri?.let {
        resolver.openOutputStream(it)?.use { stream ->
            stream.write(bytes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        return uri.toString()
    }

    throw Exception("Failed to save image")
}
// androidMain


actual fun getCurrentDateTimeString(): String {
    val sdf = SimpleDateFormat("yyyy_MM_dd HH_mm_ss", Locale.getDefault())
    return sdf.format(Date())
}