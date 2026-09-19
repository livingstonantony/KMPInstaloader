package dev.livin.instaloader.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
    var isDynamicColorEnabled by mutableStateOf(true)
}

val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

@Composable
expect fun getColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme

@Composable
fun InstaLoaderTheme(
    content: @Composable () -> Unit,
) {
    val themeSettings = remember { ThemeSettings() }
    
    val darkTheme = when (themeSettings.currentTheme) {
        AppTheme.Light -> false
        AppTheme.Dark -> true
        AppTheme.System -> isSystemInDarkTheme()
    }
    
    val colorScheme = getColorScheme(
        darkTheme = darkTheme,
        dynamicColor = themeSettings.isDynamicColorEnabled,
    )
    
    CompositionLocalProvider(LocalThemeSettings provides themeSettings) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
