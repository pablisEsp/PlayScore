package com.playscore.project.di

import com.playscore.project.navigation.NavigationManager
import org.koin.dsl.module

/**
 * Koin module for navigation components
 */
val navigationModule = module {
    // Single instance of NavigationManager
    single { NavigationManager() }
}