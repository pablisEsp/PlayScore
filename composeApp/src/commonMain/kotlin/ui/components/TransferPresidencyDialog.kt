package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import data.model.User

@Composable
fun TransferPresidencyDialog(
    team: Team,
    members: List<User>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedUserId by remember { mutableStateOf("") }

    // Find eligible members (VP first, then captains, then players)
    val eligibleMembers = listOfNotNull(
        // VP first if exists
        members.find { it.id == team.vicePresidentId },
        // Then captains
        *members.filter { it.id in team.captainIds }.toTypedArray(),
        // Then regular players
        *members.filter {
            it.id != team.presidentId &&
            it.id != team.vicePresidentId &&
            it.id !in team.captainIds
        }.toTypedArray()
    )

    if (eligibleMembers.isEmpty()) {
        // No eligible members found
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Cannot Transfer Presidency") },
            text = { Text("No eligible team members found to transfer presidency to.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
        return
    }

    // Default selection to first eligible member
    LaunchedEffect(eligibleMembers) {
        if (selectedUserId.isEmpty() && eligibleMembers.isNotEmpty()) {
            selectedUserId = eligibleMembers.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Team Presidency") },
        text = {
            Column {
                Text("Select the new team president:")
                Spacer(modifier = Modifier.height(16.dp))

                eligibleMembers.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUserId = member.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUserId == member.id,
                            onClick = { selectedUserId = member.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(member.name)
                            Text(
                                "@${member.username}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedUserId) },
                enabled = selectedUserId.isNotEmpty()
            ) {
                Text("Transfer & Leave")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}