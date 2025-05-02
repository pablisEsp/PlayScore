package com.playscore.project

import androidx.compose.runtime.Composable
import auth.FirebaseAuthInterface
import navigation.AppNavHost
import org.koin.compose.koinInject
import ui.theme.AppTheme

@Composable
fun App(
    firebaseAuth: FirebaseAuthInterface = koinInject()
) {
    AppTheme {
        AppNavHost(firebaseAuth = firebaseAuth)
    }
}