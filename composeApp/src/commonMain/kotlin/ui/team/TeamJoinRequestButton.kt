package ui.team

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@Composable
fun TeamJoinRequestButton(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userPendingRequests by viewModel.userPendingRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Load current user and their pending requests when component mounts
    LaunchedEffect(Unit) {
        viewModel.getCurrentUserData()
        viewModel.loadUserPendingRequests()
    }

    // Check if user is already in a team
    val isInTeam = currentUser?.teamMembership != null

    // Check if user already has a pending request for this team
    val hasPendingRequest = userPendingRequests.any {
        it.teamId == team.id
    }

    // Don't show button if user is already in a team or has a pending request
    if (isInTeam) {
        return
    }

    Button(
        onClick = { viewModel.createJoinRequest(team.id) },
        enabled = !isLoading && !hasPendingRequest,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        when {
            isLoading -> Text("Processing...")
            hasPendingRequest -> Text("Request Pending")
            else -> Text("Request to Join")
        }
    }
}