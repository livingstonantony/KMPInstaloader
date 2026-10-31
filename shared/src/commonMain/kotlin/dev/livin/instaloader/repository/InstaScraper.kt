package dev.livin.instaloader.repository

import dev.livin.instaloader.model.InstaOEmbedResponse
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

    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Extract media urls from the public Instagram post.
     * @param shortcodeUrl The URL of the Instagram post.
     * @return [InstaPost] containing the post details.
     */
    suspend fun fetchPostData(
        shortcodeUrl: String
    ): InstaPost {

        val shortcode = shortcodeUrl
            .getInstagramShortCode()
            ?: error("Invalid Instagram URL: $shortcodeUrl")

        println("SHORTCODE: $shortcode")

        // 1. First attempt: GraphQL query (if CSRF/session works)
        val csrf = runCatching { fetchCsrfToken() }.getOrDefault("")
        if (csrf.isNotBlank()) {
            val graphqlPost = runCatching {
                val json = fetchMetadata(shortcode = shortcode, csrf = csrf)
                val item = extractPostItem(json)
                parseGraphQLItem(shortcode = shortcode, item = item)
            }.getOrNull()

            if (graphqlPost != null && (graphqlPost.images.isNotEmpty() || !graphqlPost.video.isNullOrBlank())) {
                return graphqlPost
            }
        }

        // 2. Second attempt: OEmbed + Embed HTML Fallback (works for public posts without login)
        return fetchPostDataViaEmbed(shortcode)
    }

    private suspend fun fetchPostDataViaEmbed(shortcode: String): InstaPost {
        var caption = ""
        val images = mutableListOf<String>()
        var videoUrl: String? = null

        // Fetch OEmbed response
        val oembedUrl = "https://www.instagram.com/api/v1/oembed/?url=https://www.instagram.com/p/$shortcode/"
        runCatching {
            val response = client.get(oembedUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            }
            if (response.status.isSuccess()) {
                val oembed = jsonParser.decodeFromString<InstaOEmbedResponse>(response.bodyAsText())
                caption = oembed.title
                if (oembed.thumbnailUrl.isNotBlank()) {
                    images.add(oembed.thumbnailUrl)
                }
            }
        }

        // Fetch Embed HTML page for additional media / video URLs
        val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
        runCatching {
            val response = client.get(embedUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }
            if (response.status.isSuccess()) {
                val html = response.bodyAsText()
                val unescaped = html.replace("\\/", "/")
                    .replace("\\u0026", "&")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")

                // Extract video URL if present
                val videoRegex = Regex("""(?:video_url|src)\s*[:=]\s*["'](https://[^"'\s]+\.mp4[^"'\s]*)["']""")
                val foundVideo = videoRegex.find(unescaped)?.groupValues?.get(1)
                if (!foundVideo.isNullOrBlank()) {
                    videoUrl = foundVideo
                }

                // Extract display URLs / images
                val displayUrlRegex = Regex(""""display_url"\s*:\s*"([^"]+)"""")
                displayUrlRegex.findAll(unescaped).forEach { match ->
                    val url = match.groupValues[1]
                    if (url.isNotBlank() && !images.contains(url)) {
                        images.add(url)
                    }
                }
            }
        }

        require(images.isNotEmpty() || videoUrl != null) {
            "Unable to extract media from Instagram post: $shortcode"
        }

        return InstaPost(
            shortcode = shortcode,
            caption = caption,
            images = images.distinct(),
            video = videoUrl
        )
    }

    private fun parseGraphQLItem(shortcode: String, item: JsonObject): InstaPost {
        val postType = getPostType(item)
        var imageUrls: List<String> = emptyList()
        var videoUrl: String? = null

        when (postType) {
            PostType.SINGLE_IMAGE -> {
                imageUrls = listOfNotNull(bestCandidate(item))
            }
            PostType.SINGLE_VIDEO -> {
                imageUrls = listOfNotNull(extractReelThumbnail(item))
                videoUrl = extractVideoUrl(item)
            }
            PostType.ALBUM -> {
                imageUrls = extractImageUrls(item)
            }
        }

        return InstaPost(
            shortcode = shortcode,
            caption = "",
            images = imageUrls.filter { it.isNotEmpty() },
            video = videoUrl
        )
    }

    private enum class PostType {
        SINGLE_IMAGE,
        SINGLE_VIDEO,
        ALBUM
    }

    /**
     * Extract the post item from the JSON response.
     * @param json The JSON response.
     */
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

        return webInfo["items"]?.asJsonArrayOrNull()?.firstOrNull()?.asJsonObjectOrNull()
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


    /**
     * Extract the post type from the JSON response.
     * @param item The post meta data.
     * @return [PostType] of the post.
     */
    private fun getPostType(item: JsonObject): PostType {
        return when (item["media_type"]?.jsonPrimitive?.intOrNull) {
            1 -> PostType.SINGLE_IMAGE
            2 -> PostType.SINGLE_VIDEO
            8 -> PostType.ALBUM
            else -> PostType.SINGLE_IMAGE
        }
    }

    /**
     * Extract best quality images url from the meta data json
     * @param item The post meta data.
     * @return List of image urls.
     */
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


    private fun extractReelThumbnail(
        item: JsonObject
    ): String? {

        val candidates = item["image_versions2"]
            ?.jsonObject
            ?.get("candidates")
            ?.jsonArray
            ?: return null

        return candidates
            .mapNotNull { element ->
                element as? JsonObject
            }
            .maxByOrNull { candidate ->

                val width = candidate["width"]
                    ?.jsonPrimitive
                    ?.intOrNull ?: 0

                val height = candidate["height"]
                    ?.jsonPrimitive
                    ?.intOrNull ?: 0

                width * height
            }
            ?.get("url")
            ?.jsonPrimitive
            ?.contentOrNull
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

        val candidates =
            media["image_versions2"]?.asJsonObjectOrNull()?.get("candidates")?.asJsonArrayOrNull()
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

    fun close() {
        client.close()
    }
}