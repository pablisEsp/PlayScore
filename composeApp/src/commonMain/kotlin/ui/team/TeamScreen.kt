// ui/team/TeamScreen.kt
package ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Team
import data.model.TeamRole
import org.koin.compose.koinInject
import ui.components.LocalNavController
import viewmodel.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    navController: NavController,
    teamViewModel: TeamViewModel = koinInject()
) {
    val currentUser by teamViewModel.currentUser.collectAsState()
    val isLoading by teamViewModel.isLoading.collectAsState()
    val currentTeam by teamViewModel.currentTeam.collectAsState()

    val isTeamLeader = currentUser?.teamMembership?.role in listOf(TeamRole.PRESIDENT, TeamRole.VICE_PRESIDENT)

    // Create filtered tab list based on user role
    val tabTitles = remember(isTeamLeader) {
        if (isTeamLeader) {
            listOf("Overview", "Members", "Requests", "Management")
        } else {
            listOf("Overview", "Members")
        }
    }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Get the current user data and team if applicable
    LaunchedEffect(Unit) {
        teamViewModel.getCurrentUserData()
    }

    CompositionLocalProvider(LocalNavController provides navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Team") }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    val hasTeam = !currentUser?.teamMembership?.teamId.isNullOrEmpty()

                    if (!hasTeam) {
                        NoTeamView(
                            onCreateTeamClick = { navController.navigate("navigation.CreateTeam") },
                            onJoinTeamClick = { navController.navigate("navigation.Search?filter=Teams") }
                        )
                    } else {
                        // Team content with tabbed interface
                        currentTeam?.let { team ->
                            Column {
                                // Team header section (always visible)
                                TeamHeader(team)

                                // Tab row for navigation
                                TabRow(
                                    selectedTabIndex = selectedTabIndex,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    tabTitles.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = { Text(title) }
                                        )
                                    }
                                }

                                // Content based on selected tab and user role
                                when {
                                    // Overview tab
                                    selectedTabIndex == 0 -> TeamOverview(team)

                                    // Members tab
                                    selectedTabIndex == 1 -> TeamMembersTab(team)

                                    // Requests tab (only for leaders)
                                    selectedTabIndex == 2 && isTeamLeader -> TeamJoinRequestsTab(team)

                                    // Management tab (only for leaders)
                                    selectedTabIndex == 3 && isTeamLeader -> TeamManagementTab(team)
                                }
                            }
                        } ?: Text("Loading team information...")
                    }
                }
            }
        }
    }
}

@Composable
fun TeamHeader(team: Team) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Team logo
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = team.name.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Team info
        Column {
            Text(
                text = team.name,
                style = MaterialTheme.typography.headlineMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "W-L: ${team.totalWins}-${team.totalLosses}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Rank: #${team.ranking}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun TeamOverview(team: Team) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Team stats card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Team Stats",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Ranking", team.ranking.toString())
                    StatItem("Points", team.pointsTotal.toString())
                    StatItem("W-L", "${team.totalWins}-${team.totalLosses}")
                }
            }
        }

        // Team description
        if (team.description.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(team.description)
                }
            }
        }

        // Upcoming matches placeholder for future implementation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Upcoming Matches",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Matchmaking feature coming soon!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun NoTeamView(
    onCreateTeamClick: () -> Unit,
    onJoinTeamClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "You are not part of any team",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCreateTeamClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create a Team")
        }

        Button(
            onClick = onJoinTeamClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join a Team")
        }
    }
}