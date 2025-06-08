package ui.team

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun TeamMembersTab(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val refreshTrigger by viewModel.refreshTrigger.collectAsState()
    val currentTeam by viewModel.currentTeam.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Initial setup
    LaunchedEffect(Unit) {
        viewModel.setCurrentTeam(team)
        viewModel.getCurrentUserData()
    }

    // This will properly refresh when team changes or refresh is triggered
    LaunchedEffect(team.id, refreshTrigger) {
        viewModel.getTeamById(team.id)
    }

    // Watch for currentTeam updates and reload members when it changes
    LaunchedEffect(currentTeam) {
        currentTeam?.let { freshTeam ->
            viewModel.loadTeamMembers(freshTeam)
            // Also load join requests if the current user is a leader
            currentUser?.id?.let { userId ->
                if (freshTeam.presidentId == userId || freshTeam.vicePresidentId == userId) {
                    viewModel.loadTeamJoinRequests()
                }
            }
        }
    }

    // Use currentTeam if available, otherwise fall back to the passed team
    val activeTeam = currentTeam ?: team
    val currentUserId = currentUser?.id

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        // Display Join Requests if the current user is authorized and there are requests
        if (currentUserId != null && (activeTeam.presidentId == currentUserId || activeTeam.vicePresidentId == currentUserId)) {
            TeamJoinRequests(
                team = activeTeam,
                currentUserId = currentUserId,
                viewModel = viewModel
            )
            // Add some space if there are requests, before showing members
            val joinRequests by viewModel.teamJoinRequests.collectAsState()
            if (joinRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        TeamMembers(activeTeam)
        // Leave Team button and associated dialogs have been removed.
    }
}