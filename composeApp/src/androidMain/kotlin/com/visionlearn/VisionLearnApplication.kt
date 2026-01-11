package com.visionlearn

import android.app.Application
import com.visionlearn.di.AppConfig
import com.visionlearn.di.allModules
import com.visionlearn.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application class for VisionLearn
 * Initializes Koin DI and configures AI services
 */
class VisionLearnApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure Gemini API key from BuildConfig
        AppConfig.geminiApiKey = BuildConfig.GEMINI_API_KEY
        
        // Initialize Koin
        startKoin {
            androidLogger()
            androidContext(this@VisionLearnApplication)
            modules(allModules + platformModule)
        }
    }
}
