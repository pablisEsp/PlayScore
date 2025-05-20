package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Team
import data.model.TeamRole
import data.model.TeamMembership
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.toString

class TeamViewModel(
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentTeam = MutableStateFlow<Team?>(null)
    val currentTeam: StateFlow<Team?> = _currentTeam.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()


    fun getCurrentUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = auth.getCurrentUser()?.uid
                if (uid != null) {
                    val user = database.getUserData(uid)
                    _currentUser.value = user

                    // If user has a team, fetch team data
                    val teamId = user?.teamMembership?.teamId
                    if (teamId != null) {
                        getTeamById(teamId)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load user data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTeam(teamName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid
                if (currentUserId == null) {
                    _errorMessage.value = "You must be logged in to create a team"
                    return@launch
                }

                // Check if team name is already taken
                if (!isTeamNameAvailable(teamName)) {
                    _errorMessage.value = "Team name '$teamName' is already taken"
                    _isLoading.value = false
                    return@launch
                }

                val currentUser = _currentUser.value ?: database.getUserData(currentUserId)

                if (currentUser == null) {
                    _errorMessage.value = "User data not found"
                    return@launch
                }

                // Create new team with current user as president and the additional fields
                val newTeam = Team(
                    name = teamName,
                    presidentId = currentUserId,
                    playerIds = listOf(currentUserId),
                    createdAt = Clock.System.now().toString(),
                    description = "",
                    logoUrl = "",
                    location = "",
                    ranking = 0,
                    totalWins = 0,
                    totalLosses = 0
                )

                // Save team to database
                val teamId = database.createDocument("teams", newTeam)

                if (teamId.isNotEmpty()) {
                    // Update user's team membership
                    val teamMembership = TeamMembership(
                        teamId = teamId,
                        role = TeamRole.PRESIDENT
                    )

                    val updates = mapOf(
                        "teamMembership" to teamMembership
                    )

                    val success = database.updateUserData(currentUserId, updates)

                    if (success) {
                        _successMessage.value = "Team '$teamName' created successfully!"
                        _navigationEvent.value = TeamNavigationEvent.NavigateToTeam(teamId)
                        // Still refresh data but the navigation will happen
                        getCurrentUserData()
                    } else {
                        _errorMessage.value = "Failed to update user data"
                    }
                } else {
                    _errorMessage.value = "Failed to create team"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error creating team: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun isTeamNameAvailable(teamName: String): Boolean {
        try {
            // Query teams by name to check if this team name already exists
            val existingTeams = database.getCollectionFiltered<Team>("teams", "name", teamName)
            return existingTeams.isEmpty()
        } catch (e: Exception) {
            _errorMessage.value = "Error checking team name: ${e.message}"
            return false
        }
    }

    suspend fun getTeamById(teamId: String) {
        try {
            val team = database.getDocument<Team>("teams/$teamId")
            _currentTeam.value = team
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load team: ${e.message}"
        }
    }

    sealed class TeamNavigationEvent {
        data class NavigateToTeam(val teamId: String) : TeamNavigationEvent()
        data object None : TeamNavigationEvent()
    }

    private val _navigationEvent = MutableStateFlow<TeamNavigationEvent>(TeamNavigationEvent.None)
    val navigationEvent: StateFlow<TeamNavigationEvent> = _navigationEvent.asStateFlow()

    // function to reset the navigation event
    fun onNavigationEventProcessed() {
        _navigationEvent.value = TeamNavigationEvent.None
    }

    fun clearError() {
        _errorMessage.value = null
    }
}