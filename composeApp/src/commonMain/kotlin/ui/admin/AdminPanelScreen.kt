package ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.compose.koinInject
import viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: AdminViewModel = koinInject()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // Check if user is admin
    LaunchedEffect(Unit) {
        viewModel.checkAdminAccess()
    }

    val isAdmin = currentUser?.globalRole?.name?.contains("ADMIN") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!isAdmin) {
                // Show access denied message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Access denied. Admin privileges required.")
                }
            } else {
                // Admin dashboard content
                Text(
                    "Admin Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Tournament management section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Tournament Management",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { navController.navigate("navigation.CreateTournament") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create New Tournament")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { navController.navigate("navigation.TournamentManagement") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Tournaments")
                        }
                    }
                }

                // User management section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "User Management",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* Navigate to user management */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Users")
                        }
                    }
                }
            }
        }
    }
}