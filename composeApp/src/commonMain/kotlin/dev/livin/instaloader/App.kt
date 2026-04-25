package dev.livin.instaloader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.livin.instaloader.ui.PostDetails
import dev.livin.instaloader.utils.formatSize
import dev.livin.instaloader.utils.getCurrentDateTimeString
import dev.livin.instaloader.utils.saveImageToFile
import dev.livin.instaloader.utils.saveImagesToFiles
import dev.livin.instaloader.utils.saveVideoToFile
import dev.livin.instaloader.viewmodel.FileType
import dev.livin.instaloader.viewmodel.InstaUiState
import dev.livin.instaloader.viewmodel.InstaViewModel
import kmpinstaloader.composeapp.generated.resources.Res
import kmpinstaloader.composeapp.generated.resources.download_2
import org.jetbrains.compose.resources.painterResource

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
    val fileState by viewModel.getFileByUrl.collectAsStateWithLifecycle()
    val filesState by viewModel.getFilesByUrl.collectAsStateWithLifecycle()


    LaunchedEffect(fileState) {
        if (fileState is InstaUiState.Success) {
            val bytes = (fileState as InstaUiState.Success<Pair<ByteArray?, FileType>>).post
            bytes.let { (file, type) ->
                val fileName = getCurrentDateTimeString()

                if (file == null) return@let

                when (type) {
                    is FileType.Image -> {
                        saveImageToFile(file, fileName)
                    }

                    is FileType.Video -> {
                        saveVideoToFile(file, fileName)
                    }
                }
            }
        }
    }
    LaunchedEffect(postUrl) {
        // When user comes from shared link, user no need to click on download tail icon to fetch the post
        postUrl?.let { url ->
            viewModel.fetchPost(url)
        }
    }

    // 🔥 Handle multiple images save (SIDE EFFECT)
    LaunchedEffect(filesState) {
        if (filesState is InstaUiState.Success) {
            val files = (filesState as InstaUiState.Success<List<ByteArray?>>).post
            if (files.isNotEmpty()) {
                saveImagesToFiles(files)
            }
        }
    }

    // Apply safe drawing insets as content padding
    // Helps avoid edge-to-edge UI issues like overlapping
    // with status bar, navigation bar, notch, or gesture areas
    // Especially to avoid UI issue in the desktop app
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val topInset = with(density) {
        WindowInsets.safeDrawing.getTop(this).toDp()
    }

    val bottomInset = with(density) {
        WindowInsets.safeDrawing.getBottom(this).toDp()
    }

    val startInset = with(density) {
        WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp()
    }

    val endInset = with(density) {
        WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = topInset,
                bottom = bottomInset,
                start = startInset,
                end = endInset
            )
            .padding(16.dp),
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
            singleLine = true,
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .clickable {
                            viewModel.fetchPost(shortcode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.download_2),
                        contentDescription = "Fetch",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )


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
                    isDownloading = fileState is InstaUiState.Loading,
                    isDownloadAllLoading = filesState is InstaUiState.Loading,
                    downloadAll = {
                        viewModel.getFilesByUrl(it)
                    },
                    downloadFile = { file, type ->
                        viewModel.getFileByUrl(file, type)
                    }
                )


            }

            is InstaUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        when (val state = fileState) {
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
                val imageBytes = state.post.first
                if (imageBytes != null) {
                    println("Download Image Size: ${imageBytes.formatSize()}")

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
        when (val state = filesState) {

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

                    Text(
                        "Saved ${files.size} files to /Pictures/Instaloader",
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