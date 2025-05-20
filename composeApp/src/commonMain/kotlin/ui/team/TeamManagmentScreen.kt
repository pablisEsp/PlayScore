package ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Team
import io.ktor.websocket.Frame
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(
    teamId: String,
    navController: NavController,
    teamViewModel: TeamViewModel = koinInject()
) {
    val currentTeam by teamViewModel.currentTeam.collectAsState()
    val isLoading by teamViewModel.isLoading.collectAsState()

    LaunchedEffect(teamId) {
        teamViewModel.getTeamById(teamId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTeam?.name ?: "My Team") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
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
                currentTeam?.let { team ->
                    TeamDetails(team)
                } ?: Text("Team not found")
            }
        }
    }
}

@Composable
fun TeamDetails(team: Team) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = team.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Team Management", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Description: ${team.description.ifEmpty { "No description" }}")
                Text("Location: ${team.location.ifEmpty { "Not specified" }}")
                Text("Players: ${team.playerIds.size}")
                Text("Ranking: ${team.ranking}")
                Text("Wins/Losses: ${team.totalWins}/${team.totalLosses}")
            }
        }
    }
}