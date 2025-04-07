package org.example.project.navigation

/**
 * Sealed interface representing all possible navigation destinations in the app
 */
sealed interface Destination {
    // Auth-related screens
    object Login : Destination
    object Register : Destination
    //object ForgotPassword : Destination

    // Main app screens
//    object Home : Destination
//    object Profile : Destination
//    object Settings : Destination
//
//    // Feature-specific screens
//    data class Details(val itemId: String) : Destination
//    data class Category(val categoryId: String, val title: String) : Destination

    // Add more destinations as your app grows
}