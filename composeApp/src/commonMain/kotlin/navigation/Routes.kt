package navigation

import kotlinx.serialization.Serializable

@Serializable
data object Login

@Serializable
data object Register

@Serializable
data object Home

@Serializable
data object Search

@Serializable
data object Team

@Serializable
data class TeamManagement(val teamId: String)

@Serializable
data object CreateTeam

@Serializable
data object Profile

@Serializable
data object Settings

@Serializable
data class PostDetail(val postId: String)