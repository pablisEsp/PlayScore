package navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import firebase.auth.FirebaseAuthInterface
import org.koin.compose.koinInject
import ui.components.AppBottomNavBar
import ui.home.HomeScreen
import ui.login.LoginScreen
import ui.profile.ProfileScreen
import ui.register.RegisterScreen
import ui.search.SearchScreen
import ui.settings.SettingsScreen
import ui.team.TeamScreen

@Composable
fun AppNavHost(
    firebaseAuth: FirebaseAuthInterface = koinInject()
) {
    val navController: NavHostController = rememberNavController()
    var startDestination by remember { mutableStateOf<Any>(LoginScreen) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.let {
        when (it) {
            "navigation.HomeScreen" -> HomeScreen
            "navigation.SearchScreen" -> SearchScreen
            "navigation.TeamScreen" -> TeamScreen
            "navigation.ProfileScreen" -> ProfileScreen
            "navigation.SettingsScreen" -> SettingsScreen
            "navigation.LoginScreen" -> LoginScreen
            "navigation.RegisterScreen" -> RegisterScreen
            else -> null
        }
    }

    // Check if user is logged in to determine start destination
    LaunchedEffect(Unit) {
        if (firebaseAuth.getCurrentUser() != null) {
            startDestination = HomeScreen
        }
    }

    // Determine if bottom nav should be visible
    val showBottomNav = currentRoute in NavigationItems.allDestinations

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                currentRoute = currentRoute,
                visible = showBottomNav,
                onNavigate = { destination ->
                    // Convert the destination to its route string
                    val routeString = when(destination) {
                        is HomeScreen -> "navigation.HomeScreen"
                        is SearchScreen -> "navigation.SearchScreen"
                        is TeamScreen -> "navigation.TeamScreen"
                        is ProfileScreen -> "navigation.ProfileScreen"
                        is SettingsScreen -> "navigation.SettingsScreen"
                        else -> null
                    }

                    routeString?.let {
                        navController.navigate(it) {
                            // Pop up to the start destination of the graph
                            popUpTo("navigation.HomeScreen") {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<LoginScreen> {
                    LoginScreen(navController = navController)
                }

                composable<RegisterScreen> {
                    RegisterScreen(navController = navController)
                }

                composable<HomeScreen> {
                    HomeScreen(navController = navController)
                }

                composable<SearchScreen> {
                    SearchScreen(navController = navController)
                }

                composable<TeamScreen> {
                    TeamScreen(navController = navController)
                }

                composable<ProfileScreen> {
                    ProfileScreen(navController = navController)
                }

                composable<SettingsScreen> {
                    SettingsScreen(navController = navController)
                }
            }
        }
    }
}