package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.ApplicationStatus
import data.model.MatchStatus
import data.model.Team
import data.model.TeamApplication
import data.model.TeamRole
import data.model.Tournament
import data.model.TournamentMatch
import data.model.TournamentStatus
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import repository.TournamentRepository
import repository.UserRepository

class TournamentViewModel(
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface,
    private val userRepository: UserRepository,
    private val tournamentRepository: TournamentRepository,
) : ViewModel() {

    // Tournament states
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    val _currentTournament = MutableStateFlow<Tournament?>(null)
    val currentTournament: StateFlow<Tournament?> = _currentTournament.asStateFlow()

    private val _availableTournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val availableTournaments: StateFlow<List<Tournament>> = _availableTournaments.asStateFlow()

    private val _teamTournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val teamTournaments: StateFlow<List<Tournament>> = _teamTournaments.asStateFlow()

    // Application states
    private val _teamApplication = MutableStateFlow<TeamApplication?>(null)
    val teamApplication: StateFlow<TeamApplication?> = _teamApplication.asStateFlow()

    private val _teamApplications = MutableStateFlow<List<TeamApplication>>(emptyList())
    val teamApplications: StateFlow<List<TeamApplication>> = _teamApplications.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _tournamentMatches = MutableStateFlow<List<TournamentMatch>>(emptyList())
    val tournamentMatches: StateFlow<List<TournamentMatch>> = _tournamentMatches.asStateFlow()

    private val _teamNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val teamNames: StateFlow<Map<String, String>> = _teamNames.asStateFlow()

    private val _userCanReportScore = MutableStateFlow(false)
    val userCanReportScore: StateFlow<Boolean> = _userCanReportScore

    private var allTournamentsCache: List<Tournament>? = null

    // Load the current user when needed
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val authUser = auth.getCurrentUser()
                if (authUser != null) {
                    val userData = database.getUserData(authUser.uid)
                    _currentUser.value = userData
                } else {
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                _errorMessage.update { "Failed to load user: ${e.message}" }
                _currentUser.value = null
            }
        }
    }

    fun getCurrentUserData() {
        viewModelScope.launch {
            loadCurrentUser()
        }
    }
    // Get a tournament by ID
    fun getTournamentById(tournamentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null // Clear previous error
            try {
                // Fetch all tournaments and find the specific one by ID
                val allTournaments = database.getCollection<Tournament>(
                    "tournaments",
                    serializer = ListSerializer(Tournament.serializer()) // Use ListSerializer
                )
                allTournamentsCache = allTournaments // Update the cache
                val tournament = allTournaments.find { it.id == tournamentId }

                if (tournament != null) {
                    _currentTournament.value = tournament
                } else {
                    _errorMessage.value = "Tournament not found"
                    _currentTournament.value = null // Clear current tournament if not found
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load tournament: ${e.message}"
                _currentTournament.value = null // Clear current tournament on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper method to update team statistics
    private suspend fun updateTeamStats(teamId: String, isWin: Boolean, isTie: Boolean, pointsToAdd: Int) {
        // Get current team data
        val team = database.getDocument<Team>("teams/$teamId") ?: return

        // Calculate new stats
        val newWins = team.totalWins + if (isWin) 1 else 0
        val newLosses = team.totalLosses + if (!isWin && !isTie) 1 else 0
        val newPoints = team.pointsTotal + pointsToAdd

        // Update team in database
        database.updateFields(
            "teams",
            teamId,
            mapOf(
                "totalWins" to newWins,
                "totalLosses" to newLosses,
                "pointsTotal" to newPoints
            )
        )
    }

    // Calculate points based on match result (can be customized based on tournament rules)
    private fun calculatePoints(isWin: Boolean, isTie: Boolean): Int {
        return when {
            isWin -> 3  // Win = 3 points
            isTie -> 1  // Tie = 1 point
            else -> 0   // Loss = 0 points
        }
    }

    // Complete the checkTournamentCompletion function in TournamentViewModel
    fun checkTournamentCompletion(tournamentId: String) {
        viewModelScope.launch {
            try {
                // Get all matches for this tournament
                val matches = _tournamentMatches.value.ifEmpty {
                    // Load matches if not already loaded
                    database.getCollectionFiltered<TournamentMatch>(
                        "tournamentMatches",
                        "tournamentId",
                        tournamentId,
                        serializer = ListSerializer(TournamentMatch.serializer())
                    )
                }

                // Get the tournament
                val tournament = _currentTournament.value ?:
                    database.getDocument<Tournament>("tournaments/$tournamentId")

                // Find the final match (highest round, usually only one match)
                val maxRound = matches.maxOfOrNull { it.round } ?: return@launch
                val finalMatches = matches.filter { it.round == maxRound }

                // For single elimination, there should be exactly one final match
                val finalMatch = finalMatches.firstOrNull() ?: return@launch

                // If the final match is completed and has a winner
                if (finalMatch.status == MatchStatus.COMPLETED && finalMatch.winnerId.isNotEmpty()) {
                    // Update tournament as completed with winner
                    val currentDate = Clock.System.now().toString().substringBefore(".")

                    database.updateFields(
                        "tournaments",
                        tournamentId,
                        mapOf(
                            "status" to TournamentStatus.COMPLETED.toString(),
                            "winnerId" to finalMatch.winnerId,
                            "completedDate" to currentDate
                        )
                    )

                    // Update local tournament data
                    _currentTournament.update { tournament ->
                        tournament?.copy(
                            status = TournamentStatus.COMPLETED,
                            winnerId = finalMatch.winnerId,
                            completedDate = currentDate
                        )
                    }

                    _successMessage.value = "Tournament completed! Winner determined."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error checking tournament completion: ${e.message}"
            }
        }
    }

    // Modify advanceTeamToNextPhase to check for tournament completion
    fun advanceTeamToNextPhase(match: TournamentMatch) {
        viewModelScope.launch {
            try {
                // Only proceed if match is completed and has scores
                if (match.status != MatchStatus.COMPLETED || match.homeScore == null || match.awayScore == null) {
                    return@launch
                }

                // Determine the winner of this match
                val winningTeamId = if (match.homeScore > match.awayScore) {
                    match.homeTeamId
                } else if (match.homeScore < match.awayScore) {
                    match.awayTeamId
                } else {
                    // In case of a tie, implement your tiebreaker rule
                    // For now, default to home team
                    match.homeTeamId
                }

                // Calculate the next phase details
                val nextPhase = match.round + 1
                val nextMatchNumber = (match.matchNumber + 1) / 2

                // Find the next match this winner should advance to
                val nextPhaseMatch = _tournamentMatches.value.find {
                    it.round == nextPhase && it.matchNumber == nextMatchNumber
                }

                if (nextPhaseMatch == null) {
                    // No next match means this might be the final match
                    // Update the current match with the winner and check if tournament is complete
                    database.updateFields(
                        "tournamentMatches",
                        match.id,
                        mapOf("winnerId" to winningTeamId)
                    )

                    // Check if this was the final match, and if so, complete the tournament
                    checkTournamentCompletion(match.tournamentId)
                    return@launch
                }

                // Determine if winner goes to home or away position
                // Odd-numbered matches fill home team slots, even-numbered matches fill away slots
                val updatedFields = if (match.matchNumber % 2 != 0) {
                    mapOf("homeTeamId" to winningTeamId)
                } else {
                    mapOf("awayTeamId" to winningTeamId)
                }

                // Also store the winner ID in the current match
                database.updateFields(
                    "tournamentMatches",
                    match.id,
                    mapOf("winnerId" to winningTeamId)
                )

                // Update the next phase match with the winner
                database.updateFields(
                    "tournamentMatches",
                    nextPhaseMatch.id,
                    updatedFields
                )

                // Refresh tournament matches to show the update
                _currentTournament.value?.id?.let { tournamentId ->
                    loadTournamentMatches(tournamentId)
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error advancing team: ${e.message}"
            }
        }
    }

    // Modify your finalizeMatchResult method to call advanceTeamToNextPhase
    fun finalizeMatchResult(match: TournamentMatch) {
        viewModelScope.launch {
            try {
                // Only process if both teams have confirmed the same score
                if (match.homeTeamConfirmed && match.awayTeamConfirmed &&
                    match.homeScore != null && match.awayScore != null &&
                    match.status == MatchStatus.IN_PROGRESS) {

                    // Determine winner and loser
                    val homeTeamWon = match.homeScore > match.awayScore
                    val isTie = match.homeScore == match.awayScore

                    // Update home team stats
                    updateTeamStats(
                        teamId = match.homeTeamId,
                        isWin = homeTeamWon,
                        isTie = isTie,
                        pointsToAdd = calculatePoints(homeTeamWon, isTie)
                    )

                    // Update away team stats
                    updateTeamStats(
                        teamId = match.awayTeamId,
                        isWin = !homeTeamWon,
                        isTie = isTie,
                        pointsToAdd = calculatePoints(!homeTeamWon, isTie)
                    )

                    // Update match status to COMPLETED
                    database.updateFields(
                        "tournamentMatches",
                        match.id,
                        mapOf("status" to MatchStatus.COMPLETED)
                    )

                    // If not a tie, advance the winning team to the next phase
                    if (!isTie) {
                        // Pass a copy of the match with updated status
                        advanceTeamToNextPhase(match.copy(status = MatchStatus.COMPLETED))
                    } else {
                        // Handle tie scenario based on tournament rules
                        _errorMessage.value = "Match ended in a tie. Please consult tournament rules for tiebreaker."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error finalizing match: ${e.message}"
            }
        }
    }

    // Load available tournaments - tournaments with open registration that the team has not already joined
    fun loadAvailableTournaments(teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First, get all applications for this team
                val teamApplications = database.getCollectionFiltered<TeamApplication>(
                    "tournamentApplications",
                    "teamId",
                    teamId,
                    serializer = kotlinx.serialization.builtins.ListSerializer(TeamApplication.serializer())
                )

                // Extract the tournament IDs that this team has already applied to
                val appliedTournamentIds = teamApplications.map { it.tournamentId }.toSet()

                // Get all tournaments
                val allTournaments = database.getCollection<Tournament>(
                    "tournaments",
                    serializer = kotlinx.serialization.builtins.ListSerializer(Tournament.serializer())
                )
                allTournamentsCache = allTournaments // Update the cache

                // Filter tournaments that:
                // 1. Are open for registration
                // 2. Team isn't already part of
                // 3. Team hasn't already applied to
                _availableTournaments.value = allTournaments.filter { tournament ->
                    tournament.status == TournamentStatus.REGISTRATION &&
                    !tournament.teamIds.contains(teamId) &&
                    !appliedTournamentIds.contains(tournament.id)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load tournaments: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Modify the reportMatchScore method in TournamentViewModel to call finalizeMatchResult
    fun reportMatchScore(matchId: String, homeScore: Int, awayScore: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Check user authentication
                val currentUserId = auth.getCurrentUser()?.uid ?: run {
                    _errorMessage.value = "You must be logged in to report scores"
                    return@launch
                }

                // 2. Get current user data (ensure it's loaded)
                if (_currentUser.value == null) {
                    loadCurrentUser()
                    delay(500) // Brief delay to allow loading
                }

                val user = _currentUser.value ?: run {
                    _errorMessage.value = "User data not available"
                    return@launch
                }

                // 3. Check if user belongs to a team and has the appropriate role
                val userTeamId = user.teamMembership?.teamId ?: run {
                    _errorMessage.value = "You are not part of a team"
                    return@launch
                }

                val userRole = user.teamMembership.role
                if (userRole != TeamRole.PRESIDENT && userRole != TeamRole.VICE_PRESIDENT && userRole != TeamRole.CAPTAIN) {
                    _errorMessage.value = "Only team captains and leaders can report scores"
                    return@launch
                }

                // 4. Get match data and validate team membership
                val match = tournamentRepository.getMatchById(matchId) ?: run {
                    _errorMessage.value = "Match not found"
                    return@launch
                }

                // 5. Check if user's team is part of this match
                val isHomeTeam = userTeamId == match.homeTeamId
                val isAwayTeam = userTeamId == match.awayTeamId

                if (!isHomeTeam && !isAwayTeam) {
                    _errorMessage.value = "You are not part of either team in this match"
                    return@launch
                }

                // 6. Basic score validation
                if (homeScore < 0 || awayScore < 0) {
                    _errorMessage.value = "Scores cannot be negative"
                    return@launch
                }

                // 7. Check if this team already reported scores
                if ((isHomeTeam && match.homeTeamConfirmed) || (isAwayTeam && match.awayTeamConfirmed)) {
                    _errorMessage.value = "Your team has already reported a score for this match"
                    return@launch
                }

                // 8. Report score to repository
                val success = tournamentRepository.reportMatchResult(
                    matchId = matchId,
                    teamId = userTeamId,
                    isHomeTeam = isHomeTeam,
                    reportedByUserId = currentUserId,
                    homeScore = homeScore,
                    awayScore = awayScore
                )

                if (success) {
                    _successMessage.value = "Score reported successfully"

                    // 9. Refresh match data to show updated status
                    _currentTournament.value?.id?.let { tournamentId ->
                        loadTournamentMatches(tournamentId)
                    }

                    // 10. Get the updated match to check if both teams have confirmed
                    val updatedMatch = tournamentRepository.getMatchById(matchId)

                    // 11. If both teams have confirmed, finalize the match result
                    if (updatedMatch != null && updatedMatch.homeTeamConfirmed && updatedMatch.awayTeamConfirmed) {
                        finalizeMatchResult(updatedMatch)
                        _successMessage.value = "Score confirmed by both teams. Match completed!"
                    }
                } else {
                    _errorMessage.value = "Failed to report score"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error reporting score: ${e.message}"
                e.printStackTrace() // For debugging
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkUserCanReportScore(tournamentId: String) {
        viewModelScope.launch {
            try {
                // Get current user
                val currentUserId = auth.getCurrentUser()?.uid
                if (currentUserId == null) {
                    _userCanReportScore.value = false
                    return@launch
                }

                // Get user data
                val user = database.getUserData(currentUserId)
                if (user == null) {
                    _userCanReportScore.value = false
                    return@launch
                }

                // Check if user is in a team
                val userTeamId = user.teamMembership?.teamId
                if (userTeamId == null) {
                    _userCanReportScore.value = false
                    return@launch
                }

                // Check if user has appropriate role
                val userRole = user.teamMembership.role
                if (userRole == TeamRole.PRESIDENT || userRole == TeamRole.VICE_PRESIDENT || userRole == TeamRole.CAPTAIN) {
                    _userCanReportScore.value = true
                } else {
                    _userCanReportScore.value = false
                }
            } catch (e: Exception) {
                _userCanReportScore.value = false
            }
        }
    }

    // Load tournaments the team is participating in
    fun loadTeamTournaments(teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allTournaments = database.getCollection<Tournament>(
                    "tournaments",
                    serializer = kotlinx.serialization.builtins.ListSerializer(Tournament.serializer())
                )
                allTournamentsCache = allTournaments // Update the cache

                // Filter tournaments where the team is participating
                _teamTournaments.value = allTournaments.filter { tournament ->
                    tournament.teamIds.contains(teamId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load team tournaments: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Check if team has an application for a tournament
    fun checkTeamApplication(tournamentId: String, teamId: String) {
        viewModelScope.launch {
            try {
                // Query applications by teamId and tournamentId
                val applications = database.getCollectionFiltered<TeamApplication>(
                    "tournamentApplications",
                    "teamId",
                    teamId,
                    serializer = kotlinx.serialization.builtins.ListSerializer(TeamApplication.serializer())
                ).filter { it.tournamentId == tournamentId }

                _teamApplication.value = applications.firstOrNull()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to check application status: ${e.message}"
            }
        }
    }

    // Load all applications for a team
    fun loadTeamApplications(teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val applications = database.getCollectionFiltered<TeamApplication>(
                    "tournamentApplications",
                    "teamId",
                    teamId,
                    serializer = kotlinx.serialization.builtins.ListSerializer(TeamApplication.serializer())
                )
                _teamApplications.value = applications
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load applications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Apply to a tournament
    fun applyToTournament(tournament: Tournament, teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if team already has application
                checkTeamApplication(tournament.id, teamId)
                if (_teamApplication.value != null) {
                    _errorMessage.value = "Your team has already applied to this tournament"
                    return@launch
                }

                // Check if tournament is full
                if (tournament.teamIds.size >= tournament.maxTeams) {
                    _errorMessage.value = "Tournament is already full"
                    return@launch
                }

                // Check if registration is open
                if (tournament.status != TournamentStatus.REGISTRATION) {
                    _errorMessage.value = "Tournament registration is not open"
                    return@launch
                }

                // Create application
                val application = TeamApplication(
                    tournamentId = tournament.id,
                    teamId = teamId,
                    status = ApplicationStatus.PENDING,
                    appliedAt = Clock.System.now().toString()
                )

                val applicationId = database.createDocument("tournamentApplications", application)

                if (applicationId.isNotEmpty()) {
                    _successMessage.value = "Applied to tournament successfully"
                    _teamApplication.value = application.copy(id = applicationId)
                    loadTeamApplications(teamId) // Refresh applications list
                    loadAvailableTournaments(teamId) // Also refresh available tournaments list
                } else {
                    _errorMessage.value = "Failed to apply to tournament"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error applying to tournament: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Withdraw application if it's still pending
    fun withdrawApplication(applicationId: String, teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val application = database.getDocument<TeamApplication>("tournamentApplications/$applicationId")
                    ?: throw Exception("Application not found")

                // Only allow withdrawal if application is still pending
                if (application.status != ApplicationStatus.PENDING) {
                    _errorMessage.value = "Cannot withdraw - application already ${application.status}"
                    return@launch
                }

                val success = database.deleteDocument("tournamentApplications", applicationId)

                if (success) {
                    _successMessage.value = "Application withdrawn successfully"
                    _teamApplication.value = null
                    loadTeamApplications(teamId) // Refresh applications list
                    loadAvailableTournaments(teamId) // Also refresh available tournaments list
                }  else {
                    _errorMessage.value = "Failed to withdraw application"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error withdrawing application: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkTournamentDateOverlap(tournamentToCheck: Tournament): List<Tournament> {
        val overlappingTournaments = mutableListOf<Tournament>()
        // Ensure cache is used, if null, this will result in emptyList, preventing null pointer.
        val currentAllTournaments = allTournamentsCache ?: run {
            // Optionally, you could trigger a load of all tournaments here if the cache is empty,
            // but for now, we'll rely on it being populated by other calls.
            // Or log a warning: println("Warning: allTournamentsCache is null during overlap check.")
            emptyList()
        }


        // Check against tournaments the team is already participating in
        for (existingTournamentInTeam in _teamTournaments.value) {
            if (doDatesOverlap(tournamentToCheck, existingTournamentInTeam)) {
                overlappingTournaments.add(existingTournamentInTeam)
            }
        }

        // Also check against tournaments with pending applications
        val pendingApplications = _teamApplications.value
            .filter { it.status == ApplicationStatus.PENDING }

        for (application in pendingApplications) {
            // Use the cache to find the tournament details
            val pendingTournament = currentAllTournaments.find { it.id == application.tournamentId }
                ?: continue // If not found in cache, skip

            if (doDatesOverlap(tournamentToCheck, pendingTournament)) {
                // Avoid adding the same tournament multiple times if it's both in teamTournaments and pending (edge case)
                if (!overlappingTournaments.any { it.id == pendingTournament.id }) {
                    overlappingTournaments.add(pendingTournament)
                }
            }
        }
        return overlappingTournaments
    }

    private fun doDatesOverlap(tournament1: Tournament, tournament2: Tournament): Boolean {
        // Simple date comparison (assuming format YYYY-MM-DD)
        val start1 = tournament1.startDate
        val end1 = tournament1.endDate
        val start2 = tournament2.startDate
        val end2 = tournament2.endDate

        // Tournaments overlap if one starts before the other ends, and vice-versa
        // !(end1 < start2 || end2 < start1)
        // This means they overlap if (start1 <= end2 && start2 <= end1)
        return start1 <= end2 && start2 <= end1
    }

    // Load tournament matches
    fun loadTournamentMatches(tournamentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val matches = database.getCollectionFiltered<TournamentMatch>(
                    "tournamentMatches",
                    "tournamentId",
                    tournamentId,
                    serializer = kotlinx.serialization.builtins.ListSerializer(TournamentMatch.serializer())
                )
                _tournamentMatches.value = matches
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load tournament matches: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load team names for display
    fun loadTeamNames(tournamentId: String) {
        viewModelScope.launch {
            try {
                val tournament = _currentTournament.value ?: return@launch
                val teamIds = tournament.teamIds

                val teamNamesMap = mutableMapOf<String, String>()

                for (teamId in teamIds) {
                    val team = database.getDocument<Team>("teams/$teamId")
                    team?.let {
                        teamNamesMap[teamId] = it.name
                    }
                }

                _teamNames.value = teamNamesMap
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load team names: ${e.message}"
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}