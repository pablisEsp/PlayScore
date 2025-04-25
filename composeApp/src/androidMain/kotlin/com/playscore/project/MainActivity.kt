package com.playscore.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.google.firebase.FirebaseApp
import com.playscore.project.di.androidPlatformModule
import com.playscore.project.di.appModule
import com.playscore.project.di.navigationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase only if it hasn't been initialized yet
        if (FirebaseApp.getApps(this).isEmpty()) {
            firebase.FirebaseSetup.initialize(this)
        }

        // Initialize Koin for Android
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule, navigationModule, androidPlatformModule)
        }


        setContent {
            MaterialTheme {
                App() // Your shared multiplatform UI
            }
        }
    }
}
