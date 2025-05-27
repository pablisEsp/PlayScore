package ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.Team
import org.koin.compose.koinInject
import ui.components.LocalNavController
import viewmodel.TeamViewModel

@Composable
fun TeamDetails(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLeaveTeamConfirmation by remember { mutableStateOf(false) }
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    val navController = LocalNavController.current

    // Set current team in ViewModel to ensure it's available during leaveTeam()
    LaunchedEffect(team) {
        viewModel.setCurrentTeam(team)
    }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is TeamViewModel.TeamNavigationEvent.NavigateToTeam -> {
                viewModel.onNavigationEventProcessed()
                navController.navigate("navigation.Team") {
                    popUpTo("navigation.Home") { inclusive = false }
                }
            }
            is TeamViewModel.TeamNavigationEvent.NavigateToCreateTeam -> {
                viewModel.onNavigationEventProcessed()
                navController.navigate("navigation.CreateTeam")
            }
            is TeamViewModel.TeamNavigationEvent.None -> {}
        }
    }

    // Monitor success/error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    // Leave Team Confirmation Dialog
    if (showLeaveTeamConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveTeamConfirmation = false },
            title = { Text("Leave Team") },
            text = { Text("Are you sure you want to leave ${team.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveTeam()
                        showLeaveTeamConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Yes, Leave Team")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveTeamConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Team header with logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // If logo is available, display it
                if (team.logoUrl.isNotEmpty()) {
                    // You'd use an image loading library here like Coil
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape
                    ) {
                        // Placeholder for logo
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = team.name.take(1),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (team.location.isNotEmpty()) {
                        Text(
                            text = team.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Team stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Team Stats",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Ranking", team.ranking.toString())
                        StatItem("Total Points", team.pointsTotal.toString())
                        StatItem("W-L", "${team.totalWins}-${team.totalLosses}")
                    }
                }
            }

            // Team description
            if (team.description.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "About",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(team.description)
                    }
                }
            }

            val currentUser by viewModel.currentUser.collectAsState()
            val currentUserId = currentUser?.id
            if (!currentUserId.isNullOrEmpty()) {
                TeamJoinRequests(team, currentUserId)
            }

            // More team functionalities can be added here
            TeamMembers(team)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showLeaveTeamConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Leave Team")
            }
        }

        // Show snackbar for messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}