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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object InstaScraper {

    /**
     * Instagram's current document ID used for
     * media shortcode web information.
     */
    private const val DOC_ID = "27128499623469141"

    private const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/142.0.0.0 Safari/537.36"

    /**
     * HttpCookies stores the csrftoken cookie received
     * from Instagram and sends it with subsequent requests.
     */
    private val client = createHttpClient()

    /**
     * Download media from a public Instagram post.
     *
     * Example:
     *
     * https://www.instagram.com/p/ABC123xyz/
     */
    suspend fun downloadPost(shortcodeUrl: String): InstaPost {

        // ---------------------------------------------------------
        // 1. Extract shortcode
        // ---------------------------------------------------------

        val shortcode = shortcodeUrl
            .getInstagramShortCode()
            ?: error("Invalid Instagram URL: $shortcodeUrl")

        println("SHORT_CODE: $shortcode")

        // ---------------------------------------------------------
        // 2. Get CSRF token
        // ---------------------------------------------------------

        val csrf = fetchCsrfToken()

        println("CSRF: $csrf")

        require(csrf.isNotBlank()) { "Instagram csrftoken was not found" }

        // ---------------------------------------------------------
        // 3. Fetch Instagram metadata
        // ---------------------------------------------------------

        val json = fetchMetadata(
            shortcode = shortcode,
            csrf = csrf
        )

        println(
            "Meta data:\n$json"
        )

        // ---------------------------------------------------------
        // 4. Check GraphQL response
        // ---------------------------------------------------------

        val data = json["data"]
            ?.jsonObject
            ?: run {

                val errorMessage = json["errors"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.content

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

        // ---------------------------------------------------------
        // 5. Extract media information
        // ---------------------------------------------------------

        val webInfo = data[
            "xdt_api__v1__media__shortcode__web_info"
        ]
            ?.jsonObject
            ?: error(
                "Instagram response does not contain " +
                        "'xdt_api__v1__media__shortcode__web_info'"
            )

        // ---------------------------------------------------------
        // 6. Extract first media item
        // ---------------------------------------------------------

        val item = webInfo["items"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?: error(
                "Instagram returned an empty media items array"
            )

        // ---------------------------------------------------------
        // 7. Extract image URLs
        // ---------------------------------------------------------

        val imageUrls = extractImageUrls(item)
//        val videoUrls = extractVideoUrls(item)

        println("\nURL =====\n")

        if (imageUrls.isEmpty()) {
            println(
                "No image URLs found"
            )
        } else {
            imageUrls.forEachIndexed { index, url ->
                println("${index + 1}. $url")
            }
        }

        return InstaPost(
            shortcode = shortcode,
            caption = "",
            images = imageUrls.filter { it.isNotEmpty() },
            video = ""
        )
    }

    /**
     * Step 1:
     *
     * Open Instagram homepage so Instagram can
     * provide the csrftoken cookie.
     */
    private suspend fun fetchCsrfToken(): String {

        val response = client.get(
            "https://www.instagram.com/"
        ) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
        }

        println("Instagram homepage status: ${response.status}")

        val cookies = client.cookies("https://www.instagram.com/")

        val csrfToken = cookies
            .firstOrNull { it.name == "csrftoken" }?.value.orEmpty()

        return csrfToken
    }

    /**
     * Step 2:
     *
     * Request Instagram GraphQL metadata.
     */
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
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "*/*")
            header("Referer", "https://www.instagram.com/")
            header("X-csrftoken", csrf)
            header("X-Requested-With", "XMLHttpRequest")
        }

        val text = response.bodyAsText()

        println("GraphQL HTTP status: ${response.status}")

        println("Instagram GraphQL response:\n$text")

        // ---------------------------------------------------------
        // HTTP-level error
        // ---------------------------------------------------------

        require(response.status.isSuccess()) {
            "Instagram HTTP error " +
                    "${response.status}: $text"
        }

        // ---------------------------------------------------------
        // JSON parsing
        // ---------------------------------------------------------

        return try {
            Json.parseToJsonElement(text)
                .jsonObject

        } catch (e: Exception) {
            error(
                "Unable to parse Instagram response as JSON.\n" +
                        "Response: $text\n" +
                        "Error: ${e.message}"
            )
        }
    }

    /**
     * Extract image URLs from a media item.
     *
     * Supports:
     *
     * 1. Single image post
     * 2. Carousel / album
     */
    private fun extractImageUrls(
        item: JsonObject
    ): List<String> {
        // ---------------------------------------------------------
        // Carousel / Album
        // ---------------------------------------------------------
        item["carousel_media"]
            ?.jsonArray
            ?.let { carousel ->

                return carousel.mapNotNull { media ->

                    bestCandidate(
                        media.jsonObject
                    )
                }
            }
        // ---------------------------------------------------------
        // Single image
        // ---------------------------------------------------------

        return listOfNotNull(
            bestCandidate(item)
        )
    }

    private fun extractVideoUrls(item: JsonObject): List<String> {

        // Single video / Reel
        item["video_versions"]
            ?.jsonArray
            ?.let { videos ->
                return videos
                    .mapNotNull { video ->
                        video.jsonObject["url"]
                            ?.jsonPrimitive
                            ?.content
                    }
            }

        // Carousel containing videos
        item["carousel_media"]
            ?.jsonArray
            ?.let { carousel ->

                return carousel.flatMap { mediaElement ->

                    val media = mediaElement.jsonObject

                    media["video_versions"]
                        ?.jsonArray
                        ?.mapNotNull { video ->
                            video.jsonObject["url"]
                                ?.jsonPrimitive
                                ?.content
                        }
                        ?: emptyList()
                }
            }

        return emptyList()
    }

    /**
     * Select the highest resolution image candidate.
     *
     * Instagram may return multiple candidates:
     *
     * - 150x150
     * - 320x320
     * - 640x640
     * - 1080x1080
     *
     * We select the candidate with the largest width.
     */
    private fun bestCandidate(
        media: JsonObject
    ): String? {
        return media["image_versions2"]
            ?.jsonObject
            ?.get("candidates")
            ?.jsonArray
            ?.maxByOrNull { candidate ->
                candidate
                    .jsonObject["width"]
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: 0
            }
            ?.jsonObject
            ?.get("url")
            ?.jsonPrimitive
            ?.content
    }


    suspend fun downloadFile(url: String): ByteArray {
        val response = client.get(url) {
            // Optional: you can add headers if some CDNs require them
            header("User-Agent", "Mozilla/5.0")
        }

        if (response.status.value in 200..299) {
            return response.body<ByteArray>()
        } else {
            throw Exception("Failed to download file: ${response.status}")
        }
    }

    suspend fun downloadFiles(urls: List<String>): List<ByteArray?> = coroutineScope {
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

    fun close() {
        client.close()
    }

}
