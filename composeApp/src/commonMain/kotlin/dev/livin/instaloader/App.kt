package dev.livin.instaloader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.livin.instaloader.ui.InstaLoaderScreen
import dev.livin.instaloader.ui.InstaLoaderTheme
import dev.livin.instaloader.ui.SettingsScreen

enum class Screen(val title: String) {
    Home("Home"), Settings("Settings");

}
    @OptIn(ExperimentalMaterial3Api::class)
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
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Settings,
                            onClick = { currentScreen = Screen.Settings },
                            icon = {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },

                            )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    when (currentScreen) {
                        Screen.Home -> InstaLoaderScreen(postUrl)
                        Screen.Settings -> SettingsScreen()
                    }
                }
            }
        }

}

