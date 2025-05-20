package ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.compose.koinInject
import viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    navController: NavController,
    teamViewModel: TeamViewModel = koinInject()
) {
    var teamName by remember { mutableStateOf("") }
    val isLoading by teamViewModel.isLoading.collectAsState()
    val errorMessage by teamViewModel.errorMessage.collectAsState()
    val currentUser by teamViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUser?.teamMembership) {
        // If user now has a team, it means team creation was successful
        if (currentUser?.teamMembership?.teamId != null) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            teamViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Team") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Create Your Team",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                "As team creator, you will be the team president with full administrative powers.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = { Text("Team Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { teamViewModel.createTeam(teamName.trim()) },
                enabled = teamName.trim().isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create Team")
            }
        }
    }
}