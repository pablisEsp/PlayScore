package database

actual fun createFirebaseDatabase(): FirebaseDatabaseInterface {
    return FirebaseDatabaseAndroid()
}