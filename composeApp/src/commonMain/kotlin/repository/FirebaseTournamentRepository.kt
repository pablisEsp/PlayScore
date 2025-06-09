package repository

import data.model.*
import firebase.database.FirebaseDatabaseInterface
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer

class FirebaseTournamentRepository(
    private val database: FirebaseDatabaseInterface
) : TournamentRepository {

    override suspend fun getAllTournaments(): List<Tournament> {
        return try {
            database.getCollection("tournaments", ListSerializer(Tournament.serializer()))
        } catch (e: Exception) {
            // The logged error suggests the exception occurs within database.getCollection
            emptyList()
        }
    }

    override suspend fun getTournamentById(id: String): Tournament? {
        return try {
            database.getDocument<Tournament>("tournaments/$id")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createTournament(tournament: Tournament): String {
        return try {
            database.createDocument("tournaments", tournament)
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun updateTournament(tournament: Tournament): Boolean {
        return try {
            database.updateDocument("tournaments", tournament.id, tournament)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteTournament(id: String): Boolean {
        return try {
            database.deleteDocument("tournaments", id)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getTournamentMatches(tournamentId: String): List<TournamentMatch> {
        return try {
            database.getCollectionFiltered(
                "tournamentMatches",
                "tournamentId",
                tournamentId,
                ListSerializer(TournamentMatch.serializer())
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createMatch(match: TournamentMatch): String {
        return try {
            database.createDocument("tournamentMatches", match)
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun updateMatch(match: TournamentMatch): Boolean {
        return try {
            database.updateDocument("tournamentMatches", match.id, match)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getTeamApplications(tournamentId: String): List<TeamApplication> {
        return try {
            database.getCollectionFiltered(
                "teamApplications",
                "tournamentId",
                tournamentId,
                ListSerializer(TeamApplication.serializer())
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun applyTeamToTournament(application: TeamApplication): String {
        return try {
            database.createDocument("teamApplications", application)
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun updateApplicationStatus(applicationId: String, status: ApplicationStatus): Boolean {
        return try {
            database.updateFields(
                "teamApplications",
                applicationId,
                mapOf("status" to status)
            )
        } catch (e: Exception) {
            false
        }
    }
}