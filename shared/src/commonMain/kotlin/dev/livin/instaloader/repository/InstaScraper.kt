package dev.livin.instaloader.repository

import dev.livin.instaloader.model.InstaPost
import dev.livin.instaloader.network.createHttpClient
import dev.livin.instaloader.utils.getInstagramShortCode
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object InstaScraper {

    private const val DOC_ID = "27128499623469141"

    private val client = createHttpClient()

    private enum class PostType {
        SINGLE_IMAGE,
        SINGLE_VIDEO,
        ALBUM
    }

    // ---------------------------------------------------------
    // PUBLIC
    // ---------------------------------------------------------

    suspend fun fetchPostData(
        shortcodeUrl: String
    ): InstaPost {

        val shortcode = shortcodeUrl
            .getInstagramShortCode()
            ?: error("Invalid Instagram URL: $shortcodeUrl")

        val csrf = fetchCsrfToken()

        require(csrf.isNotBlank()) {
            "Instagram csrftoken was not found"
        }

        val json = fetchMetadata(
            shortcode = shortcode,
            csrf = csrf
        )

        val item = extractPostItem(json)
        val postType = getPostType(item)

        var imageUrls: List<String> = emptyList()
        var videoUrl: String? = null

        when (postType) {
            PostType.SINGLE_IMAGE -> {
                imageUrls = listOf(extractBestImageCandidateUrl(item) ?: "")
            }

            PostType.SINGLE_VIDEO -> {
                videoUrl = extractVideoUrl(item)
            }

            PostType.ALBUM -> {
                imageUrls = extractImageUrls(item)

            }
        }

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
    // EXTRACT POST ITEM
    // ---------------------------------------------------------

    private fun extractPostItem(
        json: JsonObject
    ): JsonObject {

        val data = json["data"]?.jsonObject ?: throwMetadataError(json)

        val webInfo = data["xdt_api__v1__media__shortcode__web_info"]
            ?.jsonObject
            ?: error(
                "Instagram response does not contain " +
                        "'xdt_api__v1__media__shortcode__web_info'"
            )

        return webInfo["items"]
            ?.asJsonArrayOrNull()
            ?.firstOrNull()
            ?.asJsonObjectOrNull()
            ?: error("Instagram returned an empty media items array")
    }

    // ---------------------------------------------------------
    // METADATA ERROR
    // ---------------------------------------------------------

    private fun throwMetadataError(
        json: JsonObject
    ): Nothing {

        val errorMessage = json["errors"]
            ?.asJsonArrayOrNull()
            ?.firstOrNull()
            ?.asJsonObjectOrNull()
            ?.get("message")
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull

        error(
            buildString {
                append("Instagram returned no data.")

                if (!errorMessage.isNullOrBlank()) {
                    append(" Error: $errorMessage")
                }

                append("\nFull response: $json")
            }
        )
    }

    // ---------------------------------------------------------
    // POST TYPE
    // ---------------------------------------------------------

    private fun getPostType(item: JsonObject): PostType {
        return when (item["media_type"]?.jsonPrimitive?.intOrNull) {
            1 -> PostType.SINGLE_IMAGE
            2 -> PostType.SINGLE_VIDEO
            8 -> PostType.ALBUM
            else -> PostType.SINGLE_IMAGE
        }
    }


    // ---------------------------------------------------------
    // IMAGE URLS
    // ---------------------------------------------------------

    private fun extractImageUrls(
        item: JsonObject
    ): List<String> {

        val carousel = item["carousel_media"]
            ?.asJsonArrayOrNull()

        if (!carousel.isNullOrEmpty()) {

            return carousel
                .mapNotNull { media ->
                    media.asJsonObjectOrNull()?.let(::bestCandidate)
                }
                .distinct()
        }

        return listOfNotNull(
            bestCandidate(item)
        ).distinct()
    }

    private fun extractBestImageCandidateUrl(
        item: JsonObject
    ): String? {

        return item["image_versions2"]
            ?.jsonObject
            ?.get("candidates")
            ?.jsonArray
            ?.mapNotNull { candidate ->

                val obj = candidate.jsonObject

                val url = obj["url"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: return@mapNotNull null

                val width = obj["width"]
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: 0

                val height = obj["height"]
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: 0

                Triple(url, width, height)
            }
            ?.maxByOrNull { (_, width, height) ->
                width * height
            }
            ?.first
    }
    // ---------------------------------------------------------
    // VIDEO URL
    // ---------------------------------------------------------

    private fun extractVideoUrl(
        item: JsonObject
    ): String? {

        val videos = item["video_versions"]
            ?.asJsonArrayOrNull()

        if (!videos.isNullOrEmpty()) {
            getBestVideo(videos)?.let {
                return it
            }
        }

        val carousel = item["carousel_media"]
            ?.asJsonArrayOrNull()

        if (!carousel.isNullOrEmpty()) {
            carousel.forEach { mediaElement ->

                val media = mediaElement
                    .asJsonObjectOrNull()
                    ?: return@forEach

                val mediaVideos = media["video_versions"]
                    ?.asJsonArrayOrNull()

                if (!mediaVideos.isNullOrEmpty()) {

                    getBestVideo(mediaVideos)?.let {
                        return it
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
                val obj = video.asJsonObjectOrNull() ?: return@mapNotNull null
                val url = obj["url"]?.asJsonPrimitiveOrNull()?.contentOrNull
                val width = obj["width"]?.asJsonPrimitiveOrNull()?.intOrNull ?: 0
                val height = obj["height"]?.asJsonPrimitiveOrNull()?.intOrNull ?: 0
                url?.let { Triple(it, width, height) }
            }
            .maxByOrNull { (_, width, height) -> width * height }?.first
    }

    // ---------------------------------------------------------
    // BEST IMAGE
    // ---------------------------------------------------------

    private fun bestCandidate(
        media: JsonObject
    ): String? {

        val candidates = media["image_versions2"]
            ?.asJsonObjectOrNull()
            ?.get("candidates")
            ?.asJsonArrayOrNull()
            ?: return null

        return candidates
            .mapNotNull { candidate ->
                val obj = candidate.asJsonObjectOrNull() ?: return@mapNotNull null
                val url = obj["url"]?.asJsonPrimitiveOrNull()?.contentOrNull
                val width = obj["width"]?.asJsonPrimitiveOrNull()?.intOrNull ?: 0
                url?.let { Pair(it, width) }
            }
            .maxByOrNull { (_, width) ->
                width
            }?.first
    }

    // ---------------------------------------------------------
    // CSRF TOKEN
    // ---------------------------------------------------------

    private suspend fun fetchCsrfToken(): String {

        client.get(
            "https://www.instagram.com/"
        )

        return client.cookies("https://www.instagram.com/")
            .firstOrNull { it.name == "csrftoken" }?.value.orEmpty()
    }

    // ---------------------------------------------------------
    // METADATA
    // ---------------------------------------------------------

    private suspend fun fetchMetadata(
        shortcode: String,
        csrf: String
    ): JsonObject {

        val variables = buildJsonObject {
            put("shortcode", shortcode)
            put("__relay_internal__pv__PolarisAIGMMediaWebLabelEnabledrelayprovider", false)
        }.toString()

        val response = client.submitForm(
            url = "https://www.instagram.com/graphql/query/",
            formParameters = parameters {
                append("variables", variables)
                append("doc_id", DOC_ID)
                append("server_timestamps", "true")
            }
        ) {

            header("Referer", "https://www.instagram.com/")
            header("X-csrftoken", csrf)
            header("X-Requested-With", "XMLHttpRequest")
        }

        val text = response.bodyAsText()

        require(response.status.isSuccess()) {
            "Instagram HTTP error ${response.status}"
        }

        return try {
            Json.parseToJsonElement(text)
                .jsonObject
        } catch (e: Exception) {
            error("Unable to parse Instagram response as JSON: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // DOWNLOAD ONE FILE
    // ---------------------------------------------------------

    suspend fun downloadFile(
        url: String
    ): ByteArray {

        val response = client.get(url)

        require(response.status.isSuccess()) {
            "Failed to download file: ${response.status}"
        }

        return response.body()
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
                    null
                }
            }
        }.awaitAll()
    }

    // ---------------------------------------------------------
    // JSON HELPERS
    // ---------------------------------------------------------

    private fun JsonElement.asJsonArrayOrNull(): JsonArray? =
        this as? JsonArray

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        this as? JsonObject

    private fun JsonElement.asJsonPrimitiveOrNull() =
        this as? kotlinx.serialization.json.JsonPrimitive
}