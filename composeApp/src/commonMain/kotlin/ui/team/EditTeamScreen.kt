package ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Team
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTeamScreen(
    navController: NavController,
    teamViewModel: TeamViewModel = koinInject(),
    teamId: String
) {
    // State for form fields
    val currentTeam by teamViewModel.currentTeam.collectAsState()
    val isLoading by teamViewModel.isLoading.collectAsState()
    val errorMessage by teamViewModel.errorMessage.collectAsState()
    val successMessage by teamViewModel.successMessage.collectAsState()

    var teamName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    val originalTeamName = remember { mutableStateOf("") }

    // Initialize form with current team data
    LaunchedEffect(currentTeam) {
        if (currentTeam != null) {
            teamName = currentTeam?.name ?: ""
            originalTeamName.value = currentTeam?.name ?: ""
            description = currentTeam?.description ?: ""
            location = currentTeam?.location ?: ""
        } else {
            // Load team if not already loaded
            teamViewModel.getTeamById(teamId)
        }
    }

    // Handle navigation
    LaunchedEffect(successMessage) {
        if (successMessage != null && successMessage!!.contains("updated")) {
            // Navigate back after successful update
            navController.popBackStack()
        }
    }

    // Clear success message when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            teamViewModel.clearSuccessMessage()
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Edit Team Information",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Form fields
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    supportingText = {
                        if (teamName != originalTeamName.value) {
                            Text("Changing team name will check for duplicates")
                        }
                    }
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(150.dp),
                    maxLines = 5
                )

                // Error message
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit button
                Button(
                    onClick = {
                        currentTeam?.let { team ->
                            val updatedTeam = team.copy(
                                name = teamName.trim(),
                                description = description.trim(),
                                location = location.trim()
                            )
                            teamViewModel.updateTeamInfo(updatedTeam)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    enabled = teamName.isNotBlank() && currentTeam != null
                ) {
                    Text("Update Team")
                }

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}