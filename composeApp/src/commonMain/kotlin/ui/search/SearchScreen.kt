package ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Team
import data.model.User
import org.koin.compose.koinInject
import ui.team.TeamJoinRequestButton
import viewmodel.SearchFilter
import viewmodel.SearchResult
import viewmodel.SearchViewModel
import viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    searchViewModel: SearchViewModel = koinInject(),
    filter: String
) {
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val activeFilter by searchViewModel.activeFilter.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val errorMessage by searchViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }



    // Apply the filter from navigation parameter
    LaunchedEffect(filter) { // Changed: Use the 'filter' parameter directly
        when (filter) {
            "Teams" -> searchViewModel.setFilter(SearchFilter.Teams)
            "Users" -> searchViewModel.setFilter(SearchFilter.Users)
            // If filter is an empty string (default from nav) or other,
            // this will keep the ViewModel's current filter.
            else -> { /* Keep current filter or handle default */ }
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            searchViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search bar (smaller and at the top)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchViewModel.updateSearchQuery(it) },
                label = { Text("Search") },
                placeholder = { Text("Search players or teams...") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == SearchFilter.All,
                    onClick = { searchViewModel.setFilter(SearchFilter.All) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = activeFilter == SearchFilter.Teams,
                    onClick = { searchViewModel.setFilter(SearchFilter.Teams) },
                    label = { Text("Teams") }
                )
                FilterChip(
                    selected = activeFilter == SearchFilter.Users,
                    onClick = { searchViewModel.setFilter(SearchFilter.Users) },
                    label = { Text("Users") }
                )
            }

            // Results
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (searchResults.isEmpty() && searchQuery.length >= 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No results found")
                }
            } else if (searchQuery.length < 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Enter at least 2 characters to search")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(searchResults) { result ->
                        when (result) {
                            is SearchResult.TeamResult -> TeamSearchItem(result.team, navController)
                            is SearchResult.UserResult -> UserSearchItem(result.user, navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamSearchItem(
    team: Team,
    navController: NavController,
    teamViewModel: TeamViewModel = koinInject()
) {
    // Get current user's team ID
    val currentUser by teamViewModel.currentUser.collectAsState()
    val currentUserTeamId = currentUser?.teamMembership?.teamId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("navigation.TeamManagement/${team.id}")
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team logo with rounded rectangle shape
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = team.name.firstOrNull()?.toString() ?: "T",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Team",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (team.description.isNotEmpty()) {
                    Text(
                        text = team.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Add the join request button with proper padding
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TeamJoinRequestButton(
                team = team,
                currentUserTeamId = currentUserTeamId,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun UserSearchItem(user: User, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Uncomment when user profile navigation is ready
                // navController.navigate("userProfile/${user.id}")
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar with circle shape
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.toString() ?: "U",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "User",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}