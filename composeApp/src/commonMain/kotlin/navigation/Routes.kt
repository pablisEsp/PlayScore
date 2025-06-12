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

@Serializable
data object ForgotPassword

@Serializable
data object TeamTournaments

@Serializable
data class TeamTournamentDetail(val tournamentId: String)

@Serializable
data object TournamentManagement

@Serializable
data class EditTournament(val tournamentId: String)

@Serializable
data class TournamentApplications(val tournamentId: String)

@Serializable
data class TournamentBracket(val tournamentId: String)

@Serializable
data object CreateTournament

@Serializable
data object AdminPanel

@Serializable
data object UserManagement

@Serializable
data object ReportedPosts

@Serializable
data object ChangePassword

@Serializable
data object AccountSettings

@Serializable
data class EditTeam(val teamId: String)