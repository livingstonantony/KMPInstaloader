package dev.livin.instaloader.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class CustomExtensionsTest {

    @Test
    fun getInstagramShortCode_returns_correct_shortcode() {
        val testCases = listOf(
            "https://www.instagram.com/p/DVv7e05keJt/" to "DVv7e05keJt",
            "https://www.instagram.com/reel/DVv7e05keJt/" to "DVv7e05keJt",
            "DVv7e05keJt" to "DVv7e05keJt"
        )

        testCases.forEach { (input, expected) ->
            // Calling it on an empty string since it's an extension but doesn't use 'this'
            val actual = input.getInstagramShortCode()
            assertEquals(expected, actual, "Failed for input: $input")
        }
    }

    @Test
    fun getInstagramShortCode_returns_null_for_invalid_input() {
        val invalidInputs = listOf(
            "https://www.google.com",
            "short", // too short (regex says 5+)
            "invalid url with spaces"
        )

        invalidInputs.forEach { input ->
            val actual = input.getInstagramShortCode()
            assertEquals(null, actual, "Should return null for input: $input")
        }
    }
}
