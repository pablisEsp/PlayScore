package ui.tournament

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.ApplicationStatus
import data.model.Team
import data.model.TeamApplication
import data.model.Tournament
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import repository.TournamentRepository
import viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentApplicationsScreen(
    navController: NavController,
    tournamentId: String,
    adminViewModel: AdminViewModel = koinInject(),
    database: FirebaseDatabaseInterface = koinInject(),
    tournamentRepository: TournamentRepository = koinInject()
) {
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var applications by remember { mutableStateOf<List<TeamApplication>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // Start with loading state
    var teams by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load tournament and applications
    LaunchedEffect(tournamentId) {
        try {
            isLoading = true

            // Load tournament data
            val fetchedTournament = tournamentRepository.getTournamentById(tournamentId)
            if (fetchedTournament == null) {
                errorMessage = "Tournament not found"
                isLoading = false
                return@LaunchedEffect
            }

            tournament = fetchedTournament

            // Load team applications
            val fetchedApplications = tournamentRepository.getTeamApplications(tournamentId)
            applications = fetchedApplications

            // Load team names for all applications
            val teamIds = fetchedApplications.map { it.teamId }.distinct()
            val teamsMap = mutableMapOf<String, String>()

            teamIds.forEach { teamId ->
                try {
                    val team = database.getDocument<Team>("teams/$teamId")
                    if (team != null) {
                        teamsMap[teamId] = team.name
                    }
                } catch (e: Exception) {
                    println("Error loading team $teamId: ${e.message}")
                }
            }

            teams = teamsMap
        } catch (e: Exception) {
            errorMessage = "Failed to load data: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Show error message if any
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tournament Applications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            applications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No applications found for this tournament")
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    // Tournament info header
                    tournament?.let { tourney ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = tourney.name,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Applications: ${applications.size} / ${tourney.maxTeams}")
                                Text("Approved: ${applications.count { it.status == ApplicationStatus.APPROVED }}")
                                Text("Pending: ${applications.count { it.status == ApplicationStatus.PENDING }}")
                            }
                        }
                    }

                    // Applications list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(applications) { application ->
                            ApplicationCard(
                                application = application,
                                teamName = teams[application.teamId] ?: "Unknown Team",
                                onApprove = {
                                    scope.launch {
                                        try {
                                            val success = tournamentRepository.updateApplicationStatus(
                                                application.id,
                                                ApplicationStatus.APPROVED
                                            )
                                            if (success) {
                                                // Update local state to reflect change
                                                applications = applications.map { app ->
                                                    if (app.id == application.id) app.copy(status = ApplicationStatus.APPROVED)
                                                    else app
                                                }
                                                snackbarHostState.showSnackbar("Team approved")
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to approve team")
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                        }
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        try {
                                            val success = tournamentRepository.updateApplicationStatus(
                                                application.id,
                                                ApplicationStatus.REJECTED
                                            )
                                            if (success) {
                                                // Update local state to reflect change
                                                applications = applications.map { app ->
                                                    if (app.id == application.id) app.copy(status = ApplicationStatus.REJECTED)
                                                    else app
                                                }
                                                snackbarHostState.showSnackbar("Team rejected")
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to reject team")
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    application: TeamApplication,
    teamName: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Applied: ${application.appliedAt}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            val statusColor = when(application.status) {
                ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
                ApplicationStatus.PENDING -> MaterialTheme.colorScheme.tertiary
            }

            Text(
                text = "Status: ${application.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )

            // Action buttons for pending applications
            if (application.status == ApplicationStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Reject")
                        Spacer(Modifier.width(4.dp))
                        Text("Reject")
                    }

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Approve")
                        Spacer(Modifier.width(4.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}