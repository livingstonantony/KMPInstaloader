package dev.livin.instaloader.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun getColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    return if (darkTheme) darkColorScheme() else lightColorScheme()
}
