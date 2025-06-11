package repository

import data.model.*
import firebase.database.FirebaseDatabaseInterface
import kotlinx.serialization.builtins.ListSerializer

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
                "tournamentApplications",
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
            database.createDocument("tournamentApplications", application)
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun updateApplicationStatus(applicationId: String, status: ApplicationStatus): Boolean {
        return try {
            // Get the application first
            val application = database.getDocument<TeamApplication>("tournamentApplications/$applicationId")
                ?: return false

            // Update application status
            val statusUpdateSuccess = database.updateFields(
                "tournamentApplications",
                applicationId,
                mapOf("status" to status)
            )

            // If approved, add team to tournament
            if (status == ApplicationStatus.APPROVED && statusUpdateSuccess) {
                val tournament = database.getDocument<Tournament>("tournaments/${application.tournamentId}")
                    ?: return false

                // Add team to tournament's teamIds if not already there
                if (!tournament.teamIds.contains(application.teamId)) {
                    val updatedTeamIds = tournament.teamIds + application.teamId

                    // Update the tournament with the new team
                    val tournamentUpdateSuccess = database.updateFields(
                        "tournaments",
                        application.tournamentId,
                        mapOf("teamIds" to updatedTeamIds)
                    )

                    // Check if tournament is now full and auto-generation is enabled
                    // For now, we'll only auto-generate matches if the tournament is exactly full
                    if (tournamentUpdateSuccess && updatedTeamIds.size == tournament.maxTeams) {
                        // Generate matches automatically when full
                        generateMatchesForTournament(application.tournamentId)
                    }

                    return tournamentUpdateSuccess
                }
            }

            return statusUpdateSuccess
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun generateMatchesForTournament(tournamentId: String): Boolean {
        return try {
            val tournament = database.getDocument<Tournament>("tournaments/$tournamentId")
                ?: run {
                    println("Error: Tournament not found for ID $tournamentId")
                    return false
                }

            if (tournament.teamIds.size < 2) {
                println("Error: Not enough teams for tournament ${tournament.id}. Found ${tournament.teamIds.size}, need at least 2.")
                return false // Need at least 2 teams
            }

            // Update tournament status to ACTIVE
            val statusUpdateSuccess = database.updateFields(
                "tournaments",
                tournamentId,
                mapOf("status" to TournamentStatus.ACTIVE.name)
            )

            if (!statusUpdateSuccess) {
                println("Error: Failed to update tournament status to ACTIVE for tournamentId: $tournamentId")
                return false // Status update failed
            }

            // Replace the problematic when expression with this:
            val matchesGeneratedSuccessfully: Boolean = when (tournament.bracketType) {
                BracketType.SINGLE_ELIMINATION -> generateSingleEliminationMatches(tournament)
                BracketType.DOUBLE_ELIMINATION -> generateDoubleEliminationMatches(tournament)
                BracketType.ROUND_ROBIN -> generateRoundRobinMatches(tournament)
            }

            // Now this line will work correctly
            if (!matchesGeneratedSuccessfully) {
                println("Error: Failed to generate matches for tournament ${tournament.id} (type: ${tournament.bracketType}). The specific match generation function returned false.")
                // Optional: Consider reverting tournament status if match generation fails.
                return false
            }
            return true // If we reach here, matches were generated successfully

        } catch (e: Exception) {
            println("Exception in FirebaseTournamentRepository.generateMatchesForTournament for ID $tournamentId: ${e.message}")
            e.printStackTrace() // Log the full stack trace for debugging
            return false
        }
    }

    private suspend fun generateSingleEliminationMatches(tournament: Tournament): Boolean {
        try {
            val teams = tournament.teamIds.shuffled()

            // Calculate number of rounds needed
            val numRounds = kotlin.math.ceil(kotlin.math.log2(tournament.maxTeams.toDouble())).toInt()

            // First round matches
            val numFirstRoundMatches = teams.size / 2

            for (i in 0 until numFirstRoundMatches) {
                val homeTeamId = teams[i * 2]
                val awayTeamId = if (i * 2 + 1 < teams.size) teams[i * 2 + 1] else ""

                val matchDate = calculateMatchDate(tournament.startDate, 0, i)

                val match = TournamentMatch(
                    tournamentId = tournament.id,
                    round = 1,
                    matchNumber = i + 1,
                    homeTeamId = homeTeamId,
                    awayTeamId = awayTeamId,
                    scheduledDate = matchDate,
                    status = MatchStatus.SCHEDULED
                )

                val matchId = database.createDocument("tournamentMatches", match)
                if (matchId.isEmpty()) {
                    println("Failed to create first round match")
                    return false
                }
            }

            // Create placeholder matches for subsequent rounds
            for (round in 2..numRounds) {
                val matchesInRound = tournament.maxTeams / (1 shl round)
                for (i in 0 until matchesInRound) {
                    val matchDate = calculateMatchDate(tournament.startDate, round - 1, i)

                    val match = TournamentMatch(
                        tournamentId = tournament.id,
                        round = round,
                        matchNumber = i + 1,
                        homeTeamId = "",
                        awayTeamId = "",
                        scheduledDate = matchDate,
                        status = MatchStatus.SCHEDULED
                    )

                    val matchId = database.createDocument("tournamentMatches", match)
                    if (matchId.isEmpty()) {
                        println("Failed to create placeholder match for round $round")
                        return false
                    }
                }
            }
            return true
        } catch (e: Exception) {
            println("Error in generateSingleEliminationMatches: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private suspend fun generateDoubleEliminationMatches(tournament: Tournament): Boolean {
        try {
            val teamIds = tournament.teamIds.shuffled()

            if (teamIds.size < 2) {
                println("Error: Not enough teams for double elimination bracket. Found ${teamIds.size}, need at least 2.")
                return false
            }

            // Parse start date for scheduling matches
            val startDate = tournament.startDate.split("-").map { it.toInt() }

            // Initialize lists for winners and losers brackets
            val matches = mutableListOf<TournamentMatch>()
            var matchCounter = 1
            var roundCounter = 1

            // First round of winners bracket
            val firstRoundPairs = teamIds.chunked(2)
            for ((index, pair) in firstRoundPairs.withIndex()) {
                if (pair.size == 2) {
                    val matchId = "${tournament.id}-W-R${roundCounter}-M${matchCounter}"
                    val match = TournamentMatch(
                        id = matchId,
                        tournamentId = tournament.id,
                        round = roundCounter,
                        matchNumber = matchCounter,
                        homeTeamId = pair[0],
                        awayTeamId = pair[1],
                        scheduledDate = calculateMatchDate(startDate, roundCounter - 1),
                        status = MatchStatus.SCHEDULED
                    )
                    matches.add(match)
                    matchCounter++
                }
            }

            // Calculate how many rounds we'll need
            val totalRounds = calculateTotalRoundsNeeded(teamIds.size)

            // Create placeholders for future winners bracket rounds
            for (round in 2..totalRounds) {
                val matchesInRound = 1 shl (totalRounds - round)
                for (match in 1..matchesInRound) {
                    val matchId = "${tournament.id}-W-R$round-M${matchCounter}"
                    val scheduledDate = calculateMatchDate(startDate, round - 1)
                    matches.add(
                        TournamentMatch(
                            id = matchId,
                            tournamentId = tournament.id,
                            round = round,
                            matchNumber = matchCounter,
                            scheduledDate = scheduledDate,
                            status = MatchStatus.SCHEDULED
                        )
                    )
                    matchCounter++
                }
            }

            // Create losers bracket matches (will need refinement)
            val losersRounds = totalRounds * 2 - 1
            var losersRoundCounter = 1
            var losersMatchCounter = matchCounter

            for (round in 1..losersRounds) {
                val matchesInRound = calculateLosersMatchesInRound(round, teamIds.size)
                if (matchesInRound > 0) {
                    for (match in 1..matchesInRound) {
                        val matchId = "${tournament.id}-L-R$losersRoundCounter-M${losersMatchCounter}"
                        val scheduledDate = calculateMatchDate(startDate, losersRoundCounter + totalRounds - 1)
                        matches.add(
                            TournamentMatch(
                                id = matchId,
                                tournamentId = tournament.id,
                                round = losersRoundCounter + totalRounds,  // Offset round number
                                matchNumber = losersMatchCounter,
                                scheduledDate = scheduledDate,
                                status = MatchStatus.SCHEDULED
                            )
                        )
                        losersMatchCounter++
                    }
                    losersRoundCounter++
                }
            }

            // Final championship match (winner of winners bracket vs winner of losers bracket)
            val finalMatchId = "${tournament.id}-F-R${totalRounds + losersRoundCounter}-M${losersMatchCounter}"
            matches.add(
                TournamentMatch(
                    id = finalMatchId,
                    tournamentId = tournament.id,
                    round = totalRounds + losersRoundCounter,
                    matchNumber = losersMatchCounter,
                    scheduledDate = calculateMatchDate(startDate, totalRounds + losersRoundCounter - 1),
                    status = MatchStatus.SCHEDULED
                )
            )

            // Create all matches in the database
            for (match in matches) {
                val matchId = database.createDocument("tournamentMatches", match)
                if (matchId.isEmpty()) {
                    println("Error: Failed to create match ${match.id} for tournament ${tournament.id}")
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            println("Error generating double elimination matches: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private suspend fun generateRoundRobinMatches(tournament: Tournament): Boolean {
        try {
            val teamIds = tournament.teamIds

            if (teamIds.size < 2) {
                println("Error: Not enough teams for round robin tournament. Found ${teamIds.size}, need at least 2.")
                return false
            }

            // Parse start date for scheduling
            val startDate = tournament.startDate.split("-").map { it.toInt() }

            // Teams play against every other team once
            val matches = mutableListOf<TournamentMatch>()
            var matchCounter = 1

            // If odd number of teams, add a "bye" placeholder
            val teamsForScheduling = if (teamIds.size % 2 == 1) {
                teamIds + "bye"
            } else {
                teamIds
            }

            // Round Robin algorithm (Circle method)
            val n = teamsForScheduling.size
            val rounds = n - 1
            val half = n / 2

            // Create fixed and rotating lists
            val fixed = teamsForScheduling.first()
            val rotating = teamsForScheduling.drop(1).toMutableList()

            for (round in 0 until rounds) {
                // Schedule matches for this round
                for (i in 0 until half) {
                    val team1 = if (i == 0) fixed else rotating[i - 1]
                    val team2 = rotating[rotating.size - i]

                    // Skip "bye" matches
                    if (team1 != "bye" && team2 != "bye") {
                        val matchId = "${tournament.id}-RR-R${round + 1}-M$matchCounter"
                        val match = TournamentMatch(
                            id = matchId,
                            tournamentId = tournament.id,
                            round = round + 1,
                            matchNumber = matchCounter,
                            homeTeamId = team1,
                            awayTeamId = team2,
                            scheduledDate = calculateMatchDate(startDate, round),
                            status = MatchStatus.SCHEDULED
                        )
                        matches.add(match)
                        matchCounter++
                    }
                }

                // Rotate teams: first team is fixed, others rotate clockwise
                rotating.add(0, rotating.removeAt(rotating.size - 1))
            }

            // Create all matches in the database
            for (match in matches) {
                val matchId = database.createDocument("tournamentMatches", match)
                if (matchId.isEmpty()) {
                    println("Error: Failed to create match ${match.id} for tournament ${tournament.id}")
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            println("Error generating round robin matches: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    override suspend fun reportMatchResult(
        matchId: String,
        teamId: String,
        isHomeTeam: Boolean,
        reportedByUserId: String,
        homeScore: Int,
        awayScore: Int
    ): Boolean {
        return try {
            // Get current match data
            val match = database.getDocument<TournamentMatch>("tournamentMatches/$matchId")
                ?: return false

            val updatedMatch = if (isHomeTeam) {
                // Home team is reporting
                if (match.awayTeamConfirmed &&
                    match.homeTeamReportedScore == homeScore &&
                    match.awayTeamReportedScore == awayScore) {
                    // Both teams reported matching scores, mark as completed
                    match.copy(
                        homeTeamConfirmed = true,
                        homeTeamReportedScore = homeScore,
                        awayTeamReportedScore = awayScore,
                        homeTeamReporterId = reportedByUserId,
                        status = MatchStatus.COMPLETED,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        winnerId = when {
                            homeScore > awayScore -> match.homeTeamId
                            awayScore > homeScore -> match.awayTeamId
                            else -> "" // Draw
                        }
                    )
                } else {
                    // Home team report, awaiting away team confirmation or there's a mismatch
                    match.copy(
                        homeTeamConfirmed = true,
                        homeTeamReportedScore = homeScore,
                        awayTeamReportedScore = awayScore,
                        homeTeamReporterId = reportedByUserId,
                        status = MatchStatus.IN_PROGRESS
                    )
                }
            } else {
                // Away team is reporting
                if (match.homeTeamConfirmed &&
                    match.homeTeamReportedScore == homeScore &&
                    match.awayTeamReportedScore == awayScore) {
                    // Both teams reported matching scores, mark as completed
                    match.copy(
                        awayTeamConfirmed = true,
                        homeTeamReportedScore = homeScore,
                        awayTeamReportedScore = awayScore,
                        awayTeamReporterId = reportedByUserId,
                        status = MatchStatus.COMPLETED,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        winnerId = when {
                            homeScore > awayScore -> match.homeTeamId
                            awayScore > homeScore -> match.awayTeamId
                            else -> "" // Draw
                        }
                    )
                } else {
                    // Away team report, awaiting home team confirmation or there's a mismatch
                    match.copy(
                        awayTeamConfirmed = true,
                        homeTeamReportedScore = homeScore,
                        awayTeamReportedScore = awayScore,
                        awayTeamReporterId = reportedByUserId,
                        status = MatchStatus.IN_PROGRESS
                    )
                }
            }

            val updated = database.updateDocument("tournamentMatches", matchId, updatedMatch)

            // If match is completed and the update was successful, update the next round match
            if (updated && updatedMatch.status == MatchStatus.COMPLETED) {
                updateNextRoundMatch(updatedMatch)
            }

            updated
        } catch (e: Exception) {
            println("Error reporting match result: ${e.message}")
            false
        }
    }

    private suspend fun updateNextRoundMatch(completedMatch: TournamentMatch) {
        try {
            // Only relevant for elimination brackets
            // For round robin, there's no advancement logic
            val tournament = database.getDocument<Tournament>("tournaments/${completedMatch.tournamentId}")
                ?: return

            if (tournament.bracketType == BracketType.ROUND_ROBIN) {
                return
            }

            // Find the next round match where this team should advance
            val nextRound = completedMatch.round + 1
            val matchesInNextRound = database.getCollectionFiltered<TournamentMatch>(
                "tournamentMatches",
                "tournamentId",
                completedMatch.tournamentId,
                serializer = ListSerializer(TournamentMatch.serializer())
            ).filter { it.round == nextRound }

            if (matchesInNextRound.isEmpty()) return

            // Determine winner of the completed match
            val winningTeamId = if (completedMatch.homeScore!! > completedMatch.awayScore!!) {
                completedMatch.homeTeamId
            } else if (completedMatch.awayScore > completedMatch.homeScore!!) {
                completedMatch.awayTeamId
            } else {
                // Handle draws - for simplicity, we'll advance the home team in case of a draw
                // In a real application, you might implement tiebreakers or other rules
                completedMatch.homeTeamId
            }

            // For single elimination, find the next match based on match number
            // This is a simplified approach and might need refinement for specific bracket structures
            val currentPosition = completedMatch.matchNumber
            val nextMatchNumber = (currentPosition + 1) / 2

            val nextMatch = matchesInNextRound.find { it.matchNumber == nextMatchNumber }
                ?: return

            // Add winning team to the appropriate spot in the next match
            val isHomeTeam = currentPosition % 2 == 1

            val updatedNextMatch = if (isHomeTeam) {
                nextMatch.copy(homeTeamId = winningTeamId)
            } else {
                nextMatch.copy(awayTeamId = winningTeamId)
            }

            database.updateDocument("tournamentMatches", nextMatch.id, updatedNextMatch)

        } catch (e: Exception) {
            println("Error updating next round match: ${e.message}")
        }
    }

    /**
     * Retrieves a tournament match by its ID
     */
    override suspend fun getMatchById(matchId: String): TournamentMatch? {
        return try {
            // Use explicit path to get the match document to avoid type confusion
            database.getDocument<TournamentMatch>("tournamentMatches/$matchId")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Helper functions for tournament match generation
    private fun calculateTotalRoundsNeeded(teamCount: Int): Int {
        // For 2 teams, need 1 round; 3-4 teams, need 2 rounds; 5-8 teams, need 3 rounds, etc.
        return kotlin.math.ceil(kotlin.math.log2(teamCount.toDouble())).toInt()
    }

    private fun calculateLosersMatchesInRound(round: Int, teamCount: Int): Int {
        // Simplified calculation of losers bracket matches
        // This is a basic approximation and would need refinement for a production system
        val totalRounds = calculateTotalRoundsNeeded(teamCount)
        return when {
            round <= totalRounds -> (1 shl (totalRounds - round)) / 2
            else -> 0
        }
    }

    private fun calculateMatchDate(startDate: List<Int>, roundOffset: Int): String {
        // Simple date calculation - add days based on round
        val newDay = startDate[2] + (roundOffset * 2)
        return "${startDate[0]}-${startDate[1]}-$newDay"
    }

    private fun calculateMatchDate(startDateStr: String, roundOffset: Int, matchNumber: Int): String {
        try {
            val startDate = startDateStr.split("-").map { it.toInt() }
            val newDay = startDate[2] + (roundOffset * 2)
            return "${startDate[0]}-${startDate[1]}-$newDay"
        } catch (e: Exception) {
            return startDateStr
        }
    }
}