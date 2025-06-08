package ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import data.model.Team
import viewmodel.TeamViewModel

@Composable
fun LeaveTeamButton(
    team: Team,
    viewModel: TeamViewModel,
    modifier: Modifier = Modifier
) {
    val teamMembersState by viewModel.teamMembers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val showPresidentLeaveWarning by viewModel.showPresidentLeaveWarning.collectAsState()

    var showLeaveTeamConfirmationDialog by remember { mutableStateOf(false) }
    var showTransferPresidencyDialog by remember { mutableStateOf(false) }

    // This will be triggered when viewModel.leaveTeam() determines the user is president
    if (showPresidentLeaveWarning) {
        val isLastMember = team.playerIds.size == 1 && team.playerIds.contains(currentUser?.id)

        if (isLastMember) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowPresidentLeaveWarning(false) },
                title = { Text("Delete Team") },
                text = { Text("You are the last member of this team. Leaving will delete the team. Are you sure you want to continue?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.transferPresidencyAndLeave("") // Empty string signals last member case
                            viewModel.setShowPresidentLeaveWarning(false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Delete Team")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setShowPresidentLeaveWarning(false) }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            PresidentLeaveWarningDialog(
                onDismiss = { viewModel.setShowPresidentLeaveWarning(false) },
                onConfirm = {
                    viewModel.setShowPresidentLeaveWarning(false)
                    showTransferPresidencyDialog = true
                }
            )
        }
    }

    if (showTransferPresidencyDialog) {
        val eligibleMembers = (teamMembersState as? TeamViewModel.TeamMembersState.Success)?.let { state ->
            buildList {
                state.vicePresident?.let { add(it) }
                addAll(state.captains)
                addAll(state.players)
            }.filter { it.id != currentUser?.id } // Ensure current user (president) is not in the list
        } ?: emptyList()

        TransferPresidencyDialog(
            team = team,
            members = eligibleMembers,
            onDismiss = { showTransferPresidencyDialog = false },
            onConfirm = { newPresidentId ->
                viewModel.transferPresidencyAndLeave(newPresidentId)
                showTransferPresidencyDialog = false
            }
        )
    }

    if (showLeaveTeamConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveTeamConfirmationDialog = false },
            title = { Text("Leave Team") },
            text = { Text("Are you sure you want to leave ${team.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveTeam() // This might trigger showPresidentLeaveWarning
                        showLeaveTeamConfirmationDialog = false
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
                TextButton(onClick = { showLeaveTeamConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Button(
        onClick = {
            // Before showing confirmation, ensure current user data is loaded
            // as leaveTeam() in ViewModel relies on it.
            if (currentUser == null) {
                viewModel.getCurrentUserData() // Ensure user data is fresh
            }
            showLeaveTeamConfirmationDialog = true
        },
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Text("Leave Team")
    }
}