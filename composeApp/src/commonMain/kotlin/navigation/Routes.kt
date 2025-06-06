package navigation

import kotlinx.serialization.Serializable

@Serializable
data object Login

@Serializable
data object Register

@Serializable
data object Home

@Serializable
data class Search(val filter: String = "")

@Serializable
data object Team

@Serializable
data object CreateTeam

@Serializable
data object Profile

@Serializable
data object Settings

@Serializable
data class PostDetail(val postId: String)

@Serializable
data class EmailVerification(val email: String? = null)
