package dev.livin.instaloader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.livin.instaloader.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val themeSettings = LocalThemeSettings.current
    val uriHandler = LocalUriHandler.current
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.primary
        )

        // Theme Section
        Text(
            text = "Appearance",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                // Theme Preference Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBottomSheet = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme Icon")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Theme", fontWeight = FontWeight.Medium)
                            Text(
                                text = when (themeSettings.currentTheme) {
                                    AppTheme.Light -> "Light"
                                    AppTheme.Dark -> "Dark"
                                    AppTheme.System -> "System Default"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Dynamic Color Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = "Dynamic Color Icon")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Dynamic color", fontWeight = FontWeight.Medium)
                            Text(
                                text = "Use system dynamic colors if supported",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = themeSettings.isDynamicColorEnabled,
                        onCheckedChange = { themeSettings.isDynamicColorEnabled = it }
                    )
                }
            }
        }

        // Contact Me Section
        Text(
            text = "Contact me",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                // Email
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("mailto:${BuildConfig.EMAIL}") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Email Icon")
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Email", fontWeight = FontWeight.Medium)
                        Text(BuildConfig.EMAIL, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Discord
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(BuildConfig.DISCORD) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, contentDescription = "Discord Icon")
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Discord", fontWeight = FontWeight.Medium)
                        Text(BuildConfig.DISCORD, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // GitHub
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(BuildConfig.GITHUB) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = "GitHub Icon")
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Star on GitHub", fontWeight = FontWeight.Medium)
                        Text(BuildConfig.GITHUB, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // About Section
        Text(
            text = "About",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                // App Version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Version Icon")
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("App version", fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = BuildConfig.VERSION,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Feedback or Bug
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("mailto:${BuildConfig.EMAIL}?subject=KMPInstaloader%20Feedback") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = "Feedback Icon")
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Feedback or Bug", fontWeight = FontWeight.Medium)
                        Text("Open mail app to: Livingston Antony", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Bottom Sheet for Theme Selection
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = "Select Theme",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Light Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            themeSettings.currentTheme = AppTheme.Light
                            showBottomSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeSettings.currentTheme == AppTheme.Light,
                        onClick = {
                            themeSettings.currentTheme = AppTheme.Light
                            showBottomSheet = false
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Light")
                }

                // Dark Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            themeSettings.currentTheme = AppTheme.Dark
                            showBottomSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeSettings.currentTheme == AppTheme.Dark,
                        onClick = {
                            themeSettings.currentTheme = AppTheme.Dark
                            showBottomSheet = false
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dark")
                }

                // System Default Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            themeSettings.currentTheme = AppTheme.System
                            showBottomSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeSettings.currentTheme == AppTheme.System,
                        onClick = {
                            themeSettings.currentTheme = AppTheme.System
                            showBottomSheet = false
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Default")
                }
            }
        }
    }
}
