package com.visionlearn.di

import com.visionlearn.data.local.DatabaseDriverFactory
import com.visionlearn.data.local.createDatabase
import com.visionlearn.platform.AccessibilityAnnouncer
import com.visionlearn.platform.TextToSpeech
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Koin module
 */
actual val platformModule: Module = module {
    // Database
    single { DatabaseDriverFactory(androidContext()) }
    single { createDatabase(get()) }
    
    // Text-to-Speech
    single { TextToSpeech(androidContext()) }
    single { AccessibilityAnnouncer(androidContext()) }
}
