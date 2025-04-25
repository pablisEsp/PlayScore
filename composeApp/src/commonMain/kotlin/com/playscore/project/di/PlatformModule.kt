package com.playscore.project.di

import org.koin.core.module.Module

/**
 * Provides platform-specific module with implementations for platform-specific dependencies
 */
expect fun providePlatformModule(): Module
