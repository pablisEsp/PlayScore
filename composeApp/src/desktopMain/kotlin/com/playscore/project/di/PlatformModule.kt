package com.playscore.project.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun providePlatformModule(): Module = desktopPlatformModule

/**
 * Desktop-specific dependencies
 */
val desktopPlatformModule = module {
    // Desktop-specific implementations
    // For example:
    // single<AuthInterface> { DesktopAuthImplementation() }
    // single<DatabaseInterface> { DesktopDatabaseImplementation() }
}
