package navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Face
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
data object LoginScreen

@Serializable
data object RegisterScreen

@Serializable
data object HomeScreen {
    val icon: ImageVector = Icons.Default.Home
    val title: String = "Home"
}

@Serializable
data object SearchScreen {
    val icon: ImageVector = Icons.Default.Search
    val title: String = "Search"
}

@Serializable
data object TeamScreen {
    val icon: ImageVector = Icons.Rounded.Face
    val title: String = "Team"
}

@Serializable
data object ProfileScreen {
    val icon: ImageVector = Icons.Default.Person
    val title: String = "Profile"
}

@Serializable
data object SettingsScreen {
    val icon: ImageVector = Icons.Default.Settings
    val title: String = "Settings"
}

object NavigationItems {
    val bottomNavItems = listOf(HomeScreen, SearchScreen, TeamScreen, ProfileScreen)
    val allDestinations = bottomNavItems + SettingsScreen
}