package ui.team

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import data.model.TeamJoinRequest
import data.model.User
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@Composable
fun TeamJoinRequestsTab(
    team: Team,
    viewModel: TeamViewModel = koinInject()
) {
    val requests by viewModel.teamJoinRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTeamJoinRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Team Join Requests",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No pending join requests")
            }
        } else {
            LazyColumn {
                items(requests) { requestWithUser ->
                    JoinRequestItem(
                        request = requestWithUser.request,
                        user = requestWithUser.user,
                        onApprove = { viewModel.handleJoinRequest(requestWithUser.request.id, true) },
                        onReject = { viewModel.handleJoinRequest(requestWithUser.request.id, false) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun JoinRequestItem(
    request: TeamJoinRequest,
    user: User,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(user.name.take(1).uppercase())
        }

        Spacer(modifier = Modifier.width(16.dp))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleMedium)
            Text("@${user.username}", style = MaterialTheme.typography.bodyMedium)
        }

        // Action buttons
        Row {
            OutlinedButton(
                onClick = onReject,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Reject")
            }

            Button(onClick = onApprove) {
                Text("Approve")
            }
        }
    }
}