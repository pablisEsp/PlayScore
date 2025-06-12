package ui.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Tournament
import data.model.TournamentStatus
import navigation.EditTournament
import navigation.TournamentApplications
import org.koin.compose.koinInject
import repository.TournamentRepository
import ui.icons.Group_add
import viewmodel.AdminViewModel

@Composable
fun TournamentManagementScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = koinInject()
) {
    val tournaments by adminViewModel.tournaments.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val successMessage by adminViewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Get the repository inside the composable
    val tournamentRepository: TournamentRepository = koinInject()

    LaunchedEffect(Unit) {
        adminViewModel.loadTournaments()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading && tournaments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tournaments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No tournaments found")
                        }
                    }
                } else {
                    items(tournaments) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onEdit = {
                                navController.navigate(EditTournament(tournament.id))
                            },
                            onDelete = {
                                adminViewModel.deleteTournament(tournament.id)
                            },
                            onManageApplications = {
                                navController.navigate(TournamentApplications(tournament.id))
                            },
                            onGenerateMatches = {
                                adminViewModel.generateMatchesForTournament(tournament.id)
                            },
                            onPopulateTeams = {
                                adminViewModel.populateTournamentWithTeams(tournament.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCard(
    tournament: Tournament,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageApplications: () -> Unit,
    onGenerateMatches: () -> Unit,
    onPopulateTeams: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showGenerateMatchesConfirmation by remember { mutableStateOf(false) }
    var showPopulateTeamsConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = tournament.name,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = tournament.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                StatusChip(status = tournament.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "From: ${tournament.startDate}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "To: ${tournament.endDate}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Teams: ${tournament.teamIds.size}/${tournament.maxTeams}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                // Only show this for tournaments in REGISTRATION status
                if (tournament.status == TournamentStatus.REGISTRATION) {
                    IconButton(onClick = { showPopulateTeamsConfirmation = true }) {
                        Icon(
                            Group_add,
                            contentDescription = "Add Demo Teams",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Add Generate Matches button for tournaments in REGISTRATION status with at least 2 teams
                if (tournament.status == TournamentStatus.REGISTRATION && tournament.teamIds.size >= 2) {
                    IconButton(onClick = { showGenerateMatchesConfirmation = true }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Generate Matches",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onManageApplications) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Manage Applications"
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Tournament"
                    )
                }

                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Tournament",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Tournament") },
            text = { Text("Are you sure you want to delete this tournament?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Generate matches confirmation dialog
    if (showGenerateMatchesConfirmation) {
        AlertDialog(
            onDismissRequest = { showGenerateMatchesConfirmation = false },
            title = { Text("Generate Tournament Matches") },
            text = {
                Column {
                    Text("Are you sure you want to generate matches for this tournament?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This will set the tournament status to ACTIVE and create all tournament matches.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This action cannot be undone.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGenerateMatches()
                        showGenerateMatchesConfirmation = false
                    }
                ) {
                    Text("Generate Matches")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGenerateMatchesConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Populate teams confirmation dialog
    if (showPopulateTeamsConfirmation) {
        AlertDialog(
            onDismissRequest = { showPopulateTeamsConfirmation = false },
            title = { Text("Add Demo Teams") },
            text = {
                Column {
                    Text("This will add 6 demo teams to the tournament for testing purposes.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current team count: ${tournament.teamIds.size}/${tournament.maxTeams}")
                    if ((tournament.teamIds.size + 6) > tournament.maxTeams) {
                        Text("Warning: This will exceed the maximum team limit!",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPopulateTeams()
                        showPopulateTeamsConfirmation = false
                    }
                ) {
                    Text("Add Demo Teams")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPopulateTeamsConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}