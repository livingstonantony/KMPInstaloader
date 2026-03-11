package dev.livin.instaloader

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform