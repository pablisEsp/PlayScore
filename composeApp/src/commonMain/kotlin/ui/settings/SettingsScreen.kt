package ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import data.model.UserRole
import org.koin.compose.koinInject
import viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: UserViewModel = koinInject()
    val currentUser by viewModel.currentUser.collectAsState()

    val isAdmin = currentUser?.globalRole == UserRole.ADMIN ||
                 currentUser?.globalRole == UserRole.SUPER_ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text("Account Settings") },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable { /* Navigate to account settings */ }
            )

            ListItem(
                headlineContent = { Text("Notifications") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable { /* Navigate to notifications settings */ }
            )

            // Admin section
            AdminSection(navController, isAdmin)
        }
    }
}

@Composable
fun AdminSection(
    navController: NavController,
    isAdmin: Boolean
) {
    if (isAdmin) {
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Admin Panel") },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable { navController.navigate("navigation.AdminPanel") }
        )
    }
}