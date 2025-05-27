package ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@Composable
fun TeamJoinRequestsTab(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val joinRequests by viewModel.teamJoinRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Set current team and load requests when tab displays
    LaunchedEffect(team.id, currentUser?.id) {
        println("DEBUG: Setting current team in TeamJoinRequestsTab: ${team.id}")
        viewModel.setCurrentTeam(team)
        viewModel.loadTeamJoinRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Join Requests",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Check permissions explicitly here instead of in the component
        val isPresident = team.presidentId == currentUser?.id
        val isVicePresident = team.vicePresidentId == currentUser?.id

        if (isLoading) {
            Text("Loading requests...")
        } else if (!isPresident && !isVicePresident) {
            Text("Only team leaders can view join requests")
        } else if (joinRequests.isEmpty()) {
            Text("No pending join requests")
        } else {
            // Use key to force recomposition when requests change
            key(joinRequests.size) {
                LazyColumn {
                    items(joinRequests) { requestWithUser ->
                        JoinRequestItem(
                            request = requestWithUser,
                            onApprove = { viewModel.handleJoinRequest(requestWithUser.request.id, true) },
                            onReject = { viewModel.handleJoinRequest(requestWithUser.request.id, false) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}