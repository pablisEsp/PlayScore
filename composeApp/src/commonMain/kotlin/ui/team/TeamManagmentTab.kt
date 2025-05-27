package ui.team

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import data.model.TeamRole
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@Composable
fun TeamManagementTab(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showLeaveTeamConfirmation by remember { mutableStateOf(false) }
    val isPresident = currentUser?.teamMembership?.role == TeamRole.PRESIDENT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Team Management",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Team settings - only for president
        if (isPresident) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Team Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* Navigate to edit team */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Team Information")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { /* Navigate to manage roles */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Team Roles")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Match settings - for future implementation
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Match Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Navigate to matchmaking settings */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("Matchmaking Preferences")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { /* Navigate to schedule */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("Team Schedule")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { /* Navigate to find match */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("Find a Match")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Leave team option at the bottom
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
    }
}