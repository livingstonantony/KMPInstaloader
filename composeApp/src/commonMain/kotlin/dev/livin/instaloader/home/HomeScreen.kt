package dev.livin.instaloader.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.livin.instaloader.ui.PostDetails
import dev.livin.instaloader.utils.getCurrentDateTimeString
import dev.livin.instaloader.utils.saveImageToFile
import dev.livin.instaloader.utils.saveImagesToFiles
import dev.livin.instaloader.utils.saveVideoToFile
import dev.livin.instaloader.viewmodel.DownloadedFile
import dev.livin.instaloader.viewmodel.FileType
import dev.livin.instaloader.viewmodel.InstaUiState
import dev.livin.instaloader.viewmodel.InstaViewModel
import kmpinstaloader.composeapp.generated.resources.Res
import kmpinstaloader.composeapp.generated.resources.close_icon
import kmpinstaloader.composeapp.generated.resources.download
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    postUrl: String?,
    viewModel: InstaViewModel = viewModel { InstaViewModel() }
) {

    var shortcode by remember { mutableStateOf(postUrl ?: "") }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fileState by viewModel.getFileByUrl.collectAsStateWithLifecycle()
    val filesState by viewModel.getFilesByUrl.collectAsStateWithLifecycle()


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val isLoading =
        uiState is InstaUiState.Loading ||
                fileState is InstaUiState.Loading ||
                filesState is InstaUiState.Loading

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            LargeTopAppBar(
                title = {
                    Text("InstaLoader")
                },
                scrollBehavior = scrollBehavior
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.fetchPost(shortcode) }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.download),
                    contentDescription = null
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            //SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                OutlinedTextField(
                    value = shortcode,
                    onValueChange = { shortcode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Post link") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState) {

                    is InstaUiState.Idle -> Unit

                    is InstaUiState.Loading -> Unit

                    is InstaUiState.Success -> {
                        PostDetails(
                            state.post,
                            isDownloading = fileState is InstaUiState.Loading,
                            isDownloadAllLoading = filesState is InstaUiState.Loading,
                            downloadAll = { viewModel.getFilesByUrl(it) },
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
            }

            // LOADING OVERLAY
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }
    }
}