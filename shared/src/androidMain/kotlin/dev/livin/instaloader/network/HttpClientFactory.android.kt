package dev.livin.instaloader.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    followRedirects = true

    install(HttpCookies)

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }


    install(DefaultRequest) {
        header(
            HttpHeaders.UserAgent,
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
        )
        header("X-IG-App-ID", "936619743392459")
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("KtorLog: $message")
            }
        }
        level = LogLevel.ALL
    }

}