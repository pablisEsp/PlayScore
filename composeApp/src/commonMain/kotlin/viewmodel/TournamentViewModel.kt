package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.ApplicationStatus
import data.model.BracketType
import data.model.MatchStatus
import data.model.Team
import data.model.TeamApplication
import data.model.Tournament
import data.model.TournamentMatch
import data.model.TournamentStatus
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import org.koin.compose.koinInject
import repository.TournamentRepository

class TournamentViewModel(
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface
) : ViewModel() {

    // Tournament states
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

    private var allTournamentsCache: List<Tournament>? = null

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

    // Get tournament standings - for active or completed tournaments
    fun loadTournamentStandings(tournamentId: String) {
        viewModelScope.launch {
        // Placeholder
    }
}

    // Get tournament schedule/matches - for active tournaments
    fun loadTournamentSchedule(tournamentId: String) {
        viewModelScope.launch {
        // Placeholder
    }
}

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}