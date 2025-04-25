package com.playscore.project.di

import android.content.Context
import auth.FirebaseAuthInterface
import auth.createFirebaseAuth
import database.FirebaseDatabaseInterface
import database.createFirebaseDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun providePlatformModule(): Module = androidPlatformModule

val androidPlatformModule = module {
    factory { (context: Context) -> context }
    single<FirebaseAuthInterface> { createFirebaseAuth() }
    single<FirebaseDatabaseInterface> { createFirebaseDatabase() }
}