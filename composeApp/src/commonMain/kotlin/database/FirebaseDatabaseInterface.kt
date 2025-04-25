package database

import data.model.User

interface FirebaseDatabaseInterface {
    suspend fun saveUserData(userData: User): Boolean
    suspend fun getUserData(uid: String?): User?
    suspend fun updateUserData(uid: String, updates: Map<String, Any>): Boolean
    suspend fun deleteUser(uid: String): Boolean
}