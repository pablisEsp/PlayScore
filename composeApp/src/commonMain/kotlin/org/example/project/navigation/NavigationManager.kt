package org.example.project.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Centralized navigation manager that handles navigation state and actions
 */
class NavigationManager {
    // Stores the backstack of destinations
    private val _backStack = MutableStateFlow<List<Destination>>(listOf(Destination.Login))
    val backStack: StateFlow<List<Destination>> = _backStack

    // Current destination is the last item in the backstack
    private val _currentDestination = MutableStateFlow<Destination>(_backStack.value.last())
    val currentDestination: StateFlow<Destination> = _currentDestination

    // Navigate to a new destination
    fun navigateTo(destination: Destination) {
        val newBackStack = _backStack.value.toMutableList().apply {
            add(destination)
        }
        _backStack.value = newBackStack
        _currentDestination.value = destination
    }

    // Go back to previous screen
    fun navigateBack(): Boolean {
        if (_backStack.value.size <= 1) {
            return false // Can't go back further
        }

        val newBackStack = _backStack.value.toMutableList().apply {
            removeAt(lastIndex)
        }

        _backStack.value = newBackStack
        _currentDestination.value = newBackStack.last()
        return true
    }

    // Clear backstack and set a new root destination
    fun navigateToRoot(rootDestination: Destination) {
        _backStack.value = listOf(rootDestination)
        _currentDestination.value = rootDestination
    }
}
