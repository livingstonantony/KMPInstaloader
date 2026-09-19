package dev.livin.instaloader

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.livin.instaloader.ui.InstaLoaderScreen
import dev.livin.instaloader.ui.PostDetails
import dev.livin.instaloader.ui.SettingsScreen
import dev.livin.instaloader.utils.formatSize
import dev.livin.instaloader.utils.getCurrentDateTimeString
import dev.livin.instaloader.utils.saveImageToFile
import dev.livin.instaloader.utils.saveImagesToFiles
import dev.livin.instaloader.utils.saveVideoToFile
import dev.livin.instaloader.viewmodel.DownloadedFile
import dev.livin.instaloader.viewmodel.FileType
import dev.livin.instaloader.viewmodel.InstaUiState
import dev.livin.instaloader.viewmodel.InstaViewModel
import kmpinstaloader.composeapp.generated.resources.Res
import kmpinstaloader.composeapp.generated.resources.download_2
import org.jetbrains.compose.resources.painterResource
import dev.livin.instaloader.ui.InstaLoaderTheme

enum class Screen {
    Home, Settings
}

@Composable
@Preview
fun App(postUrl: String? = "") {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    InstaLoaderTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentScreen) {
                    Screen.Home -> InstaLoaderScreen(postUrl)
                    Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}

