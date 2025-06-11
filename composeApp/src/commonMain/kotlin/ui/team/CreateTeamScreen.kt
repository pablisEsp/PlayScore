package ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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