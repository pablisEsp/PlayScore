package ui.team

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import org.koin.compose.koinInject
import ui.components.PresidentLeaveWarningDialog
import ui.components.TransferPresidencyDialog
import viewmodel.TeamViewModel

@Composable
fun TeamMembersTab(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val teamMembersState by viewModel.teamMembers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val showPresidentLeaveWarning by viewModel.showPresidentLeaveWarning.collectAsState()
    var showLeaveTeamConfirmation by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    val refreshTrigger by viewModel.refreshTrigger.collectAsState()
    val currentTeam by viewModel.currentTeam.collectAsState()

    // Initial setup
    LaunchedEffect(Unit) {
        viewModel.setCurrentTeam(team)
        viewModel.getCurrentUserData()
    }

    // This will properly refresh when team changes or refresh is triggered
    LaunchedEffect(team.id, refreshTrigger) {
        // Get fresh data from the database
        viewModel.getTeamById(team.id)
    }

    // Watch for currentTeam updates and reload members when it changes
    LaunchedEffect(currentTeam) {
        currentTeam?.let { freshTeam ->
            // Explicitly load team members with the fresh team data
            viewModel.loadTeamMembers(freshTeam)
        }
    }

    // Use currentTeam if available, otherwise fall back to the passed team
    val activeTeam = currentTeam ?: team

    // Rest of your code remains the same, but use activeTeam instead of team
    if (showPresidentLeaveWarning) {
        PresidentLeaveWarningDialog(
            onDismiss = { viewModel.resetLeaveWarning() },
            onConfirm = {
                viewModel.resetLeaveWarning()
                showTransferDialog = true
            }
        )
    }

    if (showTransferDialog) {
        val eligible = (teamMembersState as? TeamViewModel.TeamMembersState.Success)?.let { s ->
            buildList {
                s.vicePresident?.let { add(it) }
                addAll(s.captains)
                addAll(s.players)
            }
        } ?: emptyList()

        TransferPresidencyDialog(
            team = team,
            members = eligible,
            onDismiss = { showTransferDialog = false },
            onConfirm = { newPresidentId ->
                viewModel.transferPresidencyAndLeave(newPresidentId)
                showTransferDialog = false
            }
        )
    }

    if (showLeaveTeamConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveTeamConfirmation = false },
            title = { Text("Leave Team") },
            text = { Text("Are you sure you want to leave ${team.name}?") },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TeamMembers(activeTeam)
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
}