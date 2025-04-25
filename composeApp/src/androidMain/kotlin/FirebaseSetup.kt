package firebase

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseSetup {
    fun initialize(context: Context) {
        // Initialize Firebase
        FirebaseApp.initializeApp(context)
    }
}