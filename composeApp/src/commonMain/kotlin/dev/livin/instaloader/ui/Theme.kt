package dev.livin.instaloader.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppTheme {
    Light, Dark, System
}

class ThemeSettings {
    var currentTheme by mutableStateOf(AppTheme.System)
    var isDynamicColorEnabled by mutableStateOf(value = true)
}

val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

@Composable
fun InstaLoaderTheme(
    content: @Composable () -> Unit,
) {
    val themeSettings = remember { ThemeSettings() }
    
    CompositionLocalProvider(LocalThemeSettings provides themeSettings) {
        val darkTheme = when (themeSettings.currentTheme) {
            AppTheme.Light -> false
            AppTheme.Dark -> true
            AppTheme.System -> isSystemInDarkTheme()
        }
        
        // Use default Material3 color schemes for now
        val colorScheme = if (darkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
        
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
