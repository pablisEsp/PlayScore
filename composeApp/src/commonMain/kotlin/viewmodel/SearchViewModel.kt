package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Team
import data.model.User
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val database: FirebaseDatabaseInterface
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(SearchFilter.All)
    val activeFilter = _activeFilter.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            debounceSearch()
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun setFilter(filter: SearchFilter) {
        _activeFilter.value = filter
        performSearch()
    }

    private fun debounceSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            performSearch()
        }
    }

    private fun performSearch() {
        val query = _searchQuery.value
        if (query.length < 2) return

        viewModelScope.launch {
            _isSearching.value = true
            _errorMessage.value = null

            try {
                val results = mutableListOf<SearchResult>()

                when (_activeFilter.value) {
                    SearchFilter.All, SearchFilter.Teams -> {
                        val teams = searchTeams(query)
                        results.addAll(teams.map { SearchResult.TeamResult(it) })
                    }
                    else -> {} // Skip team search if filter is Users
                }

                when (_activeFilter.value) {
                    SearchFilter.All, SearchFilter.Users -> {
                        val users = searchUsers(query)
                        results.addAll(users.map { SearchResult.UserResult(it) })
                    }
                    else -> {} // Skip user search if filter is Teams
                }

                _searchResults.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    private suspend fun searchTeams(query: String): List<Team> {
        val allTeams = database.getCollection<Team>("teams")
        return allTeams.filter { team ->
            team.name.contains(query, ignoreCase = true) ||
                    team.description.contains(query, ignoreCase = true)
        }
    }

    private suspend fun searchUsers(query: String): List<User> {
        val allUsers = database.getCollection<User>("users")
        return allUsers.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
                    user.username.contains(query, ignoreCase = true)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

enum class SearchFilter {
    All, Teams, Users
}

sealed class SearchResult {
    data class TeamResult(val team: Team) : SearchResult()
    data class UserResult(val user: User) : SearchResult()
}