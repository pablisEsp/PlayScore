package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Tournament
import data.model.User
import data.model.UserRole
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import repository.TournamentRepository


class AdminViewModel(
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface,
    val tournamentRepository: TournamentRepository,
    private val userRepository: repository.UserRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()


    init {
        checkAdminAccess()
        loadTournaments()
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allUsers = userRepository.getAllUsers()
                _users.value = allUsers
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun updateUserRole(userId: String, newRole: UserRole): Boolean {
        // Prevent non-super-admins from assigning SUPER_ADMIN role
        if (currentUser.value?.globalRole != UserRole.SUPER_ADMIN && newRole == UserRole.SUPER_ADMIN) {
            _errorMessage.value = "Only Super Admins can assign Super Admin role"
            return false
        }

        _isLoading.value = true
        return try {
            val success = userRepository.updateUserRole(userId, newRole)
            if (success) {
                // Update the user in the local list
                _users.value = _users.value.map { user ->
                    if (user.id == userId) user.copy(globalRole = newRole) else user
                }
            } else {
                _errorMessage.value = "Failed to update user role"
            }
            success
        } catch (e: Exception) {
            _errorMessage.value = "Error updating role: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun toggleUserBan(userId: String, isBanned: Boolean): Boolean {
        // Prevent self-banning
        if (userId == _currentUser.value?.id) {
            _errorMessage.value = "You cannot ban yourself"
            return false
        }

        _isLoading.value = true
        return try {
            val success = userRepository.toggleUserBan(userId, isBanned)
            if (success) {
                // Update the user in the local list
                _users.value = _users.value.map { user ->
                    if (user.id == userId) {
                        user.copy(isBanned = isBanned)
                    } else {
                        user
                    }
                }
            } else {
                _errorMessage.value = "Failed to ${if (isBanned) "ban" else "unban"} user"
            }
            success
        } catch (e: Exception) {
            _errorMessage.value = "Error: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    fun checkAdminAccess() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = auth.getCurrentUser()?.uid
                val user = database.getUserData(uid)
                _currentUser.value = user
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load user data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTournaments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allTournaments = tournamentRepository.getAllTournaments()
                _tournaments.value = allTournaments
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load tournaments"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTournament(tournamentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = tournamentRepository.deleteTournament(tournamentId)
                if (success) {
                    loadTournaments() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to delete tournament"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun createTournamentInDb(tournament: Tournament): Boolean {
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        return try {
            val tournamentId = tournamentRepository.createTournament(tournament)
            if (tournamentId.isNotEmpty()) {
                loadTournaments() // Refresh tournaments list
                true
            } else {
                _errorMessage.value = "Failed to create tournament. Empty ID returned from repository."
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error creating tournament: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    fun updateTournament(tournament: Tournament) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = tournamentRepository.updateTournament(tournament)
                if (success) {
                    loadTournaments() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to update tournament"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}