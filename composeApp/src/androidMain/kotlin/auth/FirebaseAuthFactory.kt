package auth

import firebase.auth.FirebaseAuthAndroid

actual fun createFirebaseAuth(): FirebaseAuthInterface {
    return FirebaseAuthAndroid()
}