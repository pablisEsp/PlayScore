package ui.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.ApplicationStatus
import data.model.TeamRole
import data.model.TeamApplication
import data.model.Tournament
import navigation.TeamTournamentDetail
import org.koin.compose.koinInject
import viewmodel.TeamViewModel
import viewmodel.TournamentViewModel

@Composable
fun TeamTournamentScreen(
    navController: NavController,
    teamViewModel: TeamViewModel,
    tournamentViewModel: TournamentViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val currentTeam by teamViewModel.currentTeam.collectAsState()
    val currentUser by teamViewModel.currentUser.collectAsState()
    val teamTournaments by tournamentViewModel.teamTournaments.collectAsState()
    val availableTournaments by tournamentViewModel.availableTournaments.collectAsState()
    val applications by tournamentViewModel.teamApplications.collectAsState()
    val isLoadingTeam by teamViewModel.isLoading.collectAsState()
    val isLoadingTournaments by tournamentViewModel.isLoading.collectAsState()
    val errorMessage by tournamentViewModel.errorMessage.collectAsState()
    val successMessage by tournamentViewModel.successMessage.collectAsState()
    var showSizeWarningDialog by remember { mutableStateOf(false) }
    var tournamentToApplyTo by remember { mutableStateOf<Tournament?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val isTeamLeader = remember(currentUser) {
        val role = currentUser?.teamMembership?.role
        role == TeamRole.PRESIDENT || role == TeamRole.VICE_PRESIDENT
    }

    // Load tournament data when team is available
    LaunchedEffect(currentTeam?.id) {
        currentTeam?.id?.let { teamId ->
            tournamentViewModel.loadTeamTournaments(teamId)
            tournamentViewModel.loadAvailableTournaments(teamId)
            tournamentViewModel.loadTeamApplications(teamId)
        }
    }

    // Handle messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            tournamentViewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            tournamentViewModel.clearSuccessMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoadingTeam && currentTeam == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            currentTeam == null -> {
                NoTeamViewInTournamentScreen()
            }
            else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Your existing LazyColumn content
                        if (isLoadingTournaments && teamTournaments.isEmpty() &&
                            availableTournaments.isEmpty() && applications.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Get pending applications
                                val pendingApplications = applications.filter { it.status == ApplicationStatus.PENDING }

                                // Pending Applications Section
                                if (pendingApplications.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Pending Applications",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    items(pendingApplications) { application ->
                                        ApplicationItem(
                                            application = application,
                                            onWithdraw = {
                                                if (isTeamLeader) {
                                                    currentTeam?.id?.let { teamId ->
                                                        tournamentViewModel.withdrawApplication(application.id, teamId)
                                                    }
                                                }
                                            },
                                            isTeamLeader = isTeamLeader
                                        )
                                    }

                                    item { Spacer(modifier = Modifier.height(8.dp)) }
                                }

                                // Current Tournaments Section
                                item {
                                    Text(
                                        text = "My Tournaments",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (teamTournaments.isNotEmpty()) {
                                    items(teamTournaments) { tournament ->
                                        TournamentItem(
                                            tournament = tournament,
                                            onClick = {
                                                navController.navigate(TeamTournamentDetail(tournamentId = tournament.id))
                                            }
                                        )
                                    }
                                } else {
                                    item {
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "You're not participating in any tournaments yet",
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(16.dp)) }

                                // Available Tournaments Section
                                if (availableTournaments.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Available Tournaments",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    items(availableTournaments) { tournament ->
                                        AvailableTournamentItem(
                                            tournament = tournament,
                                            // Change the onApply lambda in AvailableTournamentItem to check team size
                                            onApply = {
                                                if (isTeamLeader) {
                                                    currentTeam?.let { team ->
                                                        // Calculate team size
                                                        val teamSize = 1 + (if (team.vicePresidentId != null) 1 else 0) +
                                                                team.captainIds.size + team.playerIds.size

                                                        if (teamSize < 5) {
                                                            // Show warning dialog
                                                            tournamentToApplyTo = tournament
                                                            showSizeWarningDialog = true
                                                        } else {
                                                            // Team size is good, apply directly
                                                            team.id.let { teamId ->
                                                                tournamentViewModel.applyToTournament(tournament, teamId)
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = { navController.navigate(TeamTournamentDetail(tournamentId = tournament.id)) },
                                            isTeamLeader = isTeamLeader
                                        )
                                    }
                                } else if (pendingApplications.isEmpty() && teamTournaments.isEmpty()) {
                                    item {
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "No tournaments are currently available for registration",
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Add this dialog at the end of the Box in TeamTournamentScreen
                        if (showSizeWarningDialog && tournamentToApplyTo != null) {
                            AlertDialog(
                                onDismissRequest = {
                                    showSizeWarningDialog = false
                                    tournamentToApplyTo = null
                                },
                                title = { Text("Team Size Warning") },
                                text = {
                                    Text("Your team currently has fewer than 5 members. Tournament participation requires at least 5 players. Are you sure you want to proceed with your application?\n\nFailure to field a complete team on tournament day may result in penalties.")
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showSizeWarningDialog = false
                                            currentTeam?.id?.let { teamId ->
                                                tournamentToApplyTo?.let { tournament ->
                                                    tournamentViewModel.applyToTournament(tournament, teamId)
                                                }
                                            }
                                            tournamentToApplyTo = null
                                        }
                                    ) {
                                        Text("Proceed Anyway")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = {
                                        showSizeWarningDialog = false
                                        tournamentToApplyTo = null
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }

            }
        }
    }
}

@Composable
private fun NoTeamViewInTournamentScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "You need to be in a team to view tournaments",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TournamentItem(
    tournament: Tournament,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = tournament.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${tournament.status.name.replace('_', ' ')}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Teams: ${tournament.teamIds.size}/${tournament.maxTeams}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                Text(
                    text = "Start: ${tournament.startDate}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "End: ${tournament.endDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailableTournamentItem(
    tournament: Tournament,
    onApply: () -> Unit,
    onClick: () -> Unit,
    isTeamLeader: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = tournament.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Teams: ${tournament.teamIds.size}/${tournament.maxTeams}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                Text(
                    text = "Start: ${tournament.startDate}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "End: ${tournament.endDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isTeamLeader) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply")
                }
            } else {
                Text(
                    text = "Only team leaders can apply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun ApplicationItem(
    application: TeamApplication,
    onWithdraw: () -> Unit,
    isTeamLeader: Boolean
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Application for Tournament",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Applied: ${application.appliedAt}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Status: ${application.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when(application.status) {
                            ApplicationStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                            ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                            ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                if (application.status == ApplicationStatus.PENDING && isTeamLeader) {
                    Button(
                        onClick = onWithdraw,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Withdraw")
                    }
                }
            }
        }
    }
}