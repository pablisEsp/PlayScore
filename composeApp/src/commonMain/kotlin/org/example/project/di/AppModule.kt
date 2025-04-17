package org.example.project.di

import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import viewmodel.LoginViewModel
import viewmodel.RegisterViewModel
import data.AuthService

/**
 * Koin module for ViewModels and other app components
 */
val appModule = module {
    // Services
    single { AuthService() }
    
    // ViewModels
    factory { LoginViewModel(get()) }
    factory { RegisterViewModel(get()) }
}