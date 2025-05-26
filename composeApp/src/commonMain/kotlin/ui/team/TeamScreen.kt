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
import ui.components.LocalNavController
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
    val currentTeam by teamViewModel.currentTeam.collectAsState()

    // Get the current user data and team if applicable
    LaunchedEffect(Unit) {
        teamViewModel.getCurrentUserData()
        // getCurrentUserData already calls getTeamById if the user has a team
    }


    CompositionLocalProvider(LocalNavController provides navController){
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
                    val hasTeam = !currentUser?.teamMembership?.teamId.isNullOrEmpty()
                    print(hasTeam)
                    if (!hasTeam) {
                        NoTeamView(
                            onCreateTeamClick = { navController.navigate("navigation.CreateTeam") },
                            onJoinTeamClick = {
                                // Navigate to search with teams filter pre-selected
                                navController.navigate("navigation.Search?filter=Teams")
                            }
                        )
                    } else {
                        // Instead of navigating away, show team content directly here
                        currentTeam?.let { team ->
                            TeamDetails(team)
                        } ?: Text("Loading team information...")
                    }
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