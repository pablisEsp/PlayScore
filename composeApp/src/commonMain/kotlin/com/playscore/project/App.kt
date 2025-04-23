package com.playscore.project

import androidx.compose.runtime.Composable
import com.playscore.project.di.appModule
import com.playscore.project.di.navigationModule
import com.playscore.project.navigation.AppNavHost
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