# KMPInstaloader

A Kotlin Multiplatform (KMP) library for downloading media content from public Instagram posts. 

Inspired by the Python [instaloader](https://github.com/instaloader/instaloader) library, this project aims to provide a similar capability for the Kotlin ecosystem, supporting Android, iOS, Desktop, Web, and Server.

> **Note:** This project is in its very initial stages.

## Features

- Extract Instagram shortcodes from URLs (Posts, Reels, TV).
- Fetch media metadata using Instagram's public GraphQL API.
- Support for single image/video posts.
- Support for sidecar (carousel) posts with multiple images.
- Multiplatform support via Ktor and Kotlinx Serialization.

## Project Structure

- **`:shared`**: The core module containing the logic for interacting with Instagram.
    - `network`: Ktor HTTP client configuration.
    - `repository`: `InstaRepository` for fetching post data.
    - `model`: Data models for Instagram API responses and the internal `InstaPost`.
    - `utils`: Utility functions like `getInstagramShortCode`.
- **`:composeApp`**: Shared UI using Compose Multiplatform for Android, iOS, Desktop, and Web.
- **`:server`**: Ktor server implementation.

## Getting Started

### Prerequisites

- Android Studio or IntelliJ IDEA.
- JDK 17 or higher.

### Usage in Code

To use the loader in your common code, you can use `InstaRepository`:

```kotlin
val repository = InstaRepository()

// Fetch post details using a URL
val post = repository.getPost("https://www.instagram.com/p/SHORTCODE/")

println("Caption: ${post.caption}")
println("Images: ${post.images}")
if (post.video != null) {
    println("Video URL: ${post.video}")
}
```

## Implementation Details

The library uses a specific `doc_id` to query Instagram's GraphQL endpoint. It mimics a mobile browser user-agent to access public data without requiring official API credentials, similar to how the Python `instaloader` works.

## Supported Platforms

- Android
- iOS
- Desktop (JVM)
- Web (Kotlin/JS)
- Server (Ktor)

### Sample:

<img src="https://github.com/livingstonantony/KMPInstaloader/blob/master/doc/img.png" width="300"> <img src="https://github.com/livingstonantony/KMPInstaloader/blob/master/doc/ios.png" width="300">

<img src="https://github.com/livingstonantony/KMPInstaloader/blob/master/doc/desktop.png" width="600">

### Note ℹ️

🙌 Looking for contributors!

If you're interested in:

- Kotlin Multiplatform (KMP)
- Networking / APIs
- Reverse engineering / scraping
- Open source collaboration
- You’re very welcome to contribute!

🛠️ Ways to help:

- Improve existing features
- Add new functionality
- Fix bugs
- Improve documentation
- Suggest ideas
Even small contributions are appreciated 💙

Let’s build something useful for the Kotlin community together!

Feel free to ⭐ the repo if you find it interesting!


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details (if available).
