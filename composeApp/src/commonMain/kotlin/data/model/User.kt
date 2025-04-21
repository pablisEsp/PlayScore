package data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val globalRole: UserRole = UserRole.USER,
    val teamMembership: TeamMembership? = null,
    val profileImage: String = "",
    val stats: UserStats = UserStats(),
    val createdAt: String = ""
)

@Serializable
enum class UserRole {
    USER, ADMIN, SUPER_ADMIN
}

@Serializable
enum class TeamRole {
    PLAYER, CAPTAIN, VICE_PRESIDENT, PRESIDENT
}

@Serializable
data class TeamMembership(
    val teamId: String? = null,
    val role: TeamRole? = null
)

@Serializable
data class UserStats(
    val matchesPlayed: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val mvps: Int = 0,
    val averageRating: Double = 0.0
)
