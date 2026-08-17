package dev.livin.instaloader.cli

import dev.livin.instaloader.repository.InstaRepository
import dev.livin.instaloader.utils.saveImageToFile
import dev.livin.instaloader.utils.saveVideoToFile
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
    
    val repository = InstaRepository()
    
    runBlocking {
        try {
            val post = repository.getPost(url)
//            println("Post found: ${post.caption.take(50)}...")
            
            val fileNameBase = getCurrentDateTimeString()
            
            // Download images
            post.images.forEachIndexed { index, imageUrl ->
                println("Downloading image ${index + 1}...")
//                val bytes = repository.downloadFile(imageUrl)
//                val path = saveImageToFile(bytes, "${fileNameBase}_$index")
//                println("Image saved to: $path")
            }
            
            // Download video if available
            post.video?.let { videoUrl ->
                println("Downloading video...")
//                val bytes = repository.downloadFile(videoUrl)
//                val path = saveVideoToFile(bytes, fileNameBase)
//                println("Video saved to: $path")
            }
            
        } catch (e: Exception) {
            println("Error: ${e.message}")
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
