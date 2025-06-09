package repository

import data.model.Tournament
import data.model.TournamentMatch
import data.model.TeamApplication
import data.model.ApplicationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TournamentRepository {
    suspend fun getAllTournaments(): List<Tournament>
    suspend fun getTournamentById(id: String): Tournament?
    suspend fun createTournament(tournament: Tournament): String
    suspend fun updateTournament(tournament: Tournament): Boolean
    suspend fun deleteTournament(id: String): Boolean
    suspend fun getTournamentMatches(tournamentId: String): List<TournamentMatch>
    suspend fun createMatch(match: TournamentMatch): String
    suspend fun updateMatch(match: TournamentMatch): Boolean
    suspend fun getTeamApplications(tournamentId: String): List<TeamApplication>
    suspend fun applyTeamToTournament(application: TeamApplication): String
    suspend fun updateApplicationStatus(applicationId: String, status: ApplicationStatus): Boolean
}