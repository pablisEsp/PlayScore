package ui.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.Team
import data.model.User
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@Composable
fun TeamMembers(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val teamMembersState by viewModel.teamMembers.collectAsState()


    LaunchedEffect(teamMembersState) {
        println("DEBUG: TeamMembersState is now: $teamMembersState")
    }

    LaunchedEffect(team.id) {
        println("DEBUG: Loading team members for team: ${team.id}")
        viewModel.loadTeamMembers(team)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Team Members",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (val members = teamMembersState) {
                is TeamViewModel.TeamMembersState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is TeamViewModel.TeamMembersState.Success -> {
                    TeamMembersList(
                        president = members.president,
                        vicePresident = members.vicePresident,
                        captains = members.captains,
                        players = members.players
                    )
                }
                is TeamViewModel.TeamMembersState.Error -> {
                    Text(
                        "Failed to load team members: ${members.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {} // Empty initial state
            }
        }
    }
}

@Composable
fun TeamMembersList(
    president: User?,
    vicePresident: User?,
    captains: List<User>,
    players: List<User>
) {
    // President
    president?.let {
        MemberItem(user = it, role = "President")
    }

    // Vice President
    vicePresident?.let {
        MemberItem(user = it, role = "Vice President")
    }

    // Captains
    if (captains.isNotEmpty()) {
        Text(
            "Captains",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        captains.forEach { captain ->
            MemberItem(user = captain, role = "Captain")
        }
    }

    // Players
    if (players.isNotEmpty()) {
        Text(
            "Players",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        players.forEach { player ->
            MemberItem(user = player)
        }
    }
}

@Composable
fun MemberItem(user: User, role: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User avatar
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Role badge
        role?.let {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}