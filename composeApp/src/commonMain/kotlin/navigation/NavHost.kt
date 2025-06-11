package navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import data.model.UserRole
import firebase.auth.FirebaseAuthInterface
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import playscore.composeapp.generated.resources.Res
import playscore.composeapp.generated.resources.greenlogo
import ui.admin.AdminPanelScreen
import ui.admin.ReportedPostsScreen
import ui.admin.UserManagementScreen
import ui.auth.EmailVerificationScreen
import ui.auth.ForgotPasswordScreen
import ui.auth.LoginScreen
import ui.auth.RegisterScreen
import ui.components.AppBottomNavBar
import ui.home.HomeScreen
import ui.post.PostDetailScreen
import ui.profile.ProfileScreen
import ui.search.SearchScreen
import ui.settings.SettingsScreen
import ui.team.CreateTeamScreen
import ui.team.TeamScreen
import ui.tournament.CreateTournamentScreen
import ui.tournament.EditTournamentScreen
import ui.tournament.TeamTournamentScreen
import ui.tournament.TournamentApplicationsScreen
import ui.tournament.TournamentDetailScreen
import ui.tournament.TournamentManagementScreen
import utils.isDesktop
import viewmodel.TeamViewModel
import viewmodel.UserViewModel

@Composable
fun AppNavHost(
    firebaseAuth: FirebaseAuthInterface = koinInject(),
    userViewModel: UserViewModel = koinInject()
) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (firebaseAuth.getCurrentUser() != null) Home else Login
    }

    val isBanned by userViewModel.isBanned.collectAsState()
    var showBannedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isBanned) {
        println("Ban status changed: $isBanned")
        if (isBanned) {
            println("Showing ban dialog")
            showBannedDialog = true
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomNav = currentRoute?.startsWith(Search::class.qualifiedName?.substringBefore("?").toString()) == true ||
            currentRoute in listOf(
        Home::class.qualifiedName,
        Team::class.qualifiedName,
        Profile::class.qualifiedName,
        Settings::class.qualifiedName
    )

    // Define when to hide the top bar (authentication screens)
    val hideTopBar = currentRoute in listOf(
        Login::class.qualifiedName,
        Register::class.qualifiedName,
        EmailVerification::class.qualifiedName,
        ForgotPassword::class.qualifiedName
    )

    Scaffold(
        topBar = {
        if (!hideTopBar) {
            // Get title based on current route
            val screenTitle = when {
                currentRoute == Home::class.qualifiedName -> "Feed"
                currentRoute == Team::class.qualifiedName -> "Teams"
                currentRoute == Profile::class.qualifiedName -> "Profile"
                currentRoute == Settings::class.qualifiedName -> "Settings"
                // Check if route starts with Search class name instead of exact match
                currentRoute?.startsWith(Search::class.qualifiedName?.substringBefore("?").toString()) == true -> "Search"
                currentRoute == PostDetail::class.qualifiedName -> "Post"
                currentRoute == AdminPanel::class.qualifiedName -> "Admin"
                currentRoute == CreateTeam::class.qualifiedName -> "Create Team"
                currentRoute == CreateTournament::class.qualifiedName -> "Create Tournament"
                currentRoute == TeamTournaments::class.qualifiedName -> "Team Tournaments"
                currentRoute == TournamentManagement::class.qualifiedName -> "Tournament Management"
                else -> ""
            }

            // Check current route to provide screen-specific actions
            if (currentRoute == Home::class.qualifiedName) {
                AppTopBar(
                    navController = navController,
                    title = screenTitle,
                    actions = {
                        if (isDesktop()) {
                            IconButton(onClick = { /* Need to handle refresh */ }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh"
                                )
                            }
                        }
                    }
                )
            } else {
                AppTopBar(navController = navController, title = screenTitle)
            }
        }
                 },
        bottomBar = {
            AppBottomNavBar(
                currentRoute = currentRoute,
                visible = showBottomNav,
                onNavigate = { navController.navigate(it) }
            )
        }
    ) { innerPadding ->
        // Apply a reduced top padding while keeping the other sides from innerPadding
        val customPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() - 16.dp, // Reduce by 16.dp for minimal padding
            bottom = innerPadding.calculateBottomPadding(),
            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
        )
        Box(modifier = Modifier.padding(customPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<Login> { LoginScreen(navController) }
                composable<Register> { RegisterScreen(navController) }
                composable<EmailVerification> { backStackEntry ->
                    val verificationArgs: EmailVerification = backStackEntry.toRoute()
                    EmailVerificationScreen(
                        navController = navController,
                        email = verificationArgs.email
                    )
                }
                composable<ForgotPassword> { ForgotPasswordScreen(navController) }
                composable<Home> { HomeScreen(navController) }
                composable<Team> { TeamScreen(navController) }
                composable<CreateTeam> { CreateTeamScreen(navController)}
                composable<Profile> { ProfileScreen(navController) }
                composable<Settings> { SettingsScreen(navController) }
                composable<AdminPanel> { AdminPanelScreen(navController) }
                composable<ReportedPosts> {
                    ReportedPostsScreen(navController)
                }
                composable<UserManagement> {
                    UserManagementScreen()
                }
                composable<CreateTournament> { CreateTournamentScreen(navController) }
                composable<TeamTournaments> {
                    val teamViewModel = koinInject<TeamViewModel>()
                    TeamTournamentScreen(
                        navController = navController,
                        teamViewModel = teamViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable<TeamTournamentDetail> { backStackEntry ->
                    val routeArgs = backStackEntry.toRoute<TeamTournamentDetail>()
                    TournamentDetailScreen(
                        navController = navController,
                        tournamentId = routeArgs.tournamentId
                    )
                }
                composable<TournamentManagement> {
                    TournamentManagementScreen(navController)
                }

                composable<EditTournament> { backStackEntry ->
                    val args: EditTournament = backStackEntry.toRoute()
                    EditTournamentScreen(
                        navController = navController,
                        tournamentId = args.tournamentId
                    )
                }

                composable<TournamentApplications> { backStackEntry ->
                    val args: TournamentApplications = backStackEntry.toRoute()
                    TournamentApplicationsScreen(
                        navController = navController,
                        tournamentId = args.tournamentId
                    )
                }
                composable<Search> { backStackEntry ->
                    val searchArgs: Search = backStackEntry.toRoute()
                    SearchScreen(
                        navController = navController,
                        filter = searchArgs.filter
                    )
                }
                composable<PostDetail> { backStackEntry ->
                    val postDetail: PostDetail = backStackEntry.toRoute()
                    PostDetailScreen(
                        postId = postDetail.postId,
                        navController = navController
                    )
                }
            }
        }
    }
    if (showBannedDialog) {
        AlertDialog(
            onDismissRequest = {
                // Don't allow dismiss without action
            },
            title = { Text("Account Suspended") },
            text = {
                Text("Your account has been banned due to violation of our terms of service. " +
                        "If you believe this is an error, please contact support.")
            },
            confirmButton = {
                Button(onClick = {
                    println("Ban dialog confirmed, logging out")
                    showBannedDialog = false
                    userViewModel.signOutBannedUser() // Call the new function here
                    userViewModel.clearBanStatus()
                    navController.navigate("navigation.Login") {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, InternalResourceApi::class)
@Composable
fun AppTopBar(
    navController: NavController,
    title: String = "",
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val userViewModel: UserViewModel = koinInject()
    val currentUser by userViewModel.currentUser.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Define which routes are main screens (they show profile icon)
    val mainScreens = listOf(
        Home::class.qualifiedName,
        Team::class.qualifiedName,
        Profile::class.qualifiedName,
        Search::class.qualifiedName?.substringBefore("?") // Base Search route without params
    )

    val isMainScreen = mainScreens.contains(currentRoute?.substringBefore("?"))
    val isAdmin = currentUser?.globalRole == UserRole.ADMIN ||
                  currentUser?.globalRole == UserRole.SUPER_ADMIN

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // First Row: Profile/Back Icon (left), Logo (center), Actions (right)
        CenterAlignedTopAppBar(
            title = {
                Image(
                    painter = painterResource(Res.drawable.greenlogo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            },
            navigationIcon = {
                if (isMainScreen) {
                    // Show profile icon on main screens
                    IconButton(onClick = { navController.navigate(Profile) }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                } else {
                    // Show back arrow on non-main screens
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                // Add admin icon if the user is an admin
                if (isAdmin) {
                    IconButton(onClick = { navController.navigate(AdminPanel) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Admin Panel"
                        )
                    }
                }
                // Include any other actions passed to this composable
                actions()
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Second Row: Title Text with minimal bottom padding
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 2.dp,
                    bottom = 10.dp
                )
            )
        }
    }
}