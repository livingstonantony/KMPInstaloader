package dev.livin.instaloader.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstaMediaItem(
    val url: String,
    val isVideo: Boolean = false,
    val thumbnailUrl: String? = null
)

@Serializable
data class InstaPost(
    val shortcode: String,
    val caption: String,
    val images: List<String>,
    val video: String? = null,
    val mediaItems: List<InstaMediaItem> = emptyList()
)