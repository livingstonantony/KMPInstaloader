package dev.livin.instaloader.cli

import dev.livin.instaloader.repository.InstaScraper
import dev.livin.instaloader.utils.getCurrentDateTimeString
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    if (args.isEmpty() || args.contains("--help") || args.contains("-h")) {
        printHelp()
        return
    }

    val url = args.lastOrNull { !it.startsWith("-") }
    
    if (url == null) {
        println("Error: No URL provided.")
        printHelp()
        return
    }

    println("Fetching post from: $url")
    
    val repository = InstaScraper
    
    runBlocking {
        try {
            val post = repository.fetchPostData(url)
            println("SUCCESS! Post shortcode: ${post.shortcode}")
            println("Caption: ${post.caption}")
            println("Images found (${post.images.size}): ${post.images}")
            println("Video found: ${post.video}")
            
            val fileNameBase = getCurrentDateTimeString()
            
            // Download images
            post.images.forEachIndexed { index, imageUrl ->
                println("Image ${index + 1} URL: $imageUrl")
            }
            
            // Download video if available
            post.video?.let { videoUrl ->
                println("Video URL: $videoUrl")
            }
            
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        } finally {
            repository.close()
        }
    }
}

fun printHelp() {
    println("KMP InstaLoader CLI")
    println("Usage: java -jar kmpinstaloader.jar [OPTIONS] <INSTAGRAM_URL>")
    println()
    println("Options:")
    println("  -h, --help    Show this help message")
}
