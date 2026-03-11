package dev.livin.instaloader.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class InstaPost(
    val shortcode: String,
    val caption: String,
    val images: List<String>,
    val video: String? = null
)

@Serializable
data class InstaOEmbedResponse(
    @SerialName("title") val title: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    @SerialName("thumbnail_width") val thumbnailWidth: Int = 0,
    @SerialName("thumbnail_height") val thumbnailHeight: Int = 0,
    @SerialName("html") val html: String = ""
)


@Serializable
data class InstaGrapQlResponse(
    @SerialName("data") val data: InstaGrapQlData? = null,
)

@Serializable
data class InstaGrapQlData(
    @SerialName("xdt_shortcode_media") val media: InstaMedia? = null
)

@Serializable
data class InstaMedia(
    @SerialName("shortcode") val shortcode: String = "",
    @SerialName("display_url") val displayUrl: String = "",
    @SerialName("edge_media_to_caption") val captionEdge: InstaCaptionEdge? = null,
    @SerialName("edge_sidecar_to_children") val sidecar: InstaSideCar? = null,
    @SerialName("video_url") val video: String? = null

)


@Serializable
data class InstaCaptionEdge(
    @SerialName("edges") val edges: List<InstaCaptionNode> = emptyList()

)

@Serializable
data class InstaCaptionNode(
    @SerialName("node") val node: InstaCaptionText? = null
)

@Serializable
data class InstaCaptionText(
    @SerialName("text") val text: String = ""
)

@Serializable
data class InstaSideCar(
    @SerialName("edges") val edges: List<InstaSidecarNode> = emptyList()

)

@Serializable
data class InstaSidecarNode(
    @SerialName("node") val node: InstaSidecarMedia? = null
)


@Serializable
data class InstaSidecarMedia(
    @SerialName("display_url") val displayUrl: String = ""
)