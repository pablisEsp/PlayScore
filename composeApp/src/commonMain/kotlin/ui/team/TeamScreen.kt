package ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Team
import data.model.TeamRole
import org.koin.compose.koinInject
import ui.components.LeaveTeamButton
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
    val joinRequests by teamViewModel.teamJoinRequests.collectAsState()
    var showManagementSheet by remember { mutableStateOf(false) }

    val isTeamLeader = currentUser?.teamMembership?.role in listOf(TeamRole.PRESIDENT, TeamRole.VICE_PRESIDENT)

    // Load join requests if the current user is a leader and team is available
    LaunchedEffect(currentTeam, currentUser, isTeamLeader) {
        val team = currentTeam
        val user = currentUser
        if (isTeamLeader && team != null && user != null) {
            teamViewModel.loadTeamJoinRequests()
        }
    }

    val tabTitles = remember(isTeamLeader, joinRequests) {
        val membersTabTitle = "Members" + if (isTeamLeader && joinRequests.isNotEmpty()) " (${joinRequests.size})" else ""
        // "Requests" tab is removed as its content is now part of the Members tab for leaders
        listOf("Overview", membersTabTitle)
    }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Adjust selectedTabIndex if it's out of bounds due to tabTitles changing
    LaunchedEffect(tabTitles.size) {
        if (selectedTabIndex >= tabTitles.size && tabTitles.isNotEmpty()) {
            selectedTabIndex = tabTitles.size - 1
        } else if (tabTitles.isEmpty() && selectedTabIndex != 0) {
            selectedTabIndex = 0 // Should not happen with current logic but good for safety
        }
    }


    LaunchedEffect(Unit) {
        teamViewModel.getCurrentUserData()
    }

    CompositionLocalProvider(LocalNavController provides navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Team") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
                    // Define a base modifier for content areas to apply padding
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)

                    if (isLoading && currentTeam == null) { // Show loading only if team isn't loaded yet
                        Box(
                            modifier = contentModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (currentTeam == null) { // currentTeam is null, and not initial loading: Show NoTeamView centered
                        Box(
                            modifier = contentModifier,
                            contentAlignment = Alignment.Center // This Box will center the NoTeamView
                        ) {
                            NoTeamView(
                                onCreateTeamClick = { navController.navigate(navigation.CreateTeam) },
                                onJoinTeamClick = { navController.navigate("navigation.Search?filter=Teams") }
                            )
                        }
                    } else { // currentTeam is not null: Show team details
                        // This Column is for when there IS a team.
                        // It fills the available space and its content scrolls.
                        Column(
                            modifier = contentModifier,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            currentTeam?.let { team -> // Should always be non-null here
                                TeamHeader(
                                    team = team,
                                    onSettingsClick = { showManagementSheet = true }
                                )
                                TabRow(selectedTabIndex = selectedTabIndex) {
                                    tabTitles.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = { Text(title) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Content based on selected tab
                                when (selectedTabIndex) {
                                    0 -> TeamOverview(team)
                                    1 -> TeamMembersTab(team, teamViewModel)
                                }
                            }
                        }
                    }

                    // Management sheet logic remains the same
                    if (showManagementSheet && currentTeam != null) {
                        currentTeam?.let { team ->
                            TeamManagementSheet(
                                team = team,
                                onDismiss = { showManagementSheet = false },
                                viewModel = teamViewModel
                            )
                        }
                    }
                }
    }
}

@Composable
fun TeamHeader(
    team: Team,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Left side with logo and team info
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // If logo is available, display it
            if (team.logoUrl.isNotEmpty()) {
                // You'd use an image loading library here like Coil
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape
                ) {
                    // Placeholder for logo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = team.name.take(1),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp) // Add left padding here
                )
                if (team.location.isNotEmpty()) {
                    Text(
                        text = team.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp) // Add left padding here too
                    )
                }
            }
        }

        // Right side with settings icon
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Team Settings",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementSheet(
    team: Team,
    onDismiss: () -> Unit,
    viewModel: TeamViewModel = koinInject()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isPresident = currentUser?.teamMembership?.role == TeamRole.PRESIDENT

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Team Management",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Team settings - only for president
            if (isPresident) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Team Settings",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* Navigate to edit team */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit Team Information")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { /* Navigate to manage roles */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Team Roles")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Match settings
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Match Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* Navigate to matchmaking settings */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    ) {
                        Text("Matchmaking Preferences")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { /* Navigate to schedule */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    ) {
                        Text("Team Schedule")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Leave team option - replaced with the reusable component
            LeaveTeamButton(
                team = team,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(32.dp))
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
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You're not part of a team yet",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onCreateTeamClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Create a Team")
        }

        Button(
            onClick = onJoinTeamClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Join a Team")
        }
    }
}



@Composable
fun TeamOverview(
    team: Team,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier // Use the modifier passed from the caller
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Team stats
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
                    StatItem("Total Points", team.pointsTotal.toString())
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
    }
}