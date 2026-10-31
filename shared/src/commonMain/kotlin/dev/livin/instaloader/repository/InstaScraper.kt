
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
import kotlinx.coroutines.CancellationException
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
import kotlinx.serialization.json.put

object InstaScraper {

    private const val INSTAGRAM_BASE_URL =
        "https://www.instagram.com"

    // Existing GraphQL operation ID.
    // Kept as a secondary extraction strategy.
    private const val DOC_ID = "27128499623469141"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/142.0.0.0 Safari/537.36"

    private val client = createHttpClient()

    private enum class PostType {
        SINGLE_IMAGE,
        SINGLE_VIDEO,
        ALBUM
    }

    // ---------------------------------------------------------
    // FETCH POST DATA
    // ---------------------------------------------------------

    suspend fun fetchPostData(
        shortcodeUrl: String
    ): InstaPost {

        val shortcode = shortcodeUrl
            .getInstagramShortCode()
            ?: error("Invalid Instagram URL: $shortcodeUrl")

        // Primary: Extract metadata from Instagram HTML.
        val item = try {

            fetchPostItemFromPage(
                shortcode = shortcode,
                shortcodeUrl = shortcodeUrl
            )

        } catch (pageError: Exception) {

            if (pageError is CancellationException) {
                throw pageError
            }

            println(
                "Instagram HTML extraction failed: ${pageError.message}"
            )

            // Fallback: Existing GraphQL implementation.
            try {

                val csrf = fetchCsrfToken()

                require(csrf.isNotBlank()) {
                    "Instagram csrftoken was not found"
                }

                val json = fetchMetadata(
                    shortcode = shortcode,
                    csrf = csrf
                )

                extractPostItem(json)

            } catch (graphqlError: Exception) {

                if (graphqlError is CancellationException) {
                    throw graphqlError
                }

                throw IllegalStateException(
                    buildString {
                        appendLine("Unable to extract Instagram post.")
                        appendLine("Shortcode: $shortcode")
                        appendLine("HTML error: ${pageError.message}")
                        appendLine("GraphQL error: ${graphqlError.message}")
                    },
                    graphqlError
                )
            }
        }

        val postType = getPostType(item)

        println("KMP_MEDIA_TYPE: $postType")
        val imageUrls: List<String>
        val videoUrl: String?

        when (postType) {

            PostType.SINGLE_IMAGE -> {

                imageUrls = listOfNotNull(
                    bestCandidate(item)
                )

                videoUrl = null
            }

            PostType.SINGLE_VIDEO -> {

                imageUrls = listOfNotNull(
                    extractReelThumbnail(item)
                        ?: bestCandidate(item)
                )

                videoUrl = extractVideoUrl(item)
            }

            PostType.ALBUM -> {

                imageUrls = extractImageUrls(item)

                videoUrl = extractVideoUrl(item)
            }
        }

        return InstaPost(
            shortcode = shortcode,
            caption = "",
            images = imageUrls.distinct(),
            video = videoUrl.orEmpty()
        )
    }

    // ---------------------------------------------------------
    // HTML EXTRACTION
    // ---------------------------------------------------------

    private suspend fun fetchPostItemFromPage(
        shortcode: String,
        shortcodeUrl: String
    ): JsonObject {

        val pageType = when {
            shortcodeUrl.contains("/reel/") -> "reel"
            shortcodeUrl.contains("/tv/") -> "tv"
            else -> "p"
        }

        val pageUrl =
            "$INSTAGRAM_BASE_URL/$pageType/$shortcode/"

        val response = client.get(pageUrl) {

            header("Accept", "text/html")

            header("User-Agent", USER_AGENT)

            header(
                "Accept-Language",
                "en-US,en;q=0.9"
            )

            header("Referer", "$INSTAGRAM_BASE_URL/")

            header("Sec-Fetch-Dest", "document")
            header("Sec-Fetch-Mode", "navigate")
            header("Sec-Fetch-Site", "none")
        }

        val html = response.bodyAsText()

        require(response.status.isSuccess()) {
            "Instagram page HTTP error: ${response.status}"
        }

        require(html.isNotBlank()) {
            "Instagram returned an empty HTML page"
        }

        println("Instagram HTML status: ${response.status}")
        println("Instagram HTML length: ${html.length}")

        // Find JSON script tags.
        val scriptRegex = Regex(
            """<script\b([^>]*)>(.*?)</script\s*>""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        )

        val jsonScriptRegex = Regex(
            """\btype\s*=\s*["']application/json["']""",
            RegexOption.IGNORE_CASE
        )

        var jsonScriptCount = 0

        for (match in scriptRegex.findAll(html)) {

            val attributes = match.groupValues[1]

            if (!jsonScriptRegex.containsMatchIn(attributes)) {
                continue
            }

            jsonScriptCount++

            val content = match.groupValues[2]

            val json = try {

                Json.parseToJsonElement(content)

            } catch (_: Exception) {

                continue
            }

            // Search the complete JSON tree.
            val mediaItem = findMediaItem(
                element = json,
                shortcode = shortcode
            )

            if (mediaItem != null) {

                println(
                    "Instagram media metadata extracted from HTML"
                )

                return mediaItem
            }
        }

        println("Instagram JSON scripts found: $jsonScriptCount")

        error(
            "No embedded media metadata found for shortcode: $shortcode. " +
                    "Instagram may have returned a login page, challenge, " +
                    "or changed its HTML structure."
        )
    }

    // ---------------------------------------------------------
    // FIND MEDIA ITEM IN EMBEDDED JSON
    // ---------------------------------------------------------

    private fun findMediaItem(
        element: JsonElement,
        shortcode: String
    ): JsonObject? {

        when (element) {

            is JsonObject -> {

                // Current Instagram Polaris response.
                val webInfo = element[
                    "xdt_api__v1__media__shortcode__web_info"
                ]?.asJsonObjectOrNull()

                val items = webInfo
                    ?.get("items")
                    ?.asJsonArrayOrNull()

                if (!items.isNullOrEmpty()) {

                    val matchingItem = items
                        .mapNotNull { it.asJsonObjectOrNull() }
                        .firstOrNull {
                            getShortcode(it) == shortcode
                        }

                    if (matchingItem != null) {
                        return matchingItem
                    }

                    // Single-item response without shortcode.
                    if (items.size == 1) {
                        return items.first().asJsonObjectOrNull()
                    }
                }

                // Older GraphQL response.
                val legacyMedia = element[
                    "xdt_shortcode_media"
                ]?.asJsonObjectOrNull()

                if (legacyMedia != null) {
                    return legacyMedia
                }

                // Direct media object.
                val itemShortcode = getShortcode(element)

                if (
                    itemShortcode == shortcode &&
                    (
                            element.containsKey("image_versions2") ||
                                    element.containsKey("video_versions") ||
                                    element.containsKey("display_url") ||
                                    element.containsKey("carousel_media") ||
                                    element.containsKey("edge_sidecar_to_children")
                            )
                ) {
                    return element
                }

                // Recursively search nested objects.
                for (value in element.values) {

                    val result = findMediaItem(
                        element = value,
                        shortcode = shortcode
                    )

                    if (result != null) {
                        return result
                    }
                }
            }

            is JsonArray -> {

                for (value in element) {

                    val result = findMediaItem(
                        element = value,
                        shortcode = shortcode
                    )

                    if (result != null) {
                        return result
                    }
                }
            }

            else -> Unit
        }

        return null
    }

    private fun getShortcode(
        item: JsonObject
    ): String? {

        return item["code"]
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull
            ?: item["shortcode"]
                ?.asJsonPrimitiveOrNull()
                ?.contentOrNull
    }

    // ---------------------------------------------------------
    // EXTRACT POST ITEM FROM GRAPHQL RESPONSE
    // ---------------------------------------------------------

    private fun extractPostItem(
        json: JsonObject
    ): JsonObject {

        val data = json["data"]
            ?.asJsonObjectOrNull()
            ?: throwMetadataError(json)

        val webInfo = data[
            "xdt_api__v1__media__shortcode__web_info"
        ]?.asJsonObjectOrNull()
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

    private fun getPostType(
        item: JsonObject
    ): PostType {

        val mediaType = item["media_type"]
            ?.asJsonPrimitiveOrNull()
            ?.intOrNull

        val isVideo = item["is_video"]
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull == "true"

        return when {

            mediaType == 8 ||
                    item.containsKey("carousel_media") ||
                    item.containsKey("edge_sidecar_to_children") ->
                PostType.ALBUM

            mediaType == 2 ||
                    isVideo ||
                    item.containsKey("video_versions") ||
                    item.containsKey("video_url") ->
                PostType.SINGLE_VIDEO

            else -> PostType.SINGLE_IMAGE
        }
    }

    // ---------------------------------------------------------
    // IMAGE URLS
    // ---------------------------------------------------------

    private fun extractImageUrls(
        item: JsonObject
    ): List<String> {

        // Current Instagram carousel structure.
        val carousel = item["carousel_media"]
            ?.asJsonArrayOrNull()

        if (!carousel.isNullOrEmpty()) {

            return carousel
                .mapNotNull { media ->
                    media.asJsonObjectOrNull()?.let(::bestCandidate)
                }
                .distinct()
        }

        // Older GraphQL carousel structure.
        val edges = item["edge_sidecar_to_children"]
            ?.asJsonObjectOrNull()
            ?.get("edges")
            ?.asJsonArrayOrNull()

        if (!edges.isNullOrEmpty()) {

            return edges
                .mapNotNull { edge ->

                    val node = edge
                        .asJsonObjectOrNull()
                        ?.get("node")
                        ?.asJsonObjectOrNull()

                    node?.let(::bestCandidate)
                }
                .distinct()
        }

        return listOfNotNull(
            bestCandidate(item)
        ).distinct()
    }

    // ---------------------------------------------------------
    // REEL THUMBNAIL
    // ---------------------------------------------------------

    private fun extractReelThumbnail(
        item: JsonObject
    ): String? {

        val candidates = item["image_versions2"]
            ?.asJsonObjectOrNull()
            ?.get("candidates")
            ?.asJsonArrayOrNull()
            ?: return item["display_url"]
                ?.asJsonPrimitiveOrNull()
                ?.contentOrNull

        return candidates
            .mapNotNull { element ->

                val candidate = element.asJsonObjectOrNull()
                    ?: return@mapNotNull null

                val width = candidate["width"]
                    ?.asJsonPrimitiveOrNull()
                    ?.intOrNull ?: 0

                val height = candidate["height"]
                    ?.asJsonPrimitiveOrNull()
                    ?.intOrNull ?: 0

                val url = candidate["url"]
                    ?.asJsonPrimitiveOrNull()
                    ?.contentOrNull

                url?.let {
                    Triple(it, width, height)
                }
            }
            .maxByOrNull { (_, width, height) ->
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

        // Legacy GraphQL video field.
        item["video_url"]
            ?.asJsonPrimitiveOrNull()
            ?.contentOrNull
            ?.let {
                return it
            }

        // Current video versions.
        val videos = item["video_versions"]
            ?.asJsonArrayOrNull()

        if (!videos.isNullOrEmpty()) {

            getBestVideo(videos)?.let {
                return it
            }
        }

        // Current carousel structure.
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

        // Legacy carousel structure.
        val edges = item["edge_sidecar_to_children"]
            ?.asJsonObjectOrNull()
            ?.get("edges")
            ?.asJsonArrayOrNull()

        if (!edges.isNullOrEmpty()) {

            edges.forEach { edge ->

                val node = edge
                    .asJsonObjectOrNull()
                    ?.get("node")
                    ?.asJsonObjectOrNull()
                    ?: return@forEach

                node["video_url"]
                    ?.asJsonPrimitiveOrNull()
                    ?.contentOrNull
                    ?.let {
                        return it
                    }

                val nodeVideos = node["video_versions"]
                    ?.asJsonArrayOrNull()

                if (!nodeVideos.isNullOrEmpty()) {

                    getBestVideo(nodeVideos)?.let {
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

                val obj = video.asJsonObjectOrNull()
                    ?: return@mapNotNull null

                val url = obj["url"]
                    ?.asJsonPrimitiveOrNull()
                    ?.contentOrNull

                val width = obj["width"]
                    ?.asJsonPrimitiveOrNull()
                    ?.intOrNull ?: 0

                val height = obj["height"]
                    ?.asJsonPrimitiveOrNull()
                    ?.intOrNull ?: 0

                url?.let {
                    Triple(it, width, height)
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

        val candidates = media["image_versions2"]
            ?.asJsonObjectOrNull()
            ?.get("candidates")
            ?.asJsonArrayOrNull()

        val bestImage = candidates
            ?.mapNotNull { candidate ->

                val obj = candidate.asJsonObjectOrNull()
                    ?: return@mapNotNull null

                val url = obj["url"]
                    ?.asJsonPrimitiveOrNull()
                    ?.contentOrNull

                val width = obj["width"]
                    ?.asJsonPrimitiveOrNull()
                    ?.intOrNull ?: 0

                url?.let {
                    it to width
                }
            }
            ?.maxByOrNull { it.second }
            ?.first

        return bestImage
            ?: media["display_url"]
                ?.asJsonPrimitiveOrNull()
                ?.contentOrNull
    }

    // ---------------------------------------------------------
    // CSRF TOKEN
    // ---------------------------------------------------------

    private suspend fun fetchCsrfToken(): String {

        client.get("$INSTAGRAM_BASE_URL/") {

            header("User-Agent", USER_AGENT)
            header("Accept", "text/html")
        }

        return client.cookies("$INSTAGRAM_BASE_URL/")
            .firstOrNull {
                it.name == "csrftoken"
            }
            ?.value
            .orEmpty()
    }

    // ---------------------------------------------------------
    // GRAPHQL METADATA FALLBACK
    // ---------------------------------------------------------

    private suspend fun fetchMetadata(
        shortcode: String,
        csrf: String
    ): JsonObject {

        val variables = buildJsonObject {

            put("shortcode", shortcode)

            put(
                "__relay_internal__pv__PolarisAIGMMediaWebLabelEnabledrelayprovider",
                false
            )
        }.toString()

        val response = client.submitForm(
            url = "$INSTAGRAM_BASE_URL/graphql/query/",
            formParameters = parameters {

                append("variables", variables)
                append("doc_id", DOC_ID)
                append("server_timestamps", "true")
            }
        ) {

            header("User-Agent", USER_AGENT)
            header("Referer", "$INSTAGRAM_BASE_URL/")
            header("X-csrftoken", csrf)
            header("X-Requested-With", "XMLHttpRequest")
            header("Accept", "*/*")
        }

        val text = response.bodyAsText()

        println("Instagram GraphQL status: ${response.status}")

        require(response.status.isSuccess()) {
            "Instagram GraphQL HTTP error: ${response.status}"
        }

        val json = try {

            Json.parseToJsonElement(text).jsonObject

        } catch (e: Exception) {

            error(
                "Unable to parse Instagram GraphQL response: ${e.message}"
            )
        }

        // Log GraphQL errors, including partial-data responses.
        val errors = json["errors"]
            ?.asJsonArrayOrNull()

        if (!errors.isNullOrEmpty()) {
            println("Instagram GraphQL errors: $errors")
        }

        return json
    }

    // ---------------------------------------------------------
    // DOWNLOAD ONE FILE
    // ---------------------------------------------------------

    suspend fun downloadFile(
        url: String
    ): ByteArray {

        val response = client.get(url) {

            header("User-Agent", USER_AGENT)
            header("Referer", "$INSTAGRAM_BASE_URL/")
        }

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

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    println(
                        "Media download failed: ${e.message}"
                    )

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

    // ---------------------------------------------------------
    // CLOSE
    // ---------------------------------------------------------

    fun close() {
        client.close()
    }
}
