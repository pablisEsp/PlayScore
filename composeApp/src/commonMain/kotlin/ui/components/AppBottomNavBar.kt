package ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import navigation.Home
import navigation.Profile
import navigation.Search
import navigation.Team
import utils.icons.Teams

data class NavigationItem(
    val route: Any,
    val icon: ImageVector,
    val title: String
)

private val bottomNavItems = listOf(
    NavigationItem(
        route = Home,
        icon = Icons.Default.Home,
        title = "Home"
    ),
    NavigationItem(
        route = Search(),
        icon = Icons.Default.Search,
        title = "Search"
    ),
    NavigationItem(
        route = Team,
        icon = Teams,
        title = "Team"
    ),
    NavigationItem(
        route = Profile,
        icon = Icons.Default.Person,
        title = "Profile"
    )
)

@Composable
fun AppBottomNavBar(
    currentRoute: String?,
    visible: Boolean = true,
    onNavigate: (Any) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = when {
                            // For Search route specifically, check if currentRoute starts with its qualified name
                            // Ensure item.route is an instance of Search for this comparison to work as intended
                            // or compare based on the type if Search() is always passed with default args from bottom nav
                            item.route is Search && currentRoute?.startsWith(Search::class.qualifiedName?.substringBefore("?").toString()) == true -> true
                            // For other routes, keep the exact match
                            else -> currentRoute == item.route::class.qualifiedName
                        }
                        
                        val animatedSize by animateDpAsState(if (selected) 30.dp else 24.dp)
                        val interactionSource = remember { MutableInteractionSource() }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onNavigate(item.route) }
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(animatedSize)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}