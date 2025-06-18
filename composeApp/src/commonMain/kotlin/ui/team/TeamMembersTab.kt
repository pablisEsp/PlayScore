package ui.team

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val teamMembers by viewModel.teamMembers.collectAsState()

    // Explicitly create a derived state that will change when team members change
    val memberCount = remember(currentTeam) { currentTeam?.playerIds?.size ?: 0 }

    // Initial setup only once
    LaunchedEffect(Unit) {
        viewModel.setCurrentTeam(team)
        viewModel.getCurrentUserData()
    }

    // IMPORTANT: Force re-fetching of team data on EVERY refresh trigger change
    LaunchedEffect(refreshTrigger) {
        println("DEBUG: Refresh trigger changed: $refreshTrigger")
        viewModel.getTeamById(team.id)
    }

    // Watch teamMembers state changes to detect when data is refreshed
    LaunchedEffect(teamMembers) {
        println("DEBUG: Team members state changed")
        // No action needed - just forcing recomposition
    }

    // Use currentTeam if available, otherwise fall back to the passed team
    val activeTeam = currentTeam ?: team
    val currentUserId = currentUser?.id

    // Debug to verify props
    println("DEBUG: TeamMembersTab rendering with team ${activeTeam.id}, ${activeTeam.playerIds.size} members, refresh=$refreshTrigger")

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
            // Space if there are requests, before showing members
            val joinRequests by viewModel.teamJoinRequests.collectAsState()
            if (joinRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        TeamMembers(team = activeTeam, viewModel = viewModel)
        // Leave Team button and associated dialogs have been removed.
    }
}