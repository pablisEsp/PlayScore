package com.playscore.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.playscore.project.di.appModule
import com.playscore.project.di.desktopPlatformModule
import org.koin.core.context.GlobalContext.startKoin

fun main() {
    // Initialize Koin for desktop
    startKoin {
        modules(appModule, desktopPlatformModule)
    }


    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PlayScore",
            state = WindowState(placement = WindowPlacement.Maximized)
        ) {
            App()
        }
    }
}