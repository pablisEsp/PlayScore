package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.Team
import data.model.TeamRole
import data.model.User

@Composable
fun RoleManagementDropdown(
    user: User,
    team: Team,
    currentUserIsPresident: Boolean,
    onRoleChanged: (User, TeamRole) -> Unit,
    onKickUser: (User) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showPresidencyFirstConfirmation by remember { mutableStateOf(false) }
    var showPresidencyFinalConfirmation by remember { mutableStateOf(false) }
    var showKickConfirmation by remember { mutableStateOf(false) }

    // Determine the user's current role
    val currentRole = user.teamMembership?.role ?: run {
        when (user.id) {
            team.presidentId -> TeamRole.PRESIDENT
            team.vicePresidentId -> TeamRole.VICE_PRESIDENT
            in team.captainIds -> TeamRole.CAPTAIN
            else -> TeamRole.PLAYER
        }
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Manage member")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Role management section
            Text(
                "Change Role",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Only show President option if current user is President
            if (currentUserIsPresident && currentRole != TeamRole.PRESIDENT) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "President" +
                                    if (currentRole == TeamRole.PRESIDENT) " ✓" else ""
                        )
                    },
                    onClick = {
                        expanded = false
                        showPresidencyFirstConfirmation = true
                    }
                )
            }

            // Other role options
            DropdownMenuItem(
                text = {
                    Text(
                        "Vice President" +
                                if (currentRole == TeamRole.VICE_PRESIDENT) " ✓" else ""
                    )
                },
                onClick = {
                    expanded = false
                    onRoleChanged(user, TeamRole.VICE_PRESIDENT)
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "Captain" +
                                if (currentRole == TeamRole.CAPTAIN) " ✓" else ""
                    )
                },
                onClick = {
                    expanded = false
                    onRoleChanged(user, TeamRole.CAPTAIN)
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "Player" +
                                if (currentRole == TeamRole.PLAYER) " ✓" else ""
                    )
                },
                onClick = {
                    expanded = false
                    onRoleChanged(user, TeamRole.PLAYER)
                }
            )

            // Divider between sections
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Team management action section
            Text(
                "Actions",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Kick user option
            DropdownMenuItem(
                text = { Text("Kick from Team") },
                onClick = {
                    expanded = false
                    showKickConfirmation = true
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }

    // Presidency transfer confirmations
    if (showPresidencyFirstConfirmation) {
        AlertDialog(
            onDismissRequest = { showPresidencyFirstConfirmation = false },
            title = { Text("Transfer Presidency") },
            text = {
                Text("Are you sure you want to make ${user.name} the team president? " +
                        "You will lose your ability to manage the team and will become a regular player.")
            },
            confirmButton = {
                Button(onClick = {
                    showPresidencyFirstConfirmation = false
                    showPresidencyFinalConfirmation = true
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresidencyFirstConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPresidencyFinalConfirmation) {
        AlertDialog(
            onDismissRequest = { showPresidencyFinalConfirmation = false },
            title = { Text("Final Confirmation") },
            text = {
                Text("WARNING: This action cannot be undone! " +
                        "Once you transfer presidency to ${user.name}, only they will have " +
                        "the ability to manage the team.")
            },
            confirmButton = {
                Button(onClick = {
                    showPresidencyFinalConfirmation = false
                    onRoleChanged(user, TeamRole.PRESIDENT)
                }) {
                    Text("Transfer Presidency")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresidencyFinalConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Kick user confirmation
    if (showKickConfirmation) {
        AlertDialog(
            onDismissRequest = { showKickConfirmation = false },
            title = { Text("Kick User") },
            text = {
                Text("Are you sure you want to remove ${user.name} from the team? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showKickConfirmation = false
                        onKickUser(user)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Kick User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKickConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}