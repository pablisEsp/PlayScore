// ui/team/TeamScreen.kt
package ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import navigation.CreateTeam
import navigation.TeamManagement
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    navController: NavController,
    teamViewModel: TeamViewModel = koinInject()
) {
    val currentUser by teamViewModel.currentUser.collectAsState()
    val isLoading by teamViewModel.isLoading.collectAsState()
    val navigationEvent by teamViewModel.navigationEvent.collectAsState()

    // Observe navigation events
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is TeamViewModel.TeamNavigationEvent.NavigateToTeam -> {
                navController.navigate(TeamManagement(event.teamId).toString())
                teamViewModel.onNavigationEventProcessed()
            }
            TeamViewModel.TeamNavigationEvent.None -> { /* Do nothing */ }
        }
    }

    // Auto-navigate to team management if user has a team
    LaunchedEffect(currentUser) {
        val teamId = currentUser?.teamMembership?.teamId
        if (teamId != null && navigationEvent == TeamViewModel.TeamNavigationEvent.None) {
            // User has a team but no navigation event is pending, navigate to team management
            navController.navigate(TeamManagement(teamId).toString())
        }
    }

    LaunchedEffect(Unit) {
        teamViewModel.getCurrentUserData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                val hasTeam = currentUser?.teamMembership?.teamId != null

                if (!hasTeam) {
                    NoTeamView(
                        onCreateTeamClick = { navController.navigate(CreateTeam) },
                        onJoinTeamClick = { /* Will implement later */}
                            )
                        } else {
                        // Team view will be implemented later
                        Text("Your team view will be shown here")
                    }
                }
            }
        }
    }

    @Composable
    fun NoTeamView(
        onCreateTeamClick: () -> Unit,
        onJoinTeamClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "You are not part of any team",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateTeamClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create a Team")
            }

            Button(
                onClick = onJoinTeamClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join a Team")
            }
        }
    }