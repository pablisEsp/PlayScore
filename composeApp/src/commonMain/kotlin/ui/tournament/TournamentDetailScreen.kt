package ui.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.ApplicationStatus
import data.model.Team
import data.model.TeamApplication
import data.model.TeamRole
import data.model.Tournament
import data.model.TournamentStatus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import repository.TournamentRepository
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

    val refreshKey = rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(navController) {
        val navBackStackEntry = navController.currentBackStackEntry
        val previousEntry = navController.previousBackStackEntry
        if (previousEntry?.destination?.route?.contains("applications") == true) {
            refreshKey.value++
        }
    }

    // Use the ViewModel's method to load tournament data
    LaunchedEffect(tournamentId, refreshKey.value) {
        tournamentViewModel.getTournamentById(tournamentId) // Call ViewModel method

        currentTeamFromState?.id?.let { teamId ->
            tournamentViewModel.checkTeamApplication(tournamentId, teamId)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val canApply = remember(currentUser, currentTeamFromState) {
        val team = currentTeamFromState
        val role = currentUser?.teamMembership?.role
        team != null && (role == TeamRole.PRESIDENT || role == TeamRole.VICE_PRESIDENT)
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        },
                        currentTeam = currentTeamFromState
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
    currentTeam: Team?,
    modifier: Modifier = Modifier // Default modifier
) {
    Box(modifier = Modifier.fillMaxSize()) { // Outer Box to hold content and dialog
        // Single scrollable Column for all content
        Column(
            modifier = modifier // Use the modifier passed to the function
                .fillMaxSize() // Ensure this Column fills the Box
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Content previously in the inner Column is now directly here
            Text(tournament.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))

            StatusChip(status = tournament.status)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(Icons.Filled.DateRange, "Start Date", formatDate(tournament.startDate))
                    InfoRow(Icons.Filled.DateRange, "End Date", formatDate(tournament.endDate))
                    InfoRow(label = "Max Teams", value = tournament.maxTeams.toString())
                    InfoRow(label = "Bracket Type", value = tournament.bracketType.name.replace('_', ' '))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Registration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
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
            if (currentTeamId != null) {
                if (tournament.teamIds.contains(currentTeamId)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Your team is registered for this tournament",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    teamApplication?.let { app ->
                        ApplicationStatusCard(application = app)
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
                                onClick = onApplyClick,  // Just call onApplyClick directly without size checking
                                modifier = Modifier.fillMaxWidth(),
                                enabled = tournament.teamIds.size < tournament.maxTeams
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
            }

            Spacer(modifier = Modifier.height(16.dp))
            // TODO: Add sections for Standings, Matches, Participants if applicable based on tournament status
        }

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