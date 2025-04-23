package com.playscore.project.di

import auth.FirebaseAuthInterface
import auth.createFirebaseAuth
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import viewmodel.LoginViewModel
import viewmodel.RegisterViewModel
import viewmodel.HomeViewModel

val appModule = module {
    // Firebase Auth
    single<FirebaseAuthInterface> { createFirebaseAuth() }

    // ViewModels
    viewModel { LoginViewModel(auth = get()) }
    viewModel { RegisterViewModel(auth = get()) }
    viewModel { HomeViewModel(auth = get()) }
}
