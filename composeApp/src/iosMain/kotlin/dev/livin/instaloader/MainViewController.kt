package dev.livin.instaloader

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun mainViewController(): UIViewController = ComposeUIViewController {
    App()
}
