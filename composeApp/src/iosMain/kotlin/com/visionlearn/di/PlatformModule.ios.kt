package com.visionlearn.di

import com.visionlearn.data.local.DatabaseDriverFactory
import com.visionlearn.data.local.createDatabase
import com.visionlearn.platform.AccessibilityAnnouncer
import com.visionlearn.platform.TextToSpeech
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific Koin module
 */
actual val platformModule: Module = module {
    // Database
    single { DatabaseDriverFactory() }
    single { createDatabase(get()) }
    
    // Text-to-Speech
    single { TextToSpeech() }
    single { AccessibilityAnnouncer() }
}
