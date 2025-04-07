package org.example.project.di

import data.AuthService
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import viewmodel.LoginViewModel
import viewmodel.RegisterViewModel

/**
 * Koin module for ViewModels and other app components
 */
val appModule = module {
    // Services
    single { AuthService() }

    // ViewModels - inject AuthService
    viewModel { LoginViewModel(authService = get()) }
    viewModel { RegisterViewModel(authService = get()) }
}