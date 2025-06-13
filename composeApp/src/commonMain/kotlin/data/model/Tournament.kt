package data.model

import kotlinx.serialization.Serializable

@Serializable
data class Tournament(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorType: CreatorType = CreatorType.ADMIN,
    val startDate: String = "",
    val endDate: String = "",
    val status: TournamentStatus = TournamentStatus.UPCOMING,
    val teamIds: List<String> = emptyList(),
    val maxTeams: Int = 8,
    val bracketType: BracketType = BracketType.SINGLE_ELIMINATION,
    val winnerId: String = "",
    val completedDate: String = ""

)

@Serializable
enum class CreatorType {
    ADMIN, TEAM_PRESIDENT
}

@Serializable
enum class TournamentStatus {
    UPCOMING, REGISTRATION, ACTIVE, COMPLETED, CANCELLED
}

@Serializable
enum class BracketType {
    SINGLE_ELIMINATION, DOUBLE_ELIMINATION, ROUND_ROBIN
}

@Serializable
data class TournamentMatch(
    val id: String = "",
    val tournamentId: String = "",
    val round: Int = 0,
    val matchNumber: Int = 0,
    val homeTeamId: String = "",
    val awayTeamId: String = "",
    val scheduledDate: String = "",
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val winnerId: String = "",
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val homeTeamScore: Int = homeScore ?: 0,
    val awayTeamScore: Int = awayScore ?: 0,

    val homeTeamConfirmed: Boolean = false,
    val awayTeamConfirmed: Boolean = false,
    val homeTeamReportedScore: Int? = null,
    val awayTeamReportedScore: Int? = null,
    val homeTeamReporterId: String? = null,
    val awayTeamReporterId: String? = null

)

@Serializable
enum class MatchStatus {
    SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
}

@Serializable
data class TeamApplication(
    val id: String = "",
    val teamId: String = "",
    val tournamentId: String = "",
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: String = ""
)

@Serializable
enum class ApplicationStatus {
    PENDING, APPROVED, REJECTED
}