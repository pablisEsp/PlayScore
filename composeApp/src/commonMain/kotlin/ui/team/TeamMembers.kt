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

    LaunchedEffect(team.id) {
        viewModel.loadTeamMembers(team)
    }

    when (val state = teamMembersState) {
        is TeamViewModel.TeamMembersState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                LinearProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        is TeamViewModel.TeamMembersState.Success -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                // President section
                state.president?.let { president ->
                    MemberSection("President", listOf(president))
                }

                // Vice President section
                state.vicePresident?.let { vp ->
                    MemberSection("Vice President", listOf(vp))
                }

                // Captains section
                if (state.captains.isNotEmpty()) {
                    MemberSection("Captains", state.captains)
                }

                // Regular players section
                if (state.players.isNotEmpty()) {
                    MemberSection("Players", state.players)
                }

                if (state.president == null && state.vicePresident == null &&
                    state.captains.isEmpty() && state.players.isEmpty()) {
                    Text(
                        "No team members found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        is TeamViewModel.TeamMembersState.Error -> {
            Text(
                "Error: ${state.message}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        else -> { /* Initial state - show nothing */ }
    }
}

@Composable
fun MemberSection(title: String, members: List<User>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        members.forEach { member ->
            MemberItem(member)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun MemberItem(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar/initials
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.name.take(1).uppercase())
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User details
            Column {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}