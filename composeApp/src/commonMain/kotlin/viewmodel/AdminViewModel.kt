package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Tournament
import data.model.User
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
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkAdminAccess()
        loadTournaments()
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

    fun clearError() {
        _errorMessage.value = null
    }
}