package dev.livin.instaloader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.livin.instaloader.ui.PostDetails
import dev.livin.instaloader.utils.getCurrentDateTimeString
import dev.livin.instaloader.utils.saveImageToFile
import dev.livin.instaloader.viewmodel.InstaUiState
import dev.livin.instaloader.viewmodel.InstaViewModel
import dev.livin.instaloader.viewmodel.formatSize

@Composable
@Preview
fun App(postUrl: String? = "") {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            InstaLoaderScreen(postUrl)
        }
    }
}

@Composable
fun InstaLoaderScreen(
    postUrl: String?,
    viewModel: InstaViewModel = viewModel { InstaViewModel() }
) {
    var shortcode by remember { mutableStateOf(postUrl ?: "") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val getFileByUrl by viewModel.getFileByUrl.collectAsStateWithLifecycle()
    val getFilesByUrl by viewModel.getFilesByUrl.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Insta Loader",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = shortcode,
            onValueChange = { shortcode = it },
            label = { Text("Enter Shortcode") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.fetchPost(shortcode) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Post")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is InstaUiState.Idle -> {
                Text("Enter a shortcode to see post details")
            }

            is InstaUiState.Loading -> {
                CircularProgressIndicator()
            }

            is InstaUiState.Success -> {
                PostDetails(
                    state.post,
                    isDownloading = getFileByUrl is InstaUiState.Loading,
                    isDownloadAllLoading = getFilesByUrl is InstaUiState.Loading,
                    downloadAll = {
                        viewModel.getFilesByUrl(it)
                    }) {
                    viewModel.getFileByUrl(it)
                }
            }

            is InstaUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        when (val state = getFileByUrl) {
            is InstaUiState.Idle -> {
//                Text("Enter a shortcode to see post details")
            }

            is InstaUiState.Loading -> {
                Text(
                    "Downloading...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            is InstaUiState.Success -> {
                val imageBytes = state.post
                if (imageBytes != null) {
                    println("Download Image Size: ${imageBytes.formatSize()}")
                    val fileName = getCurrentDateTimeString()
                    val path = saveImageToFile(imageBytes, fileName)

                    Text(
                        "Saved successfully at /Pictures/Instaloader",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    Text(
                        "No image found",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            is InstaUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        when (val state = getFilesByUrl) {

            is InstaUiState.Idle -> {
                // Do nothing
            }

            is InstaUiState.Loading -> {
                Text(
                    "Downloading all images...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            is InstaUiState.Success -> {
                val files = state.post

                if (files.isNotEmpty()) {

                    val paths = saveImagesToFiles(files)

                    Text(
                        "Saved ${paths.size} files to /Pictures/Instaloader",
                        modifier = Modifier.padding(top = 16.dp)
                    )

                } else {
                    Text(
                        "No files found",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            is InstaUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

    }
}

fun saveImagesToFiles(files: List<ByteArray?>): List<String> {
    val savedPaths = mutableListOf<String>()
    val baseName = getCurrentDateTimeString() // SAME format, single call

    files.forEachIndexed { index, bytes ->
        if (bytes != null) {
            val fileName = "${baseName}_${index+1}"
            val path = saveImageToFile(bytes, fileName)
            savedPaths.add(path)
        }
    }

    return savedPaths
}
