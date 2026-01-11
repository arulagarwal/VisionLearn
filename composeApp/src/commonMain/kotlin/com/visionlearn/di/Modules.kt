package com.visionlearn.di

import com.visionlearn.ai.*
import com.visionlearn.data.repository.CustomActivityRepositoryImpl
import com.visionlearn.data.repository.ProfileRepositoryImpl
import com.visionlearn.data.repository.ProgressRepositoryImpl
import com.visionlearn.data.repository.SessionRepositoryImpl
import com.visionlearn.domain.repository.CustomActivityRepository
import com.visionlearn.domain.repository.ProfileRepository
import com.visionlearn.domain.repository.ProgressRepository
import com.visionlearn.domain.repository.SessionRepository
import com.visionlearn.presentation.screens.creator.CreatorScreenModel
import com.visionlearn.presentation.screens.home.HomeScreenModel
import com.visionlearn.presentation.screens.learning.LearningScreenModel
import com.visionlearn.presentation.screens.onboarding.OnboardingScreenModel
import com.visionlearn.presentation.screens.profile.ProfileScreenModel
import com.visionlearn.presentation.screens.progress.ProgressScreenModel
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Application configuration
 */
object AppConfig {
    /**
     * Gemini API key from Google AI Studio
     * Get yours free at: https://aistudio.google.com
     */
    var geminiApiKey: String = ""
    
    /**
     * Whether to use Gemini AI (true) or local rule-based AI (false)
     */
    val useGeminiAI: Boolean
        get() = geminiApiKey.isNotBlank()
}

/**
 * Core module - common dependencies
 */
val coreModule = module {
    single { 
        Json { 
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
        } 
    }
    
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 30000
            }
        }
    }
}

/**
 * AI module - Uses Gemini if API key provided, otherwise local fallback
 */
val aiModule = module {
    single<AIService> {
        if (AppConfig.useGeminiAI) {
            GeminiAIService(
                httpClient = get(),
                apiKey = AppConfig.geminiApiKey,
                json = get()
            )
        } else {
            LocalAIService()
        }
    }
    
    single<AISessionGenerator> {
        if (AppConfig.useGeminiAI) {
            GeminiSessionGenerator(
                httpClient = get(),
                apiKey = AppConfig.geminiApiKey,
                json = get()
            )
        } else {
            LocalSessionGenerator()
        }
    }
}

/**
 * Repository module - data layer
 */
val repositoryModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    single<ProgressRepository> { ProgressRepositoryImpl(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    single<CustomActivityRepository> { CustomActivityRepositoryImpl(get()) }
}

/**
 * ScreenModel module - presentation layer
 */
val screenModelModule = module {
    factory { OnboardingScreenModel(get()) }
    factory { HomeScreenModel(get(), get(), get(), get()) }
    factory { ProfileScreenModel(get()) }
    factory { ProgressScreenModel(get(), get()) }
    factory { CreatorScreenModel(get(), get()) }
    factory { 
        LearningScreenModel(
            profileRepository = get(),
            sessionRepository = get(),
            progressRepository = get(),
            sessionGenerator = get()
        ) 
    }
}

/**
 * All shared modules combined
 */
val allModules = listOf(
    coreModule,
    aiModule,
    repositoryModule,
    screenModelModule
)

/**
 * Platform-specific modules
 */
expect val platformModule: Module

/**
 * Initialize Koin for the application
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    try {
        startKoin {
            config?.invoke(this)
            modules(allModules + platformModule)
        }
    } catch (_: Exception) {
        // Koin may already be started
    }
}
