package ui.team

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import data.model.Team
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@Composable
fun TeamJoinRequestButton(
    team: Team,
    currentUserTeamId: String?,
    modifier: Modifier = Modifier,
    viewModel: TeamViewModel = koinInject()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingRequests by viewModel.userPendingRequests.collectAsState()
    var showCancelConfirmation by remember { mutableStateOf(false) }

    // Check if current user has a pending request for this team
    val pendingRequest = pendingRequests.find { it.teamId == team.id }

    // Load user's pending requests when component first displays
    LaunchedEffect(Unit) {
        viewModel.loadUserPendingRequests()
    }

    // Cancel request confirmation dialog
    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("Cancel Request") },
            text = { Text("Are you sure you want to cancel your request to join ${team.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRequest?.let { viewModel.cancelJoinRequest(it.id) }
                        showCancelConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Yes, Cancel Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text("Keep Request")
                }
            }
        )
    }

    if (currentUserTeamId != null && currentUserTeamId == team.id) {
        // User is already in this team
        Button(
            onClick = { /* Do nothing */ },
            enabled = false,
            modifier = modifier
        ) {
            Text("Your Team")
        }
    } else if (currentUserTeamId != null) {
        // User is in another team
        Button(
            onClick = { /* Do nothing */ },
            enabled = false,
            modifier = modifier
        ) {
            Text("Already in Team")
        }
    } else if (pendingRequest != null) {
        // User has a pending request for this team
        OutlinedButton(
            onClick = { showCancelConfirmation = true },
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Cancel Request")
        }
    } else {
        // User can join this team
        Button(
            onClick = { viewModel.createJoinRequest(team.id) },
            enabled = !isLoading,
            modifier = modifier
        ) {
            Text("Request to Join")
        }
    }
}