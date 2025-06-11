package ui.tournament

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.BracketType
import data.model.MatchStatus
import data.model.Tournament
import data.model.TournamentMatch
import org.koin.compose.koinInject
import viewmodel.TournamentViewModel

@Composable
fun TournamentBracketScreen(
    navController: NavController,
    tournamentId: String,
    tournamentViewModel: TournamentViewModel = koinInject()
) {
    val tournament by tournamentViewModel.currentTournament.collectAsState()
    val tournamentMatches by tournamentViewModel.tournamentMatches.collectAsState()
    val isLoading by tournamentViewModel.isLoading.collectAsState()
    val errorMessage by tournamentViewModel.errorMessage.collectAsState()
    val teamNamesMap by tournamentViewModel.teamNames.collectAsState()
    val userCanReport by tournamentViewModel.userCanReportScore.collectAsState()

    // For score reporting dialog
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedMatch by remember { mutableStateOf<TournamentMatch?>(null) }

    // For displaying success/error messages
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        tournamentViewModel.getCurrentUserData()
    }

    LaunchedEffect(tournamentId) {
        tournamentViewModel.getTournamentById(tournamentId)
        tournamentViewModel.loadTournamentMatches(tournamentId)
        tournamentViewModel.loadTeamNames(tournamentId)
        tournamentViewModel.checkUserCanReportScore(tournamentId)
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage ?: "An error occurred")
            tournamentViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (isLoading && tournament == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (tournament == null) {
                Text(
                    text = "Tournament not found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when (tournament?.bracketType) {
                    BracketType.SINGLE_ELIMINATION -> SingleEliminationBracket(
                        tournament = tournament!!,
                        matches = tournamentMatches,
                        teamNamesMap = teamNamesMap,
                        userCanReport = userCanReport,
                        onMatchClick = { match ->
                            selectedMatch = match
                            showReportDialog = true
                        }
                    )
                    BracketType.DOUBLE_ELIMINATION -> DoubleEliminationBracket(
                        tournament = tournament!!,
                        matches = tournamentMatches,
                        teamNamesMap = teamNamesMap,
                        userCanReport = userCanReport,
                        onMatchClick = { match ->
                            selectedMatch = match
                            showReportDialog = true
                        }
                    )
                    BracketType.ROUND_ROBIN -> RoundRobinBracket(
                        tournament = tournament!!,
                        matches = tournamentMatches,
                        teamNamesMap = teamNamesMap,
                        userCanReport = userCanReport,
                        onMatchClick = { match ->
                            selectedMatch = match
                            showReportDialog = true
                        }
                    )
                    null -> Text(
                        text = "Unknown tournament format",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Score reporting dialog
    if (showReportDialog && selectedMatch != null) {
        ScoreReportDialog(
            match = selectedMatch!!,
            teamNamesMap = teamNamesMap,
            onDismiss = {
                showReportDialog = false
                selectedMatch = null
            },
            onSubmit = { homeScore, awayScore ->
                tournamentViewModel.reportMatchScore(
                    selectedMatch!!.id,
                    homeScore,
                    awayScore
                )
                showReportDialog = false
                selectedMatch = null
            }
        )
    }
}

@Composable
fun SingleEliminationBracket(
    tournament: Tournament,
    matches: List<TournamentMatch>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit
) {
    // Organize matches by round
    val matchesByRound = matches.groupBy { it.round }
    val maxRound = matches.maxOfOrNull { it.round } ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Single Elimination Bracket",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Draw bracket rounds horizontally
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // Render each round of matches
                for (round in 1..maxRound) {
                    val roundMatches = matchesByRound[round] ?: emptyList()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            // Increase spacing in later rounds
                            (20 * (round - 1) + 16).dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(180.dp)
                    ) {
                        Text(
                            text = when (round) {
                                maxRound -> "Final"
                                maxRound - 1 -> "Semifinals"
                                maxRound - 2 -> "Quarterfinals"
                                else -> "Round $round"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        roundMatches.forEach { match ->
                            MatchCard(
                                match = match,
                                teamNamesMap = teamNamesMap,
                                userCanReport = userCanReport,
                                onClick = { onMatchClick(match) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoubleEliminationBracket(
    tournament: Tournament,
    matches: List<TournamentMatch>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit
) {
    // Group matches by specific bracket sections
    val winnersMatches = matches.filter { it.id.contains("-W-") }
    val losersMatches = matches.filter { it.id.contains("-L-") }
    val finalMatches = matches.filter { it.id.contains("-F-") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Double Elimination Bracket",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Winners bracket section
        Text(
            text = "Winners Bracket",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        BracketSection(
            matches = winnersMatches,
            teamNamesMap = teamNamesMap,
            userCanReport = userCanReport,
            onMatchClick = onMatchClick
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Losers bracket section
        Text(
            text = "Losers Bracket",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        BracketSection(
            matches = losersMatches,
            teamNamesMap = teamNamesMap,
            userCanReport = userCanReport,
            onMatchClick = onMatchClick
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Finals section
        if (finalMatches.isNotEmpty()) {
            Text(
                text = "Finals",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            finalMatches.forEach { match ->
                MatchCard(
                    match = match,
                    teamNamesMap = teamNamesMap,
                    userCanReport = userCanReport,
                    onClick = { onMatchClick(match) },
                    isFinal = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun BracketSection(
    matches: List<TournamentMatch>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit
) {
    val matchesByRound = matches.groupBy { it.round }
    val rounds = matchesByRound.keys.sorted()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height((matches.size * 80 + 32).dp)
    ) {
        rounds.forEach { round ->
            val roundMatches = matchesByRound[round] ?: emptyList()
            item {
                Text(
                    text = "Round $round",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roundMatches.forEach { match ->
                        MatchCard(
                            match = match,
                            teamNamesMap = teamNamesMap,
                            userCanReport = userCanReport,
                            onClick = { onMatchClick(match) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoundRobinBracket(
    tournament: Tournament,
    matches: List<TournamentMatch>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit
) {
    // Group matches by round
    val matchesByRound = matches.groupBy { it.round }
    val rounds = matchesByRound.keys.sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Round Robin Tournament",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Display each round
        rounds.forEach { round ->
            val roundMatches = matchesByRound[round] ?: emptyList()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Round $round",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    roundMatches.forEach { match ->
                        MatchCard(
                            match = match,
                            teamNamesMap = teamNamesMap,
                            userCanReport = userCanReport,
                            onClick = { onMatchClick(match) },
                            compact = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: TournamentMatch,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
    isFinal: Boolean = false
) {
    // Get team names or placeholders
    val homeTeamName = if (match.homeTeamId.isEmpty()) "TBD" else
        teamNamesMap[match.homeTeamId] ?: "Team ${match.homeTeamId.takeLast(4)}"

    val awayTeamName = if (match.awayTeamId.isEmpty()) "TBD" else
        teamNamesMap[match.awayTeamId] ?: "Team ${match.awayTeamId.takeLast(4)}"

    val cardShape = RoundedCornerShape(8.dp)
    val cardModifier = if (isFinal) {
        Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = cardShape
            )
    } else {
        Modifier.width(if (compact) 280.dp else 180.dp)
    }

    Card(
        onClick = onClick,
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = when (match.status) {
                MatchStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                MatchStatus.IN_PROGRESS -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                MatchStatus.SCHEDULED -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Date
            Text(
                text = "Match #${match.matchNumber} - ${match.scheduledDate}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )

            // Teams
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = homeTeamName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (match.homeTeamScore > match.awayTeamScore &&
                                          match.status == MatchStatus.COMPLETED)
                                     FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Scores
                if (match.status == MatchStatus.COMPLETED) {
                    Text(
                        text = "${match.homeTeamScore} - ${match.awayTeamScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Away team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = awayTeamName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (match.awayTeamScore > match.homeTeamScore &&
                                          match.status == MatchStatus.COMPLETED)
                                     FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Status and action indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when(match.status) {
                        MatchStatus.SCHEDULED -> "Upcoming"
                        MatchStatus.IN_PROGRESS -> "In Progress"
                        MatchStatus.COMPLETED -> "Completed"
                        MatchStatus.IN_PROGRESS -> "Score Disputed"
                        else -> match.status.toString()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when(match.status) {
                        MatchStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        MatchStatus.IN_PROGRESS -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (userCanReport && match.status != MatchStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Report Score",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreReportDialog(
    match: TournamentMatch,
    teamNamesMap: Map<String, String>,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int) -> Unit
) {
    var homeScore by remember { mutableStateOf(match.homeScore?.toString() ?: "") }
    var awayScore by remember { mutableStateOf(match.awayScore?.toString() ?: "") }

    // Get team names from the map
    val homeTeamName = if (match.homeTeamId.isEmpty()) "TBD" else
        teamNamesMap[match.homeTeamId] ?: "Team ${match.homeTeamId.takeLast(4)}"
    val awayTeamName = if (match.awayTeamId.isEmpty()) "TBD" else
        teamNamesMap[match.awayTeamId] ?: "Team ${match.awayTeamId.takeLast(4)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Match Result") },
        text = {
            Column {
                // Status messages
                when {
                    match.status == MatchStatus.COMPLETED -> {
                        Text(
                            "This match has been completed with the following score:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    match.status == MatchStatus.IN_PROGRESS -> {
                        Text(
                            "This match has conflicting reported scores. Your submission will resolve the dispute.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    match.homeTeamConfirmed || match.awayTeamConfirmed -> {
                        Text(
                            "One team has already reported a score. Your submission will finalize the result.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = homeTeamName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = homeScore,
                        onValueChange = { homeScore = it },
                        label = { Text("Score") },
                        singleLine = true,
                        enabled = match.status != MatchStatus.COMPLETED,
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = awayTeamName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = awayScore,
                        onValueChange = { awayScore = it },
                        label = { Text("Score") },
                        singleLine = true,
                        enabled = match.status != MatchStatus.COMPLETED,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val home = homeScore.toIntOrNull() ?: 0
                        val away = awayScore.toIntOrNull() ?: 0
                        onSubmit(home, away)
                    } catch (e: Exception) {
                        // Handle invalid input
                    }
                },
                enabled = match.status != MatchStatus.COMPLETED
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}