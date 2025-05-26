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
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    navController: NavController,
    teamViewModel: TeamViewModel = koinInject()
) {
    var teamName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val isLoading by teamViewModel.isLoading.collectAsState()
    val errorMessage by teamViewModel.errorMessage.collectAsState()
    val teamCreationResult by teamViewModel.teamCreationResult.collectAsState()
    val isTeamCreationComplete by teamViewModel.isTeamCreationComplete.collectAsState()
    val currentUser by teamViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        teamViewModel.getCurrentUserData()
    }

    // Redirect users who already have a team
    LaunchedEffect(currentUser) {
        val teamId = currentUser?.teamMembership?.teamId
        if (!teamId.isNullOrEmpty()) {
            // User already has a team, navigate to team management
            navController.navigate("navigation.Team") {
                popUpTo("navigation.Home") { inclusive = false }
            }
        }
    }

    // Handle navigation when team creation completes
    LaunchedEffect(isTeamCreationComplete) {
        if (isTeamCreationComplete) {
            val teamId = teamViewModel.currentUser.value?.teamMembership?.teamId
            if (!teamId.isNullOrEmpty()) {
                // Navigate to team management
                navController.navigate("navigation.Team") {
                    // Clear the back stack to prevent going back to the team creation screen
                    popUpTo("navigation.Home") { inclusive = false }
                }
            }
            teamViewModel.resetTeamCreationState()
        }
    }

    // Show feedback messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            teamViewModel.clearError()
        }
    }

    LaunchedEffect(teamCreationResult) {
        teamCreationResult?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Team") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Create Your Team",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = { Text("Team Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    teamViewModel.createTeam(teamName.trim(), description.trim())
                },
                enabled = teamName.trim().isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create Team")
            }

            // Show success message if available
            teamCreationResult?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}