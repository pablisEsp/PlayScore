package ui.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.ApplicationStatus
import data.model.TeamApplication
import data.model.TeamRole
import data.model.Tournament
import data.model.TournamentStatus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import viewmodel.TeamViewModel
import viewmodel.TournamentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    navController: NavController,
    tournamentId: String,
    tournamentViewModel: TournamentViewModel = koinInject(),
    teamViewModel: TeamViewModel = koinInject()
) {
    val tournament by tournamentViewModel.currentTournament.collectAsState()
    val isLoading by tournamentViewModel.isLoading.collectAsState()
    val errorMessage by tournamentViewModel.errorMessage.collectAsState()
    val currentTeamFromState by teamViewModel.currentTeam.collectAsState()
    val currentUser by teamViewModel.currentUser.collectAsState()
    val teamApplication by tournamentViewModel.teamApplication.collectAsState()
    val successMessage by tournamentViewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val canApply = remember(currentUser, currentTeamFromState) {
        val team = currentTeamFromState
        val role = currentUser?.teamMembership?.role
        team != null && (role == TeamRole.PRESIDENT || role == TeamRole.VICE_PRESIDENT)
    }

    LaunchedEffect(tournamentId, currentTeamFromState?.id) {
        tournamentViewModel.getTournamentById(tournamentId)
        val team = currentTeamFromState
        if (team != null) {
            tournamentViewModel.checkTeamApplication(tournamentId, team.id)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
            tournamentViewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
            tournamentViewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(tournament?.name ?: "Tournament Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && tournament == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (tournament == null) {
                Text(
                    "Tournament not found or error loading.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                tournament?.let {
                    TournamentDetails(
                        tournament = it,
                        teamApplication = teamApplication,
                        canApply = canApply,
                        currentTeamId = currentTeamFromState?.id,
                        onApplyClick = {
                            currentTeamFromState?.id?.let { teamId ->
                                tournamentViewModel.applyToTournament(it, teamId)
                            }
                        },
                        onWithdrawClick = {
                            teamApplication?.id?.let { appId ->
                                currentTeamFromState?.id?.let { teamId ->
                                    tournamentViewModel.withdrawApplication(appId, teamId)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentDetails(
    tournament: Tournament,
    teamApplication: TeamApplication?,
    canApply: Boolean,
    currentTeamId: String?,
    onApplyClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(tournament.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))

        StatusChip(status = tournament.status)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // InfoRow(Icons.Filled.Games, "Game", tournament.game) // Example: Assuming 'game' was a field
                // InfoRow(Icons.Filled.Computer, "Platform", tournament.platform)
                // InfoRow(Icons.Filled.Public, "Region", tournament.region)
                InfoRow(Icons.Filled.DateRange, "Start Date", formatDate(tournament.startDate))
                InfoRow(Icons.Filled.DateRange, "End Date", formatDate(tournament.endDate))
                InfoRow(label = "Max Teams", value = tournament.maxTeams.toString())
                InfoRow(label = "Bracket Type", value = tournament.bracketType.name.replace('_', ' '))
                // InfoRow(Icons.Filled.AttachMoney, "Entry Fee", "${tournament.entryFee} ${tournament.currency}")
                // InfoRow(Icons.Filled.EmojiEvents, "Prize Pool", "${tournament.prizePool} ${tournament.currency}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Registration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // InfoRow(Icons.Filled.EventBusy, "Deadline", formatDate(tournament.registrationDeadline))
                InfoRow(label = "Teams Registered", value = "${tournament.teamIds.size} / ${tournament.maxTeams}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (tournament.description.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tournament.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Application Status and Actions
        if (currentTeamId != null) { // Only show application section if user is in a team
            teamApplication?.let { app ->
                ApplicationStatusCard(application = app)
                Spacer(modifier = Modifier.height(8.dp))
                if (app.status == ApplicationStatus.PENDING && canApply) {
                    Button(
                        onClick = onWithdrawClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Withdraw Application")
                    }
                }
            } ?: run { // No application yet
                if (tournament.status == TournamentStatus.REGISTRATION && canApply) {
                    Button(
                        onClick = onApplyClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = tournament.teamIds.size < tournament.maxTeams // Disable if full
                    ) {
                        Text(if (tournament.teamIds.size < tournament.maxTeams) "Apply to Tournament" else "Tournament Full")
                    }
                } else if (tournament.status != TournamentStatus.REGISTRATION) {
                    Text("Registration for this tournament is closed.", style = MaterialTheme.typography.bodyMedium)
                } else if (!canApply) {
                     Text("Only team leaders (President or Vice-President) can apply.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // TODO: Add sections for Standings, Matches, Participants if applicable based on tournament status
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector? = null, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
fun StatusChip(status: TournamentStatus) {
    val (backgroundColor, textColor) = when (status) {
        TournamentStatus.UPCOMING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TournamentStatus.REGISTRATION -> Color(0xFF4CAF50) to Color.White // Green
        TournamentStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer // Changed ONGOING to ACTIVE
        TournamentStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TournamentStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        // Removed 'else' as all enum cases are covered
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp
    ) {
        Text(
            text = status.name.replace('_', ' '),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ApplicationStatusCard(application: TeamApplication) {
    val (backgroundColor, textColor, message) = when (application.status) {
        ApplicationStatus.PENDING -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Application Pending")
        ApplicationStatus.APPROVED -> Triple(Color(0xFF4CAF50), Color.White, "Application Approved") // Green
        ApplicationStatus.REJECTED -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Application Rejected")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor) // Corrected parameter
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Application Status", style = MaterialTheme.typography.titleSmall.copy(color = textColor), fontWeight = FontWeight.Bold) // Apply color directly or via copy
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium.copy(color = textColor)) // Apply color
            Text("Applied on: ${formatDate(application.appliedAt)}", style = MaterialTheme.typography.bodySmall.copy(color = textColor.copy(alpha = 0.7f))) // Apply color
        }
    }
}

// Helper function to format date strings
private fun formatDate(dateString: String): String {
    return try {
        if (dateString.contains("T")) {
            dateString.substringBefore("T")
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}