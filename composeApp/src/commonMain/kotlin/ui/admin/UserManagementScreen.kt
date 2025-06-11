package ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.User
import data.model.UserRole
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import viewmodel.AdminViewModel

@Composable
fun UserManagementScreen(
    viewModel: AdminViewModel = koinInject()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentAdminUser by viewModel.currentUser.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    // State for Super Admin confirmation dialog
    var showSuperAdminConfirmDialog by remember { mutableStateOf(false) }
    var userToPromote by remember { mutableStateOf<User?>(null) }

    // Load users when screen is opened
    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
    }

    // Show error messages in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search users...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Filter users based on search query
                val filteredUsers = if (searchQuery.isBlank()) {
                    users
                } else {
                    users.filter { user ->
                        user.name.contains(searchQuery, ignoreCase = true) ||
                        user.email.contains(searchQuery, ignoreCase = true) ||
                        user.username.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserCard(
                            user = user,
                            currentAdminRole = currentAdminUser?.globalRole ?: UserRole.USER,
                            currentUser = currentAdminUser,
                            onRoleChange = { newRole ->
                                if (newRole == UserRole.SUPER_ADMIN) {
                                    userToPromote = user
                                    showSuperAdminConfirmDialog = true
                                } else {
                                    // For other roles, no confirmation needed
                                    scope.launch {
                                        val success = viewModel.updateUserRole(user.id, newRole)
                                        if (success) {
                                            snackbarHostState.showSnackbar("Updated ${user.name}'s role to ${newRole.name}")
                                        }
                                    }
                                }
                            },
                            onToggleBan = { isBanned ->
                                scope.launch {
                                    val success = viewModel.toggleUserBan(user.id, isBanned)
                                    val message = if (isBanned) "banned" else "unbanned"
                                    if (success) {
                                        snackbarHostState.showSnackbar("${user.name} has been $message")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Super Admin confirmation dialog
    if (showSuperAdminConfirmDialog && userToPromote != null) {
        AlertDialog(
            onDismissRequest = {
                showSuperAdminConfirmDialog = false
                userToPromote = null
            },
            title = { Text("Confirm Super Admin Role") },
            text = {
                Text("Are you sure you want to promote ${userToPromote?.name} to Super Admin? " +
                     "This will give them unrestricted access to the entire application.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            userToPromote?.let { user ->
                                val success = viewModel.updateUserRole(user.id, UserRole.SUPER_ADMIN)
                                if (success) {
                                    snackbarHostState.showSnackbar("Updated ${user.name}'s role to SUPER_ADMIN")
                                }
                            }
                        }
                        showSuperAdminConfirmDialog = false
                        userToPromote = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuperAdminConfirmDialog = false
                        userToPromote = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    currentAdminRole: UserRole,
    currentUser: User?,
    onRoleChange: (UserRole) -> Unit,
    onToggleBan: (Boolean) -> Unit
) {
    val isBanned = user.isBanned ?: false
    var showDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Role and ban status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val roleColor = when (user.globalRole) {
                            UserRole.SUPER_ADMIN -> MaterialTheme.colorScheme.tertiary
                            UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                            UserRole.USER -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Text(
                            text = user.globalRole.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = roleColor
                        )

                        if (isBanned) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BANNED",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Role management dropdown
                Box {
                    IconButton(onClick = { showDropdown = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "User options"
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        // Role management section
                        Text(
                            text = "Change Role",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Only show roles the current admin can assign
                        // SUPER_ADMIN can assign all roles
                        // ADMIN can only assign USER role
                        if (currentAdminRole == UserRole.SUPER_ADMIN) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Super Admin" +
                                        if (user.globalRole == UserRole.SUPER_ADMIN) " ✓" else ""
                                    )
                                },
                                onClick = {
                                    showDropdown = false
                                    onRoleChange(UserRole.SUPER_ADMIN)
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Admin" +
                                        if (user.globalRole == UserRole.ADMIN) " ✓" else ""
                                    )
                                },
                                onClick = {
                                    showDropdown = false
                                    onRoleChange(UserRole.ADMIN)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "User" +
                                    if (user.globalRole == UserRole.USER) " ✓" else ""
                                )
                            },
                            onClick = {
                                showDropdown = false
                                onRoleChange(UserRole.USER)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Ban management section
                        Text(
                            text = "Actions",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Toggle ban status - don't show if it's the current user
                        if (user.id != currentUser?.id) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isBanned) "Unban User" else "Ban User",
                                        color = if (isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showDropdown = false
                                    onToggleBan(!isBanned)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}