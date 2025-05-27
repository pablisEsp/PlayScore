package ui.team

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@Composable
fun TeamJoinRequests(
    team: Team,
    currentUserId: String,
    viewModel: TeamViewModel = koinInject()
) {
    val joinRequests by viewModel.teamJoinRequests.collectAsState()
    val isPresident = team.presidentId == currentUserId
    val isVicePresident = team.vicePresidentId == currentUserId

    // Only team leaders can see this section
    if (!isPresident && !isVicePresident) {
        return
    }

    LaunchedEffect(team.id) {
        viewModel.loadTeamJoinRequests()
    }

    if (joinRequests.isEmpty()) {
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Join Requests (${joinRequests.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

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

@Composable
fun JoinRequestItem(
    request: TeamViewModel.TeamJoinRequestWithUser,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.user.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "@${request.user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action buttons
        Row {
            TextButton(
                onClick = onReject,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reject")
            }

            Button(onClick = onApprove) {
                Text("Approve")
            }
        }
    }
}