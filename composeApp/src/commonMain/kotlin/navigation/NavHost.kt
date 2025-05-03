package navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import firebase.auth.FirebaseAuthInterface
import org.koin.compose.koinInject
import ui.home.HomeScreen
import ui.login.LoginScreen
import ui.register.RegisterScreen

@Composable
fun AppNavHost(
    firebaseAuth: FirebaseAuthInterface = koinInject()
) {
    val navController: NavHostController = rememberNavController()
    var startDestination by remember { mutableStateOf<Any>(LoginScreen) }

    // Check if user is logged in to determine start destination
    LaunchedEffect(Unit) {
        if (firebaseAuth.getCurrentUser() != null) {
            startDestination = HomeScreen
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<LoginScreen> {
            LoginScreen(
                navController = navController
            )
        }
        composable<RegisterScreen> {
            RegisterScreen(
                navController = navController
            )
        }
        composable<HomeScreen> {
            HomeScreen(
                navController = navController
            )
        }
    }
}