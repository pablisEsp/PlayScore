package ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.*

@Composable
fun AppBottomNavBar(
    currentRoute: Any?,
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
                    NavigationItems.bottomNavItems.forEach { destination ->
                        val selected = currentRoute == destination
                        val icon = when (destination) {
                            is HomeScreen -> HomeScreen.icon
                            is SearchScreen -> SearchScreen.icon
                            is TeamScreen -> TeamScreen.icon
                            is ProfileScreen -> ProfileScreen.icon
                            else -> HomeScreen.icon
                        }
                        val title = when (destination) {
                            is HomeScreen -> HomeScreen.title
                            is SearchScreen -> SearchScreen.title
                            is TeamScreen -> TeamScreen.title
                            is ProfileScreen -> ProfileScreen.title
                            else -> ""
                        }
                        val animatedSize by animateDpAsState(if (selected)  30.dp else 24.dp)
                        val interactionSource = remember { MutableInteractionSource() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null, // Disable ripple effect
                                    onClick = { onNavigate(destination) }
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(animatedSize)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = title,
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