package auth

actual fun createFirebaseAuth(): FirebaseAuthInterface = FirebaseAuthAndroid()
