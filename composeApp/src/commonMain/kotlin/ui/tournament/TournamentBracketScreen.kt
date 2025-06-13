package ui.tournament

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.BracketType
import data.model.MatchStatus
import data.model.Tournament
import data.model.TournamentMatch
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

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
    val scope = rememberCoroutineScope()

    // Organize matches by round
    val matchesByRound = matches.groupBy { it.round }
    val maxRound = matches.maxOfOrNull { it.round } ?: 0

    // Get available rounds
    val rounds = matchesByRound.keys.sorted()

    // Determine the initial page based on match status
    val initialPage = remember {
        // Find the first round that has incomplete matches
        rounds.indexOfFirst { round ->
            val roundMatches = matchesByRound[round] ?: emptyList()
            roundMatches.any { it.status != MatchStatus.COMPLETED }
        }.let { index ->
            // If all matches are complete or no incomplete match found, show the final round
            if (index == -1) rounds.size - 1 else index
        }
    }

    // State for pager
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { rounds.size }
    )

    // For round names
    val getRoundName = { round: Int ->
        when (round) {
            maxRound -> "Final"
            maxRound - 1 -> "Semifinals"
            maxRound - 2 -> "Quarterfinals"
            else -> "Round $round"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Single Elimination Bracket",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (rounds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matches available",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 8.dp,
                divider = { /* No divider */ },
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                rounds.forEachIndexed { index, round ->
                    val roundName = getRoundName(round)
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = roundName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bracket content with horizontal pager for swiping
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val selectedRound = rounds.getOrNull(page)
                if (selectedRound != null) {
                    val roundMatches = matchesByRound[selectedRound] ?: emptyList()

                    // Main content: matches with connection lines
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp)
                    ) {
                        // Header showing current round
                        Text(
                            text = getRoundName(selectedRound),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (roundMatches.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matches scheduled for this round yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Display matches with connection lines for the selected round
                            BracketRoundWithConnections(
                                roundMatches = roundMatches,
                                teamNamesMap = teamNamesMap,
                                userCanReport = userCanReport,
                                onMatchClick = onMatchClick,
                                isLastRound = selectedRound == maxRound,
                                showNextRoundConnections = selectedRound < maxRound
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BracketRoundWithConnections(
    roundMatches: List<TournamentMatch>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit,
    isLastRound: Boolean,
    showNextRoundConnections: Boolean
) {
    if (roundMatches.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No matches in this round",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        return
    }

    // Sort matches by matchNumber to ensure proper order
    val sortedMatches = roundMatches.sortedBy { it.matchNumber }

    // Calculate vertical spacing based on match count
    val spacing = if (sortedMatches.size > 3) 40.dp else 60.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(if (showNextRoundConnections) spacing else 16.dp)
    ) {
        sortedMatches.forEachIndexed { index, match ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // The match card
                MatchCard(
                    match = match,
                    teamNamesMap = teamNamesMap,
                    userCanReport = userCanReport,
                    onClick = { onMatchClick(match) },
                    compact = false,
                    isFinal = isLastRound,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Draw connection lines to next round
                if (showNextRoundConnections && sortedMatches.size > 1) {
                    // Draw connections only if we have multiple matches and need to connect them
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacing - 8.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        val strokeWidth = 2.dp.toPx()
                        val lineColor = Color.Gray.copy(alpha = 0.6f)

                        // Calculate whether this match connects to the next round
                        val isEvenMatch = index % 2 == 0
                        val hasPair = isEvenMatch && index < sortedMatches.size - 1

                        val startX = size.width / 2
                        val matchHeight = 80.dp.toPx() // Approximate height of a match card

                        // Draw vertical line from match
                        drawLine(
                            color = lineColor,
                            start = Offset(startX, 0f),
                            end = Offset(startX, if (hasPair) size.height / 2 else size.height),
                            strokeWidth = strokeWidth
                        )

                        // For even matches that have a pair (odd match following),
                        // draw horizontal connector to the next match
                        if (hasPair) {
                            val midY = size.height / 2
                            val nextMatchX = size.width / 2

                            // Draw horizontal line connecting to the next match's line
                            drawLine(
                                color = lineColor,
                                start = Offset(startX, midY),
                                end = Offset(nextMatchX, midY),
                                strokeWidth = strokeWidth
                            )

                            // Draw vertical line from horizontal connector to bottom
                            // (This will connect to the next round)
                            if (index == 0 || index % 2 == 0) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset((startX + nextMatchX) / 2, midY),
                                    end = Offset((startX + nextMatchX) / 2, size.height),
                                    strokeWidth = strokeWidth
                                )
                            }
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
    val scope = rememberCoroutineScope()

    // Group matches by bracket section
    val winnersMatches = matches.filter { it.id.contains("-W-") }
    val losersMatches = matches.filter { it.id.contains("-L-") }
    val finalMatches = matches.filter { it.id.contains("-F-") }

    // Group by round within each bracket
    val winnersMatchesByRound = winnersMatches.groupBy { it.round }.toSortedMap()
    val losersMatchesByRound = losersMatches.groupBy { it.round }.toSortedMap()

    // For tabs
    val sections = listOf("Winners Bracket", "Losers Bracket", "Finals")
    val pagerState = rememberPagerState(initialPage = 0) { sections.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Double Elimination Bracket",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tab row for different sections
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp,
            divider = { /* No divider */ },
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content for each tab
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WinnersBracketSection(
                    matchesByRound = winnersMatchesByRound,
                    teamNamesMap = teamNamesMap,
                    userCanReport = userCanReport,
                    onMatchClick = onMatchClick
                )
                1 -> LosersBracketSection(
                    matchesByRound = losersMatchesByRound,
                    teamNamesMap = teamNamesMap,
                    userCanReport = userCanReport,
                    onMatchClick = onMatchClick
                )
                2 -> FinalsSection(
                    matches = finalMatches,
                    teamNamesMap = teamNamesMap,
                    userCanReport = userCanReport,
                    onMatchClick = onMatchClick
                )
            }
        }
    }
}

@Composable
fun WinnersBracketSection(
    matchesByRound: Map<Int, List<TournamentMatch>>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit
) {
    if (matchesByRound.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No matches in winners bracket")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        matchesByRound.forEach { (round, roundMatches) ->
            Text(
                text = when (round) {
                    matchesByRound.keys.max() -> "Winners Final"
                    matchesByRound.keys.max() - 1 -> "Winners Semifinal"
                    else -> "Winners Round $round"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            BracketRoundWithConnections(
                roundMatches = roundMatches,
                teamNamesMap = teamNamesMap,
                userCanReport = userCanReport,
                onMatchClick = onMatchClick,
                isLastRound = round == matchesByRound.keys.max(),
                showNextRoundConnections = round < matchesByRound.keys.max()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LosersBracketSection(
    matchesByRound: Map<Int, List<TournamentMatch>>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit
) {
    if (matchesByRound.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No matches in losers bracket")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        matchesByRound.forEach { (round, roundMatches) ->
            Text(
                text = when (round) {
                    matchesByRound.keys.max() -> "Losers Final"
                    matchesByRound.keys.max() - 1 -> "Losers Semifinal"
                    else -> "Losers Round $round"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            BracketRoundWithConnections(
                roundMatches = roundMatches,
                teamNamesMap = teamNamesMap,
                userCanReport = userCanReport,
                onMatchClick = onMatchClick,
                isLastRound = round == matchesByRound.keys.max(),
                showNextRoundConnections = round < matchesByRound.keys.max()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FinalsSection(
    matches: List<TournamentMatch>,
    teamNamesMap: Map<String, String>,
    userCanReport: Boolean,
    onMatchClick: (TournamentMatch) -> Unit
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No final matches scheduled yet")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        matches.sortedBy { it.matchNumber }.forEachIndexed { index, match ->
            if (index == 0) {
                Text(
                    text = "Grand Final",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Text(
                    text = "Reset Match (if needed)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Played if winners bracket finalist loses first match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Display the match with a special highlight for finals
            MatchCard(
                match = match,
                teamNamesMap = teamNamesMap,
                userCanReport = userCanReport,
                onClick = { onMatchClick(match) },
                isFinal = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add explanation text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Double Elimination Format",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Teams from winners bracket have an advantage in finals\n" +
                           "• Teams must lose twice to be eliminated\n" +
                           "• In finals, team from losers bracket must win twice to be champion"
                )
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
    isFinal: Boolean = false,
    modifier: Modifier = Modifier
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

    // Determine if this match has a winner
    val hasWinner = match.status == MatchStatus.COMPLETED && match.winnerId.isNotEmpty()
    val homeTeamIsWinner = hasWinner && match.winnerId == match.homeTeamId
    val awayTeamIsWinner = hasWinner && match.winnerId == match.awayTeamId

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Trophy icon for winner
                        if (homeTeamIsWinner) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "🏆",
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = homeTeamName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (homeTeamIsWinner) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = awayTeamName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (awayTeamIsWinner) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )

                        // Trophy icon for winner
                        if (awayTeamIsWinner) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "🏆",
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
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
                        MatchStatus.CANCELLED -> "Cancelled"
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