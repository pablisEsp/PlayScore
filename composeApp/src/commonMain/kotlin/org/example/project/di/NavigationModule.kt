package org.example.project.di

import org.example.project.navigation.NavigationManager
import org.koin.dsl.module

/**
 * Koin module for navigation components
 */
val navigationModule = module {
    // Single instance of NavigationManager
    single { NavigationManager() }
}