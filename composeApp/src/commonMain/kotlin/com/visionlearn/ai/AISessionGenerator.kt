package com.visionlearn.ai

import com.visionlearn.domain.model.*
import kotlinx.serialization.Serializable

/**
 * AI-powered learning session generator
 * Creates personalized, adaptive learning content based on the child's CVI profile
 */
interface AISessionGenerator {
    /**
     * Generate a complete learning session tailored to the child's profile
     */
    suspend fun generateSession(
        profile: VisualProfile,
        moduleType: ModuleType,
        previousPerformance: SessionPerformance? = null
    ): Result<GeneratedSession>
    
    /**
     * Adapt remaining questions based on mid-session performance
     */
    suspend fun adaptSession(
        profile: VisualProfile,
        moduleType: ModuleType,
        questionsCompleted: Int,
        correctSoFar: Int,
        averageResponseTimeMs: Long
    ): Result<List<GeneratedQuestion>>
    
    /**
     * Generate a quick warm-up activity
     */
    suspend fun generateWarmUp(
        profile: VisualProfile
    ): Result<GeneratedQuestion>
}

/**
 * A complete AI-generated learning session
 */
@Serializable
data class GeneratedSession(
    val id: String,
    val moduleType: String,
    val theme: String,
    val difficultyLevel: Int,
    val questions: List<GeneratedQuestion>,
    val estimatedDurationMinutes: Int,
    val encouragementMessages: List<String>,
    val completionMessage: String
)

/**
 * A single generated question
 */
@Serializable
data class GeneratedQuestion(
    val id: String,
    val type: String, // "recognition", "sorting", "matching", "sequencing", "cause_effect"
    val prompt: String,
    val promptAudio: String, // TTS-friendly version
    val correctAnswer: String,
    val distractors: List<String>,
    val hint: String,
    val successFeedback: String,
    val retryFeedback: String,
    val visualConfig: VisualConfig
)

/**
 * Visual configuration for a question
 */
@Serializable
data class VisualConfig(
    val backgroundColor: String = "black",
    val accentColor: String = "yellow",
    val useAnimation: Boolean = false,
    val itemSize: String = "large", // "small", "medium", "large"
    val displayTimeoutMs: Long = 5000
)

/**
 * Performance data from previous sessions
 */
data class SessionPerformance(
    val accuracy: Int,
    val avgResponseTimeMs: Long,
    val successfulThemes: List<String>,
    val challengingAreas: List<String>,
    val totalSessionsCompleted: Int
)
