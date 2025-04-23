package com.playscore.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this) // Initialize Firebase

        setContent {
            MaterialTheme {
                App() // Your shared multiplatform UI
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}