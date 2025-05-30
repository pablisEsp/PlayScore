package ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import data.model.TeamRole
import data.model.User
import org.koin.compose.koinInject
import ui.components.RoleManagementDropdown
import viewmodel.TeamViewModel

@Composable
fun TeamMembers(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val teamMembersState by viewModel.teamMembers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val refreshTrigger by viewModel.refreshTrigger.collectAsState()
    val currentTeam by viewModel.currentTeam.collectAsState()

    // Use the most current team data, falling back to the prop if null
    val activeTeam = currentTeam ?: team

    // Make sure currentUser is loaded
    LaunchedEffect(activeTeam.id, refreshTrigger) {
        viewModel.getCurrentUserData()
        viewModel.loadTeamMembers(activeTeam)
    }

    // Use currentUser from ViewModel
    val userId = currentUser?.id

    // Debug info to help troubleshoot
    println("DEBUG: TeamMembers - currentUserId = $userId")
    println("DEBUG: TeamMembers - team.presidentId = ${team.presidentId}")
    val isPresident = team.presidentId == userId
    println("DEBUG: isPresident = $isPresident")


    Column(modifier = Modifier.fillMaxWidth()) {
        when (teamMembersState) {
            is TeamViewModel.TeamMembersState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is TeamViewModel.TeamMembersState.Error -> {
                Text(
                    text = "Error: ${(teamMembersState as TeamViewModel.TeamMembersState.Error).message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            is TeamViewModel.TeamMembersState.Success -> {
                val membersState = teamMembersState as TeamViewModel.TeamMembersState.Success

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // President Section
                    membersState.president?.let { president ->
                        item {
                            TeamRoleHeader("President")
                            TeamMemberItem(
                                user = president,
                                team = team,
                                currentUserId = userId,
                                onRoleChanged = { user, role ->
                                    viewModel.changeUserRole(user, role)
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Vice President Section
                    membersState.vicePresident?.let { vicePresident ->
                        item {
                            TeamRoleHeader("Vice President")
                            TeamMemberItem(
                                user = vicePresident,
                                team = team,
                                currentUserId = userId,
                                onRoleChanged = { user, role ->
                                    viewModel.changeUserRole(user, role)
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Captains Section
                    if (membersState.captains.isNotEmpty()) {
                        item {
                            TeamRoleHeader("Captains")
                        }
                        items(membersState.captains) { captain ->
                            TeamMemberItem(
                                user = captain,
                                team = team,
                                currentUserId = userId,
                                onRoleChanged = { user, role ->
                                    viewModel.changeUserRole(user, role)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Players Section
                    if (membersState.players.isNotEmpty()) {
                        item {
                            TeamRoleHeader("Players")
                        }
                        items(membersState.players) { player ->
                            TeamMemberItem(
                                user = player,
                                team = team,
                                currentUserId = userId,
                                onRoleChanged = { user, role ->
                                    viewModel.changeUserRole(user, role)
                                }
                            )
                        }
                    }
                }
            }
            else -> {
                Text("No team members found")
            }
        }
    }
}

@Composable
fun TeamRoleHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun TeamMemberItem(
    user: User,
    team: Team,
    currentUserId: String?,
    viewModel: TeamViewModel = koinInject(),
    onRoleChanged: (User, TeamRole) -> Unit
) {
    println("DEBUG: TeamMemberItem for ${user.name}")

    val isCurrentUser = user.id == currentUserId
    val isPresident = team.presidentId == currentUserId

    // Current user can manage roles if they're the president
    val canManageRoles = isPresident && !isCurrentUser

    println("DEBUG: canManageRoles = $canManageRoles")
    println("DEBUG: team = ${team.id}")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name + if (isCurrentUser) " (You)" else "",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Role management dropdown (only for president and not on themselves)
        if (canManageRoles) {
            println("DEBUG: Showing dropdown for ${user.name}")
            RoleManagementDropdown(
                user = user,
                team = team,
                currentUserIsPresident = isPresident,
                onRoleChanged = onRoleChanged,
                onKickUser = { viewModel.kickUser(it, team) }
            )
        } else {
            println("DEBUG: Not showing dropdown because: canManageRoles=$canManageRoles, team is not null")
        }
    }
}