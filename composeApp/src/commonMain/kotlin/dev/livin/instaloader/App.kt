package dev.livin.instaloader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.livin.instaloader.viewmodel.InstaUiState
import dev.livin.instaloader.viewmodel.InstaViewModel

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            InstaLoaderScreen()
        }
    }
}

@Composable
fun InstaLoaderScreen(viewModel: InstaViewModel = viewModel { InstaViewModel() }) {
    var shortcode by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                PostDetails(state.post)
            }
            is InstaUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PostDetails(post: dev.livin.instaloader.model.InstaPost) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Caption:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = post.caption,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Images: (${post.images.size})",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(post.images) { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentScale = ContentScale.Inside
                )
            }
        }
    }
}
