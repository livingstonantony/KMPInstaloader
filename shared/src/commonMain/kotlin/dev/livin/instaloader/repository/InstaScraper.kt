package dev.livin.instaloader.repository

import dev.livin.instaloader.model.InstaPost
import dev.livin.instaloader.network.createHttpClient
import dev.livin.instaloader.utils.getInstagramShortCode

import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText

import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parameters

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object InstaScraper {

    private const val DOC_ID = "27128499623469141"

    private const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/142.0.0.0 Safari/537.36"

    private val client = createHttpClient()

    private enum class PostType {
        SINGLE_IMAGE,
        SINGLE_VIDEO,
        ALBUM
    }

    // ---------------------------------------------------------
    // PUBLIC
    // ---------------------------------------------------------

    suspend fun downloadPost(
        shortcodeUrl: String
    ): InstaPost {

        val shortcode = shortcodeUrl
            .getInstagramShortCode()
            ?: error(
                "Invalid Instagram URL: $shortcodeUrl"
            )

        println("SHORT_CODE: $shortcode")

        // -----------------------------------------------------
        // Get CSRF
        // -----------------------------------------------------

        val csrf = fetchCsrfToken()

        println("CSRF: $csrf")

        require(csrf.isNotBlank()) {
            "Instagram csrftoken was not found"
        }

        // -----------------------------------------------------
        // Get metadata
        // -----------------------------------------------------

        val json = fetchMetadata(
            shortcode = shortcode,
            csrf = csrf
        )

        println("Meta data:\n$json")

        // -----------------------------------------------------
        // data
        // -----------------------------------------------------

        val data = json["data"]
            ?.jsonObject
            ?: run {

                val errorMessage = json["errors"]
                    ?.asJsonArrayOrNull()
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.contentOrNull

                error(
                    buildString {

                        append(
                            "Instagram returned no data."
                        )

                        if (!errorMessage.isNullOrBlank()) {
                            append(
                                " Error: $errorMessage"
                            )
                        }

                        append(
                            "\nFull response: $json"
                        )
                    }
                )
            }

        // -----------------------------------------------------
        // web info
        // -----------------------------------------------------

        val webInfo = data[
            "xdt_api__v1__media__shortcode__web_info"
        ]
            ?.jsonObject
            ?: error(
                "Instagram response does not contain " +
                        "'xdt_api__v1__media__shortcode__web_info'"
            )

        // -----------------------------------------------------
        // item
        // -----------------------------------------------------

        val item = webInfo["items"]
            ?.asJsonArrayOrNull()
            ?.firstOrNull()
            ?.jsonObject
            ?: error(
                "Instagram returned an empty media items array"
            )

        // -----------------------------------------------------
        // Detect post type
        // -----------------------------------------------------

        val postType = getPostType(item)

        println("POST TYPE: $postType")

        // -----------------------------------------------------
        // Extract media
        // -----------------------------------------------------

        val imageUrls: List<String>
        val videoUrl: String?

        when (postType) {

            PostType.SINGLE_IMAGE -> {

                println(
                    "Single image post"
                )

                imageUrls = extractImageUrls(item)

                videoUrl = null
            }

            PostType.SINGLE_VIDEO -> {

                println(
                    "Single video / Reel"
                )

                imageUrls = emptyList()

                videoUrl = extractVideoUrl(item)
            }

            PostType.ALBUM -> {

                println(
                    "Album / Carousel"
                )

                imageUrls = extractImageUrls(item)

                videoUrl = extractVideoUrl(item)
            }
        }

        // -----------------------------------------------------
        // Debug output
        // -----------------------------------------------------

        println()
        println("URL =====")
        println()

        if (imageUrls.isEmpty()) {

            println(
                "No image URLs found"
            )

        } else {

            println(
                "Image URLs:"
            )

            imageUrls.forEachIndexed { index, url ->

                println(
                    "${index + 1}. $url"
                )
            }
        }

        if (videoUrl.isNullOrEmpty()) {

            println(
                "No video URLs found"
            )

        } else {

            println(
                "Video URL: $videoUrl"
            )
        }

        // -----------------------------------------------------
        // Return result
        // -----------------------------------------------------

        return InstaPost(
            shortcode = shortcode,
            caption = "",
            images = imageUrls.filter {
                it.isNotEmpty()
            },
            video = videoUrl.orEmpty()
        )
    }

    // ---------------------------------------------------------
    // POST TYPE
    // ---------------------------------------------------------

    private fun getPostType(
        item: JsonObject
    ): PostType {

        // Instagram may return:
        //
        // "carousel_media": null
        //
        // or:
        //
        // "carousel_media": [...]
        //
        // We MUST safely handle both.

        val carousel = item[
            "carousel_media"
        ]?.asJsonArrayOrNull()

        if (!carousel.isNullOrEmpty()) {

            return PostType.ALBUM
        }

        // Instagram may return:
        //
        // "video_versions": null
        //
        // or:
        //
        // "video_versions": [...]
        //
        // Again, handle both safely.

        val videoVersions = item[
            "video_versions"
        ]?.asJsonArrayOrNull()

        if (!videoVersions.isNullOrEmpty()) {

            return PostType.SINGLE_VIDEO
        }

        return PostType.SINGLE_IMAGE
    }

    // ---------------------------------------------------------
    // IMAGE URLS
    // ---------------------------------------------------------

    private fun extractImageUrls(
        item: JsonObject
    ): List<String> {

        // -----------------------------------------------------
        // Carousel
        // -----------------------------------------------------

        val carousel = item[
            "carousel_media"
        ]?.asJsonArrayOrNull()

        if (!carousel.isNullOrEmpty()) {

            return carousel
                .mapNotNull { media ->

                    bestCandidate(
                        media.jsonObject
                    )
                }
                .distinct()
        }

        // -----------------------------------------------------
        // Single image
        // -----------------------------------------------------

        return listOfNotNull(
            bestCandidate(item)
        ).distinct()
    }

    // ---------------------------------------------------------
    // VIDEO URL
    // ---------------------------------------------------------

    private fun extractVideoUrl(
        item: JsonObject
    ): String? {

        // -----------------------------------------------------
        // Single video / Reel
        // -----------------------------------------------------

        val videos = item[
            "video_versions"
        ]?.asJsonArrayOrNull()

        if (!videos.isNullOrEmpty()) {

            val bestVideo = getBestVideo(
                videos
            )

            if (!bestVideo.isNullOrEmpty()) {

                return bestVideo
            }
        }

        // -----------------------------------------------------
        // Carousel videos
        // -----------------------------------------------------

        val carousel = item[
            "carousel_media"
        ]?.asJsonArrayOrNull()

        if (!carousel.isNullOrEmpty()) {

            carousel.forEach { mediaElement ->

                val media = mediaElement
                    .jsonObject

                val mediaVideos = media[
                    "video_versions"
                ]?.asJsonArrayOrNull()

                if (!mediaVideos.isNullOrEmpty()) {

                    val bestVideo = getBestVideo(
                        mediaVideos
                    )

                    if (!bestVideo.isNullOrEmpty()) {

                        return bestVideo
                    }
                }
            }
        }

        return null
    }

    // ---------------------------------------------------------
    // BEST VIDEO
    // ---------------------------------------------------------

    private fun getBestVideo(
        videos: JsonArray
    ): String? {

        return videos
            .mapNotNull { video ->

                val obj = video.jsonObject

                val url = obj[
                    "url"
                ]
                    ?.jsonPrimitive
                    ?.contentOrNull

                val width = obj[
                    "width"
                ]
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: 0

                val height = obj[
                    "height"
                ]
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: 0

                if (url != null) {

                    Triple(
                        url,
                        width,
                        height
                    )

                } else {

                    null
                }
            }
            .maxByOrNull { (_, width, height) ->

                width * height

            }
            ?.first
    }

    // ---------------------------------------------------------
    // BEST IMAGE
    // ---------------------------------------------------------

    private fun bestCandidate(
        media: JsonObject
    ): String? {

        val imageVersions = media[
            "image_versions2"
        ]?.jsonObject
            ?: return null

        val candidates = imageVersions[
            "candidates"
        ]?.asJsonArrayOrNull()
            ?: return null

        return candidates
            .mapNotNull { candidate ->

                val obj = candidate.jsonObject

                val url = obj[
                    "url"
                ]
                    ?.jsonPrimitive
                    ?.contentOrNull

                val width = obj[
                    "width"
                ]
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: 0

                if (url != null) {

                    Pair(
                        url,
                        width
                    )

                } else {

                    null
                }
            }
            .maxByOrNull { (_, width) ->
                width
            }
            ?.first
    }

    // ---------------------------------------------------------
    // CSRF
    // ---------------------------------------------------------

    private suspend fun fetchCsrfToken(): String {

        val response = client.get(
            "https://www.instagram.com/"
        ) {

            header(
                HttpHeaders.UserAgent,
                USER_AGENT
            )

            header(
                HttpHeaders.Accept,
                "text/html,application/xhtml+xml"
            )
        }

        println(
            "Instagram homepage status: " +
                    response.status
        )

        val cookies = client.cookies(
            "https://www.instagram.com/"
        )

        return cookies
            .firstOrNull {
                it.name == "csrftoken"
            }
            ?.value
            .orEmpty()
    }

    // ---------------------------------------------------------
    // METADATA
    // ---------------------------------------------------------

    private suspend fun fetchMetadata(
        shortcode: String,
        csrf: String
    ): JsonObject {

        val variables = buildJsonObject {

            put(
                "shortcode",
                shortcode
            )

            put(
                "__relay_internal__pv__PolarisAIGMMediaWebLabelEnabledrelayprovider",
                false
            )

        }.toString()

        val response = client.submitForm(

            url =
                "https://www.instagram.com/graphql/query/",

            formParameters = parameters {

                append(
                    "variables",
                    variables
                )

                append(
                    "doc_id",
                    DOC_ID
                )

                append(
                    "server_timestamps",
                    "true"
                )
            }

        ) {

            header(
                HttpHeaders.UserAgent,
                USER_AGENT
            )

            header(
                HttpHeaders.Accept,
                "*/*"
            )

            header(
                "Referer",
                "https://www.instagram.com/"
            )

            header(
                "X-csrftoken",
                csrf
            )

            header(
                "X-Requested-With",
                "XMLHttpRequest"
            )
        }

        val text = response.bodyAsText()

        println(
            "GraphQL HTTP status: " +
                    response.status
        )

        println(
            "Instagram GraphQL response:\n$text"
        )

        require(
            response.status.isSuccess()
        ) {

            "Instagram HTTP error " +
                    "${response.status}: $text"
        }

        return try {

            Json.parseToJsonElement(
                text
            ).jsonObject

        } catch (e: Exception) {

            error(
                "Unable to parse Instagram response as JSON.\n" +
                        "Response: $text\n" +
                        "Error: ${e.message}"
            )
        }
    }

    // ---------------------------------------------------------
    // DOWNLOAD ONE FILE
    // ---------------------------------------------------------

    suspend fun downloadFile(
        url: String
    ): ByteArray {

        val response = client.get(url) {

            header(
                "User-Agent",
                USER_AGENT
            )
        }

        if (response.status.value in 200..299) {

            return response.body<ByteArray>()

        } else {

            throw Exception(
                "Failed to download file: " +
                        response.status
            )
        }
    }

    // ---------------------------------------------------------
    // DOWNLOAD MULTIPLE FILES
    // ---------------------------------------------------------

    suspend fun downloadFiles(
        urls: List<String>
    ): List<ByteArray?> = coroutineScope {

        urls.map { url ->

            async {

                try {

                    downloadFile(url)

                } catch (e: Exception) {

                    e.printStackTrace()

                    null
                }
            }

        }.awaitAll()
    }

    // ---------------------------------------------------------
    // CLOSE
    // ---------------------------------------------------------

    fun close() {

        client.close()
    }
}

// =============================================================
// JSON SAFE HELPERS
// =============================================================
//
// This is the important part.
//
// Instagram can return:
//
// "carousel_media": null
//
// "carousel_media": [...]
//
// "video_versions": null
//
// "video_versions": [...]
//
// Calling .jsonArray directly on JsonNull causes:
//
// JsonNull is not a JsonArray
//
// These helpers safely return null when the JSON value
// is not actually an array.
// =============================================================

private fun kotlinx.serialization.json.JsonElement
        .asJsonArrayOrNull(): JsonArray? {

    return this as? JsonArray
}