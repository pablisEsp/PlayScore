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

    private val _teamCreationResult = MutableStateFlow<String?>(null)
    val teamCreationResult = _teamCreationResult.asStateFlow()

    private val _isTeamCreationComplete = MutableStateFlow(false)
    val isTeamCreationComplete = _isTeamCreationComplete.asStateFlow()

    // Team members state
    private val _teamMembers = MutableStateFlow<TeamMembersState>(TeamMembersState.Initial)
    val teamMembers: StateFlow<TeamMembersState> = _teamMembers

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

    fun createTeam(teamName: String, description: String = "") {
        _isLoading.value = true
        _teamCreationResult.value = null
        _isTeamCreationComplete.value = false

        viewModelScope.launch {
            try {
                val currentUserId = auth.getCurrentUser()?.uid
                if (currentUserId == null) {
                    _errorMessage.value = "You must be logged in to create a team"
                    return@launch
                }

                // Check if team name is already taken
                if (!isTeamNameAvailable(teamName)) {
                    _errorMessage.value = "Team name '$teamName' is already taken"
                    return@launch
                }

                // Create new team with current user as president
                val newTeam = Team(
                    name = teamName,
                    description = description,
                    presidentId = currentUserId,
                    playerIds = listOf(currentUserId),
                    createdAt = Clock.System.now().toString()
                    // Other fields remain the same
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

                    // Update user's team membership to null
                    val success = database.updateUserData(currentUserId, updates)
                    if (success) {
                        // Update local state
                        _currentUser.value = _currentUser.value?.copy(teamMembership = teamMembership)
                        getTeamById(teamId) // Load the newly created team
                        _isTeamCreationComplete.value = true
                        _successMessage.value = "Team successfully created"

                        // Navigate to Team screen
                        _navigationEvent.value = TeamNavigationEvent.NavigateToTeam(teamId)
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

    fun resetTeamCreationState() {
        _isTeamCreationComplete.value = false
        _teamCreationResult.value = null
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

    // Function to load team members
    fun loadTeamMembers(team: Team) {
        viewModelScope.launch {
            _teamMembers.value = TeamMembersState.Loading
            println("DEBUG: Loading team members for team ID: ${team.id}")
            println("DEBUG: President ID: ${team.presidentId}")
            println("DEBUG: Vice President ID: ${team.vicePresidentId}")
            println("DEBUG: Captain IDs: ${team.captainIds}")
            println("DEBUG: Player IDs: ${team.playerIds}")


            try {
                // Load president (only if ID is not empty)
                val president = if (team.presidentId.isNotBlank()) {
                    val user = database.getUserData(team.presidentId)
                    println("DEBUG: Fetched president: $user")
                    user
                } else null

                // Load vice president (only if ID is not null or empty)
                val vicePresident = if (!team.vicePresidentId.isNullOrBlank()) {
                    database.getUserData(team.vicePresidentId)
                } else null

                // For captains and players, filter out empty IDs first
                val captainIds = team.captainIds.filter { it.isNotBlank() }
                val playerOnlyIds = team.playerIds
                    .filter { it.isNotBlank() }
                    .filter {
                        it != team.presidentId &&
                                it != team.vicePresidentId &&
                                !captainIds.contains(it)
                    }

                // Fetch captains
                val captains = mutableListOf<User>()
                for (id in captainIds) {
                    database.getUserData(id)?.let { captains.add(it) }
                }

                // Fetch regular players
                val players = mutableListOf<User>()
                for (id in playerOnlyIds) {
                    database.getUserData(id)?.let { players.add(it) }
                }

                _teamMembers.value = TeamMembersState.Success(
                    president = president,
                    vicePresident = vicePresident,
                    captains = captains,
                    players = players
                )
            } catch (e: Exception) {
                _teamMembers.value = TeamMembersState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun leaveTeam() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid
                if (currentUserId == null) {
                    _errorMessage.value = "User not authenticated"
                    return@launch
                }

                val team = _currentTeam.value
                if (team == null) {
                    _errorMessage.value = "No team data available"
                    return@launch
                }

                // Check if user is the only member in the team
                val isOnlyMember = team.playerIds.size == 1 &&
                        team.playerIds.contains(currentUserId) &&
                        team.presidentId == currentUserId &&
                        team.captainIds.isEmpty() &&
                        team.vicePresidentId == null

                // Track if team was deleted for proper navigation later
                var teamWasDeleted = false

                if (isOnlyMember) {
                    // Delete the team if user is the only member
                    val success = database.deleteDocument("teams", team.id)
                    if (!success) {
                        _errorMessage.value = "Failed to delete team"
                        return@launch
                    }
                } else if (team.presidentId == currentUserId) {
                    // User is president but not alone - must transfer presidency
                    val updatedTeam = transferPresidency(team, currentUserId)
                    if (updatedTeam == null) {
                        _errorMessage.value = "Failed to transfer team leadership"
                        return@launch
                    }
                } else {
                    // User is regular member - just remove from team lists
                    val updatedTeam = removeUserFromTeam(team, currentUserId)
                    if (updatedTeam == null) {
                        _errorMessage.value = "Failed to update team"
                        return@launch
                    }
                }

                // Update user's team membership to an empty object instead of null
                val emptyTeamMembership = TeamMembership(
                    teamId = "",
                    role = TeamRole.PLAYER
                )

                val success = database.updateUserData(currentUserId, mapOf("teamMembership" to emptyTeamMembership))
                if (success) {
                    // Update local state
                    _currentUser.value = _currentUser.value?.copy(teamMembership = emptyTeamMembership)
                    _currentTeam.value = null
                    _teamMembers.value = TeamMembersState.Initial
                    _successMessage.value = "Successfully left the team"

                    // Navigate to create team screen if team was deleted
                    if (teamWasDeleted) {
                        _navigationEvent.value = TeamNavigationEvent.NavigateToCreateTeam
                    } else {
                        _navigationEvent.value = TeamNavigationEvent.NavigateToTeam("")
                    }
                } else {
                    _errorMessage.value = "Failed to update user data"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error leaving team: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun transferPresidency(team: Team, currentUserId: String): Team? {
        // Find the next highest role member
        val newPresidentId = when {
            // Vice president gets priority
            !team.vicePresidentId.isNullOrBlank() -> team.vicePresidentId
            // Then captains
            team.captainIds.isNotEmpty() -> team.captainIds.first()
            // Then regular players
            team.playerIds.isNotEmpty() ->
                team.playerIds.firstOrNull { it != currentUserId }
            else -> null
        }

        if (newPresidentId == null) return null

        // Create new player list without the current user
        val newPlayerIds = team.playerIds.filter { it != currentUserId }

        // Create updated team
        val updatedTeam = team.copy(
            presidentId = newPresidentId,
            // If VP becomes president, clear VP role
            vicePresidentId = if (newPresidentId == team.vicePresidentId) null else team.vicePresidentId,
            // If captain becomes president, remove from captains list
            captainIds = if (team.captainIds.contains(newPresidentId))
                team.captainIds.filter { it != newPresidentId }
            else team.captainIds,
            playerIds = newPlayerIds
        )

        val success = database.updateDocument("teams", team.id, updatedTeam)
        return if (success) updatedTeam else null
    }

    private suspend fun removeUserFromTeam(team: Team, currentUserId: String): Team? {
        val updatedTeam = team.copy(
            captainIds = team.captainIds.filter { it != currentUserId },
            playerIds = team.playerIds.filter { it != currentUserId },
            vicePresidentId = if (team.vicePresidentId == currentUserId) null else team.vicePresidentId
        )

        val success = database.updateDocument("teams", team.id, updatedTeam)
        return if (success) updatedTeam else null
    }


    sealed class TeamMembersState {
        object Initial : TeamMembersState()
        object Loading : TeamMembersState()
        data class Success(
            val president: User?,
            val vicePresident: User?,
            val captains: List<User>,
            val players: List<User>
        ) : TeamMembersState()
        data class Error(val message: String) : TeamMembersState()
    }

    sealed class TeamNavigationEvent {
        data class NavigateToTeam(val teamId: String) : TeamNavigationEvent()
        data object NavigateToCreateTeam : TeamNavigationEvent()
        data object None : TeamNavigationEvent()
    }

    private val _navigationEvent = MutableStateFlow<TeamNavigationEvent>(TeamNavigationEvent.None)
    val navigationEvent: StateFlow<TeamNavigationEvent> = _navigationEvent.asStateFlow()

    // function to reset the navigation event
    fun onNavigationEventProcessed() {
        _navigationEvent.value = TeamNavigationEvent.None
    }

    fun setCurrentTeam(team: Team) {
        _currentTeam.value = team
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}