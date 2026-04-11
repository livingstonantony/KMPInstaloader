package dev.livin.instaloader.utils

// iosMain/kotlin/your/package/NSDataUtils.kt

import kotlinx.cinterop.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData =
    memScoped {
        NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = size.toULong()
        )
    }