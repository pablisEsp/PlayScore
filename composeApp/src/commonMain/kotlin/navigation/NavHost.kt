package navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import firebase.auth.FirebaseAuthInterface
import org.koin.compose.koinInject
import ui.components.AppBottomNavBar
import ui.home.HomeScreen
import ui.login.LoginScreen
import ui.post.PostDetailScreen
import ui.profile.ProfileScreen
import ui.register.RegisterScreen
import ui.search.SearchScreen
import ui.settings.SettingsScreen
import ui.team.CreateTeamScreen
import ui.team.TeamScreen

@Composable
fun AppNavHost(
    firebaseAuth: FirebaseAuthInterface = koinInject()
) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (firebaseAuth.getCurrentUser() != null) Home else Login
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomNav = currentRoute in listOf(
        Home::class.qualifiedName,
        Search::class.qualifiedName,
        Team::class.qualifiedName,
        Profile::class.qualifiedName,
        Settings::class.qualifiedName
    )

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                currentRoute = currentRoute,
                visible = showBottomNav,
                onNavigate = { navController.navigate(it) }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<Login> { LoginScreen(navController) }
                composable<Register> { RegisterScreen(navController) }
                composable<Home> { HomeScreen(navController) }
                composable<Search> { SearchScreen(navController) }
                composable<Team> { TeamScreen(navController) }
                composable<CreateTeam> { CreateTeamScreen(navController)}
                composable<Profile> { ProfileScreen(navController) }
                composable<Settings> { SettingsScreen(navController) }
                composable(
                    route = "navigation.Search?filter={filter}",
                    arguments = listOf(
                        navArgument("filter") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    )
                ) {
                    SearchScreen(navController)
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
}