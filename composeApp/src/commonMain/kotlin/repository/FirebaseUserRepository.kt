package repository

import data.model.User
import data.model.UserRole
import firebase.database.FirebaseDatabaseInterface
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer

class FirebaseUserRepository(
    private val database: FirebaseDatabaseInterface
) : UserRepository {

    override suspend fun getAllUsers(): List<User> {
        return database.getCollection("users", ListSerializer(serializer<User>()))
    }

    override suspend fun updateUserRole(userId: String, newRole: UserRole): Boolean {
        return database.updateFields("users", userId, mapOf("globalRole" to newRole.name))
    }

    override suspend fun toggleUserBan(userId: String, isBanned: Boolean): Boolean {
        return database.updateFields("users", userId, mapOf("isBanned" to isBanned))
    }

    override suspend fun getUserById(userId: String): User? {
        return database.getUserData(userId)
    }
}