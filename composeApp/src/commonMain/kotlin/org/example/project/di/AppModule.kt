package org.example.project.di

import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import viewmodel.LoginViewModel
import viewmodel.RegisterViewModel
import viewmodel.HomeViewModel
import data.AuthService
import data.TokenManager

/**
 * Koin module for ViewModels and other app components
 */
val appModule = module {
    // Services
    single { TokenManager() }
    single { AuthService(get()) }
    
    // ViewModels
    factory { LoginViewModel(get()) }
    factory { RegisterViewModel(get()) }
    factory { HomeViewModel(get()) }
}