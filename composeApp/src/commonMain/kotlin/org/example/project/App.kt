package org.example.project

import androidx.compose.runtime.Composable
import org.example.project.di.appModule
import org.example.project.di.navigationModule
import org.example.project.navigation.AppNavHost
import org.koin.compose.KoinApplication


@Composable
fun App() {
    // Setup Koin at the app entry point
    KoinApplication(application = {
        modules(appModule, navigationModule)
    }) {
        AppNavHost()
    }
}