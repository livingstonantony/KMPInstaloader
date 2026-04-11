package dev.livin.instaloader.repository

import dev.livin.instaloader.model.InstaGrapQlResponse
import dev.livin.instaloader.model.InstaPost
import dev.livin.instaloader.network.createHttpClient
import dev.livin.instaloader.utils.getInstagramShortCode
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class InstaRepository {

    private val client = createHttpClient()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getPost(url: String): InstaPost {

        val shortcode = url.getInstagramShortCode()?:""
        val variables = """{"shortcode":"${shortcode}"}"""
        val docId = "10015901848480474"

        val httpResponse = client.get("https://www.instagram.com/graphql/query/") {
            parameter("doc_id", docId)
            parameter("variables", variables)
            header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.3")
            header("X-IG-App-ID", "936619743392459")
            header("Accept", "application/json")
        }

        val rawJson = httpResponse.bodyAsText()


        println("=== InstaRepository DEBUG ===")
        println("Status : ${httpResponse.status}")
        println("Raw Json : $rawJson")
        println("================================")

        if (!rawJson.trimStart().startsWith("{")) {
            throw Exception("Unexpected response (status ${httpResponse.status}): $rawJson")
        }

        val response = json.decodeFromString<InstaGrapQlResponse>(rawJson)
        val media =
            response.data?.media ?: throw Exception("No media found for shortcode: $shortcode")

        val caption = media.captionEdge?.edges?.firstOrNull()?.node?.text ?: ""

        val images = if (media.sidecar != null && media.sidecar.edges.isNotEmpty()) {
            media.sidecar.edges.map { it.node?.displayUrl ?: "" }
        } else {
            listOf(media.displayUrl)
        }

        return InstaPost(
            shortcode = shortcode,
            caption = caption,
            images = images.filter { it.isNotEmpty() },
            video = media.video
        )

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

}