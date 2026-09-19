package dev.livin.instaloader.utils

// iosMain

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary

@OptIn(ExperimentalForeignApi::class)
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

    val extension = bytes.detectImageExtension()
    val path = "$dir/$fileName.$extension"

    // Save temporarily
    NSFileManager.defaultManager.createFileAtPath(
        path,
        data,
        null
    )

    // Save to Photos
    PHPhotoLibrary.sharedPhotoLibrary().performChanges({
        PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(
            NSURL.fileURLWithPath(path)
        )
    }, completionHandler = { success, error ->

        if (success) {
            println("Image saved to Photos: $fileName")
        } else {
            println(
                "Failed to save image: ${error?.localizedDescription}"
            )
        }
    })

    return path
}


@OptIn(ExperimentalForeignApi::class)
actual fun saveVideoToFile(
    bytes: ByteArray,
    fileName: String
): String {

    val data = bytes.toNSData()

    val dir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).first() as String

    val path = "$dir/$fileName.mp4"

    // Save temporarily
    NSFileManager.defaultManager.createFileAtPath(
        path,
        data,
        null
    )

    // Save to Photos
    PHPhotoLibrary.sharedPhotoLibrary().performChanges({
        PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(
            NSURL.fileURLWithPath(path)
        )
    }, completionHandler = { success, error ->

        if (success) {
            println("Video saved to Photos: $fileName")
        } else {
            println(
                "Failed to save video: ${error?.localizedDescription}"
            )
        }
    })

    return path
}
actual fun getCurrentDateTimeString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy_MM_dd HH_mm_ss"
    return formatter.stringFromDate(NSDate())
}