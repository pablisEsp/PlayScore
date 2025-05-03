package com.playscore.project.di


import data.TokenManager
import firebase.auth.FirebaseAuthInterface
import firebase.auth.createFirebaseAuth
import firebase.database.FirebaseDatabaseInterface
import firebase.database.createFirebaseDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import viewmodel.HomeViewModel
import viewmodel.LoginViewModel
import viewmodel.RegisterViewModel
import kotlin.coroutines.CoroutineContext

// Common module for shared dependencies
val appModule = module {
    // Firebase
    single<FirebaseAuthInterface> { createFirebaseAuth() } // Provided in platform modules
    single<FirebaseDatabaseInterface> { createFirebaseDatabase() } // Provided in platform modules


    // Common singletons
    singleOf(::TokenManager)

    // Provide a default CoroutineContext for ViewModels
    single<CoroutineContext> { Dispatchers.Main }

    // ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::HomeViewModel)
}

// Platform module (to be implemented in platform-specific source sets)
val platformModule = module {
    // Placeholder for platform-specific dependencies
}