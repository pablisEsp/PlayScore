package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.RequestStatus
import data.model.Team
import data.model.TeamJoinRequest
import data.model.TeamMembership
import data.model.TeamRole
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class TeamViewModel(
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentTeam = MutableStateFlow<Team?>(null)
    val currentTeam: StateFlow<Team?> = _currentTeam.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger.asStateFlow()

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

    // Navigation event for team-related actions
    sealed class TeamNavigationEvent {
        data class NavigateToTeam(val teamId: String = "") : TeamNavigationEvent()
        object NavigateToCreateTeam : TeamNavigationEvent()
        object None : TeamNavigationEvent()
    }

    // Property to your class
    private val _navigationEvent = MutableStateFlow<TeamNavigationEvent>(TeamNavigationEvent.None)
    val navigationEvent: StateFlow<TeamNavigationEvent> = _navigationEvent.asStateFlow()

    // Method to reset navigation after handling
    fun onNavigationEventProcessed() {
        _navigationEvent.value = TeamNavigationEvent.None
    }

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
                        // Delete all pending join requests by this user
                        deleteAllUserPendingRequests(currentUserId)

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
            val existingTeams = database.getCollectionFiltered<Team>(
                path = "teams",
                field = "name",
                value = teamName,
                serializer = kotlinx.serialization.builtins.ListSerializer(Team.serializer())
            )
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

            try {
                // Update the current team in the ViewModel to ensure it's the latest
                _currentTeam.value = team

                // First, get a fresh copy of the team from the database
                val freshTeam = database.getDocument<Team>("teams/${team.id}") ?: team

                // Important: Update the currentTeam value with fresh data
                _currentTeam.value = freshTeam

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

    private val _showPresidentLeaveWarning = MutableStateFlow(false)
    val showPresidentLeaveWarning: StateFlow<Boolean> = _showPresidentLeaveWarning.asStateFlow()

    fun resetLeaveWarning() {
        _showPresidentLeaveWarning.value = false
    }


    fun leaveTeam() {
        viewModelScope.launch {
            val currentUserId = auth.getCurrentUser()?.uid
            val team = _currentTeam.value

            if (currentUserId == null || team == null) {
                _errorMessage.value = "Unable to leave team: missing data"
                return@launch
            }

            // Presidents must transfer first
            if (team.presidentId == currentUserId) {
                _showPresidentLeaveWarning.value = true
                return@launch
            }

            _isLoading.value = true
            try {
                // Remove user from the appropriate team role
                val updatedTeam = when {
                    team.vicePresidentId == currentUserId -> team.copy(vicePresidentId = null)
                    currentUserId in team.captainIds    -> team.copy(captainIds = team.captainIds.filter { it != currentUserId })
                    else                                -> team.copy(playerIds  = team.playerIds.filter  { it != currentUserId })
                }

                // Update team document
                val teamSuccess = database.updateDocument("teams", team.id, updatedTeam)
                if (!teamSuccess) {
                    _errorMessage.value = "Failed to update team membership"
                    return@launch
                }

                // Clear teamMembership on user profile
                val userSuccess = database.updateUserData(
                    currentUserId,
                    mapOf("teamMembership" to null)
                )
                if (!userSuccess) {
                    _errorMessage.value = "Failed to update user profile"
                    return@launch
                }

                // Update local state and navigate
                _currentUser.value    = _currentUser.value?.copy(teamMembership = null)
                _currentTeam.value    = null
                _teamMembers.value    = TeamMembersState.Initial
                _successMessage.value = "Successfully left the team"
                _navigationEvent.value = TeamNavigationEvent.NavigateToCreateTeam

            } catch (e: Exception) {
                _errorMessage.value = "Error leaving team: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun transferPresidencyAndLeave(newPresidentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid
                val team = _currentTeam.value

                if (currentUserId == null || team == null) {
                    _errorMessage.value = "Missing data to transfer presidency"
                    return@launch
                }

                // Handle the special case first: empty newPresidentId means last member case
                if (newPresidentId.isEmpty() ||
                    (team.playerIds.size == 1 && team.playerIds.contains(currentUserId) &&
                            team.presidentId == currentUserId)) {

                    // Delete the team since the president is the last member
                    val teamDeleted = database.deleteDocument("teams", team.id)

                    if (!teamDeleted) {
                        _errorMessage.value = "Failed to delete empty team"
                        return@launch
                    }

                    // Clear user's team membership
                    val clearSuccess = database.updateUserData(
                        currentUserId,
                        mapOf("teamMembership" to null)
                    )

                    if (!clearSuccess) {
                        _errorMessage.value = "Failed to clear your team membership"
                        return@launch
                    }

                    // Update local state & navigate
                    _currentUser.value = _currentUser.value?.copy(teamMembership = null)
                    _currentTeam.value = null
                    _teamMembers.value = TeamMembersState.Initial
                    _successMessage.value = "Team deleted as you were the last member"
                    _navigationEvent.value = TeamNavigationEvent.NavigateToCreateTeam

                    return@launch
                }

                // Regular presidency transfer for teams with multiple members
                println("DEBUG: Transferring presidency from $currentUserId to $newPresidentId")

                // STEP 1: Update team document FIRST to change presidentId
                val updatedTeam = team.copy(
                    presidentId = newPresidentId,
                    // If VP becomes president, clear VP role
                    vicePresidentId = if (team.vicePresidentId == newPresidentId) null else team.vicePresidentId,
                    // If captain becomes president, remove from captains list
                    captainIds = team.captainIds.filterNot { it == newPresidentId },
                    // Remove the current president (who is leaving) from playerIds
                    playerIds = team.playerIds.filterNot { it == currentUserId }
                )

                val teamUpdated = database.updateDocument("teams", team.id, updatedTeam)
                if (!teamUpdated) {
                    _errorMessage.value = "Failed to update team"
                    return@launch
                }

                // STEP 2: Now that the team document shows the new president,
                // update the new president's role
                val newPresidentUpdate = database.updateUserData(
                    newPresidentId,
                    mapOf("teamMembership" to TeamMembership(teamId = team.id, role = TeamRole.PRESIDENT))
                )

                if (!newPresidentUpdate) {
                    _errorMessage.value = "Failed to update new president's role"
                    return@launch
                }

                // STEP 3: Finally, clear former president's membership
                val clearSuccess = database.updateUserData(
                    currentUserId,
                    mapOf("teamMembership" to null)
                )

                if (!clearSuccess) {
                    _errorMessage.value = "Failed to clear your team membership"
                    return@launch
                }

                // Update local state & navigate
                _currentUser.value = _currentUser.value?.copy(teamMembership = null)
                _currentTeam.value = null
                _teamMembers.value = TeamMembersState.Initial
                _successMessage.value = "Presidency transferred successfully"
                _navigationEvent.value = TeamNavigationEvent.NavigateToCreateTeam

            } catch (e: Exception) {
                _errorMessage.value = "Error transferring presidency: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun kickUser(user: User, team: Team) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid

                println("DEBUG: Starting kick operation for user ${user.id}")
                println("DEBUG: Using team: ${team.id}")

                if (currentUserId == null) {
                    _errorMessage.value = "Missing data to kick user"
                    return@launch
                }

                // Set the current team before proceeding
                _currentTeam.value = team

                // Update the team document first
                println("DEBUG: Updating team document")
                val updatedTeam = team.copy(
                    vicePresidentId = if (team.vicePresidentId == user.id) null else team.vicePresidentId,
                    captainIds = team.captainIds.filterNot { it == user.id },
                    playerIds = team.playerIds.filterNot { it == user.id }
                )

                val teamSuccess = database.updateDocument("teams", team.id, updatedTeam)
                println("DEBUG: Team update result: $teamSuccess")

                if (!teamSuccess) {
                    _errorMessage.value = "Failed to update team"
                    return@launch
                }

                // Then update the user's membership
                println("DEBUG: Updating user's team membership")
                val userSuccess = database.updateUserData(
                    user.id,
                    mapOf("teamMembership" to null)
                )

                println("DEBUG: User update result: $userSuccess")

                if (userSuccess) {
                    _successMessage.value = "User ${user.name} has been removed from the team"
                } else {
                    // Even when user update fails, we still need to update the UI
                    _successMessage.value = "User was removed from team roster"
                    // Log the error but continue
                    println("DEBUG: Failed to update user membership, but team was updated")
                }

                // ALWAYS update UI regardless of user document update success
                // Get fresh team data directly from database
                val freshTeam = database.getDocument<Team>("teams/${team.id}")
                if (freshTeam != null) {
                    // Update the local state with fresh data from database
                    _currentTeam.value = freshTeam
                    loadTeamMembers(freshTeam)
                } else {
                    // Fallback to our local updated copy
                    _currentTeam.value = updatedTeam
                    loadTeamMembers(updatedTeam)
                }

                // Force UI refresh
                _refreshTrigger.value += 1

            } catch (e: Exception) {
                println("DEBUG: Exception in kickUser: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = "Error kicking user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeUserRole(user: User, newRole: TeamRole) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid
                val team = _currentTeam.value

                if (currentUserId == null || team == null) {
                    _errorMessage.value = "Unable to change role: missing data"
                    return@launch
                }

                // Verify current user is president
                if (team.presidentId != currentUserId) {
                    _errorMessage.value = "Only team president can change roles"
                    return@launch
                }

                // Don't allow changing the president's role
                if (user.id == team.presidentId) {
                    _errorMessage.value = "Cannot change president's role"
                    return@launch
                }

                // Create updated team object
                val updatedTeam = when (newRole) {
                    TeamRole.PRESIDENT -> {
                        // Transfer presidency - current president becomes regular player
                        team.copy(
                            presidentId = user.id,
                            // If VP becomes president, clear VP role
                            vicePresidentId = if (user.id == team.vicePresidentId) null else team.vicePresidentId,
                            // If captain becomes president, remove from captains list
                            captainIds = team.captainIds.filter { it != user.id },
                            // Add former president to regular players if not already there
                            playerIds = if (currentUserId in team.playerIds)
                                team.playerIds
                            else
                                team.playerIds + currentUserId
                        )
                    }
                    TeamRole.VICE_PRESIDENT -> {
                        // Make user vice president, handle previous VP if exists
                        val currentVP = team.vicePresidentId

                        // Remove user from captain list if they were a captain
                        val updatedCaptains = team.captainIds.filter { it != user.id }

                        // If there was a different VP, demote them to captain
                        val finalCaptains = if (currentVP != null && currentVP != user.id) {
                            updatedCaptains + currentVP
                        } else {
                            updatedCaptains
                        }

                        team.copy(
                            vicePresidentId = user.id,
                            captainIds = finalCaptains
                        )
                    }
                    TeamRole.CAPTAIN -> {
                        // Make user captain, remove from VP if they were VP
                        val updatedCaptains = if (user.id !in team.captainIds) {
                            team.captainIds + user.id
                        } else {
                            team.captainIds
                        }

                        team.copy(
                            vicePresidentId = if (team.vicePresidentId == user.id) null else team.vicePresidentId,
                            captainIds = updatedCaptains
                        )
                    }
                    TeamRole.PLAYER -> {
                        // Demote user to regular player
                        team.copy(
                            vicePresidentId = if (team.vicePresidentId == user.id) null else team.vicePresidentId,
                            captainIds = team.captainIds.filter { it != user.id }
                        )
                    }
                }

                // Update team in database
                val teamSuccess = database.updateDocument("teams", team.id, updatedTeam)
                if (!teamSuccess) {
                    _errorMessage.value = "Failed to update team roles"
                    return@launch
                }

                // Update user's role in their profile
                val userUpdate = database.updateUserData(user.id, mapOf(
                    "teamMembership" to TeamMembership(
                        teamId = team.id,
                        role = newRole
                    )
                ))

                // If this was a presidency transfer, update the former president's role to player
                if (newRole == TeamRole.PRESIDENT) {
                    val formerPresidentUpdate = database.updateUserData(currentUserId, mapOf(
                        "teamMembership" to TeamMembership(
                            teamId = team.id,
                            role = TeamRole.PLAYER
                        )
                    ))

                    if (!formerPresidentUpdate) {
                        _errorMessage.value = "Failed to update former president's role"
                        return@launch
                    }

                    // Update current user in state
                    _currentUser.value = _currentUser.value?.copy(
                        teamMembership = TeamMembership(
                            teamId = team.id,
                            role = TeamRole.PLAYER
                        )
                    )

                    // Refresh UI after presidency transfer:
                    _successMessage.value = "Presidency transferred successfully"

                    // Update the current team in state
                    _currentTeam.value = updatedTeam

                    // Force reload of team members to reflect new roles
                    loadTeamMembers(updatedTeam)

                    // Refresh current user data to ensure UI shows updated state
                    getCurrentUserData()

                    // Navigation event to refresh the UI if needed
                    _navigationEvent.value = TeamNavigationEvent.NavigateToTeam(team.id)
                }

                if (userUpdate) {
                    _successMessage.value = "${user.name}'s role updated to ${newRole.name}"

                    // Refresh team members list to show updated roles
                    loadTeamMembers(updatedTeam)
                } else {
                    _errorMessage.value = "Failed to update user's role"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error updating role: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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

    // Helper function to delete all pending join requests for a user
    private suspend fun deleteAllUserPendingRequests(userId: String) {
        try {
            // Get all pending requests by the user
            val requests = database.getCollectionFiltered<TeamJoinRequest>(
                "teamJoinRequests",
                "userId",
                userId,
                serializer = kotlinx.serialization.builtins.ListSerializer(TeamJoinRequest.serializer())
            ).filter { it.status == RequestStatus.PENDING }

            // Delete each request
            for (request in requests) {
                database.deleteDocument("teamJoinRequests", request.id)
            }

            // Update the local state
            _userPendingRequests.value = emptyList()

            println("DEBUG: Deleted ${requests.size} pending join requests for user $userId")
        } catch (e: Exception) {
            println("DEBUG: Error deleting user requests: ${e.message}")
            // Don't throw or set error message to avoid interrupting the main flow
        }
    }

    fun setShowPresidentLeaveWarning(show: Boolean) {
        _showPresidentLeaveWarning.value = show
    }

    // ----------------- TEAM JOINING ---------------- //
    private val _teamJoinRequests = MutableStateFlow<List<TeamJoinRequestWithUser>>(emptyList())
    val teamJoinRequests: StateFlow<List<TeamJoinRequestWithUser>> = _teamJoinRequests.asStateFlow()

    data class TeamJoinRequestWithUser(
        val request: TeamJoinRequest,
        val user: User
    )

    fun loadTeamJoinRequests() {
        viewModelScope.launch {
            val currentTeamId = _currentTeam.value?.id ?: return@launch
            println("DEBUG: Starting loadTeamJoinRequests for team: $currentTeamId")
            try {
                _isLoading.value = true
                val requests = database.getCollectionFiltered<TeamJoinRequest>(
                    "teamJoinRequests",
                    "teamId",
                    currentTeamId,
                    serializer = kotlinx.serialization.builtins.ListSerializer(TeamJoinRequest.serializer())
                ).filter { it.status == RequestStatus.PENDING }
                println("DEBUG: Raw requests from DB: $requests")
                val requestsWithUsers = requests.mapNotNull { request ->
                    try {
                        val user = database.getUserData(request.userId)
                        println("DEBUG: Fetched user ${request.userId}: $user")
                        if (user != null) TeamJoinRequestWithUser(request, user) else null
                    } catch (e: Exception) {
                        println("DEBUG: Error fetching user ${request.userId}: ${e.message}")
                        null
                    }
                }
                println("DEBUG: Found ${requests.size} pending requests, ${requestsWithUsers.size} with users for team $currentTeamId")
                _teamJoinRequests.value = requestsWithUsers
            } catch (e: Exception) {
                println("DEBUG: Error in loadTeamJoinRequests: ${e.message}")
                _errorMessage.value = "Failed to load join requests: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createJoinRequest(teamId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUserId = auth.getCurrentUser()?.uid ?: throw Exception("Not logged in")

                // First, check if the user is already in a team
                val currentUser = _currentUser.value ?: database.getDocument<User>("users/$currentUserId")

                if (currentUser?.teamMembership != null) {
                    _errorMessage.value = "You are already a member of a team"
                    return@launch
                }

                // Create the request object first
                val request = TeamJoinRequest(
                    teamId = teamId,
                    userId = currentUserId,
                    timestamp = Clock.System.now().toString()
                )

                // Check if the collection exists before querying
                try {
                    // Only check for existing requests if we're sure the collection exists
                    val existingRequests = database.getCollectionFiltered<TeamJoinRequest>(
                        "teamJoinRequests",
                        "userId",
                        currentUserId,
                        serializer = kotlinx.serialization.builtins.ListSerializer(TeamJoinRequest.serializer())
                    ).filter { it.status == RequestStatus.PENDING && it.teamId == teamId }

                    if (existingRequests.isNotEmpty()) {
                        _errorMessage.value = "You already have a pending request for this team"
                        return@launch
                    }
                } catch (e: Exception) {
                    // Collection might not exist yet, which is fine - continue with creating the request
                    println("DEBUG: No existing requests found or collection doesn't exist yet")
                }

                // Create the request in the database
                val id = database.createDocument("teamJoinRequests", request)
                if (id.isNotEmpty()) {
                    _successMessage.value = "Join request sent successfully"

                    // Reload the user's pending requests to update the UI
                    loadUserPendingRequests()

                    // New request to the current list to update UI immediately
                    val newRequest = request.copy(id = id)
                    _userPendingRequests.value += newRequest
                } else {
                    _errorMessage.value = "Failed to send join request"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun handleJoinRequest(requestId: String, approve: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUserId = auth.getCurrentUser()?.uid ?: throw Exception("Not logged in")

                // Get the request
                val request = database.getDocument<TeamJoinRequest>("teamJoinRequests/$requestId")
                    ?: throw Exception("Request not found")

                // Check user's team role
                val currentTeam = _currentTeam.value ?: throw Exception("Team not found")
                val isPresident = currentTeam.presidentId == currentUserId
                val isVicePresident = currentTeam.vicePresidentId == currentUserId

                if (!isPresident && !isVicePresident) {
                    throw Exception("Only team leaders can approve requests")
                }

                val updatedRequest = request.copy(
                    status = if (approve) RequestStatus.APPROVED else RequestStatus.REJECTED,
                    responseTimestamp = Clock.System.now().toString(),
                    responseBy = currentUserId
                )

                val success = database.updateDocument("teamJoinRequests", requestId, updatedRequest)

                if (success && approve) {
                    // Add user to team
                    val updatedPlayerIds = currentTeam.playerIds + request.userId
                    val updatedTeam = currentTeam.copy(playerIds = updatedPlayerIds)

                    val teamUpdateSuccess = database.updateDocument("teams", currentTeam.id, updatedTeam)

                    if (teamUpdateSuccess) {
                        // Update the user's team membership
                        val teamMembership = TeamMembership(
                            teamId = currentTeam.id,
                            role = TeamRole.PLAYER
                        )

                        val userUpdateSuccess = database.updateUserData(
                            request.userId,
                            mapOf("teamMembership" to teamMembership)
                        )

                        if (teamUpdateSuccess && userUpdateSuccess) {
                            // Delete all other pending requests from this user since they joined a team
                            deleteAllUserPendingRequests(request.userId)

                            _successMessage.value = if (approve) "Request approved" else "Request rejected"

                            // Get fresh team data from database to ensure we have the latest
                            val freshTeam = database.getDocument<Team>("teams/${currentTeam.id}")
                            if (freshTeam != null) {
                                _currentTeam.value = freshTeam
                                // Force reload with fresh data
                                loadTeamMembers(freshTeam)
                                loadTeamJoinRequests()
                                // Force UI refresh by incrementing trigger
                                _refreshTrigger.value += 1
                            } else {
                                // Fallback to using our local updated copy
                                _currentTeam.value = updatedTeam
                                loadTeamMembers(updatedTeam)
                                loadTeamJoinRequests()
                                _refreshTrigger.value += 1
                            }
                        } else {
                            _errorMessage.value = "Failed to update user data"
                        }
                    } else {
                        _errorMessage.value = "Failed to update team"
                    }
                } else if (success) {
                    _successMessage.value = "Request rejected"
                    // Even for rejections, refresh the data to update UI
                    loadTeamJoinRequests()
                    _refreshTrigger.value += 1
                } else {
                    _errorMessage.value = "Failed to update request"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _userPendingRequests = MutableStateFlow<List<TeamJoinRequest>>(emptyList())
    val userPendingRequests: StateFlow<List<TeamJoinRequest>> = _userPendingRequests.asStateFlow()

    // Load user's pending requests
    fun loadUserPendingRequests() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.getCurrentUser()?.uid ?: return@launch

                println("DEBUG: Loading pending requests for user: $currentUserId")

                val requests = database.getCollectionFiltered<TeamJoinRequest>(
                    "teamJoinRequests",
                    "userId",
                    currentUserId,
                    serializer = kotlinx.serialization.builtins.ListSerializer(TeamJoinRequest.serializer())
                ).filter { it.status == RequestStatus.PENDING }

                println("DEBUG: Found ${requests.size} pending requests")

                _userPendingRequests.value = requests
            } catch (e: Exception) {
                println("DEBUG: Error loading user pending requests: ${e.message}")
                // Don't set error message to avoid UI disruption
            }
        }
    }

    // Cancel join request
    fun cancelJoinRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = database.deleteDocument("teamJoinRequests", requestId)

                if (success) {
                    // Update the local state by removing the canceled request
                    _userPendingRequests.value = _userPendingRequests.value.filter { it.id != requestId }
                    _successMessage.value = "Join request canceled"
                } else {
                    _errorMessage.value = "Failed to cancel join request"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error canceling request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ----------------------------------------------- //

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