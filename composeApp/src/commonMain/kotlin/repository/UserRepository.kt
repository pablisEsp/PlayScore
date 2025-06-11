package repository

import data.model.User
import data.model.UserRole

interface UserRepository {
    suspend fun getAllUsers(): List<User>
    suspend fun updateUserRole(userId: String, newRole: UserRole): Boolean
    suspend fun toggleUserBan(userId: String, isBanned: Boolean): Boolean
    suspend fun getUserById(userId: String): User?
}