package com.playscore.project.di

import android.content.Context
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import firebase.auth.createFirebaseAuth
import firebase.database.createFirebaseDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun providePlatformModule(): Module = androidPlatformModule

val androidPlatformModule = module {
    factory { (context: Context) -> context }
    single<FirebaseAuthInterface> { createFirebaseAuth() }
    single<FirebaseDatabaseInterface> { createFirebaseDatabase() }
}