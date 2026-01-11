package com.visionlearn.ai

import com.visionlearn.domain.model.CVIPhase
import com.visionlearn.domain.model.ImageAnalysisResult
import com.visionlearn.domain.model.ProgressInsights
import com.visionlearn.domain.model.ProgressRecord
import com.visionlearn.domain.model.VisualProfile

/**
 * Interface for AI-powered services using Claude API
 */
interface AIService {
    
    /**
     * Analyze an image for CVI-appropriate content
     * Returns description, complexity assessment, and recommendations
     */
    suspend fun analyzeImage(imageBytes: ByteArray): Result<ImageAnalysisResult>
    
    /**
     * Generate a simplified description of an image for a specific CVI phase
     */
    suspend fun simplifyDescription(
        description: String,
        targetPhase: CVIPhase
    ): Result<String>
    
    /**
     * Generate audio-friendly description for TTS
     */
    suspend fun generateAudioDescription(
        imageDescription: String,
        profile: VisualProfile
    ): Result<String>
    
    /**
     * Suggest activities based on visual profile
     */
    suspend fun suggestActivities(
        profile: VisualProfile
    ): Result<List<ActivitySuggestion>>
    
    /**
     * Generate progress insights from session data
     */
    suspend fun generateProgressInsights(
        profile: VisualProfile,
        records: List<ProgressRecord>
    ): Result<ProgressInsights>
    
    /**
     * Assess if content is appropriate for a visual profile
     */
    suspend fun assessContentAppropriateness(
        content: String,
        profile: VisualProfile
    ): Result<ContentAssessment>
}

/**
 * Suggestion for an activity based on profile
 */
data class ActivitySuggestion(
    val moduleType: String,
    val title: String,
    val description: String,
    val targetBehaviors: List<String>,
    val estimatedDifficulty: Int,
    val reasoning: String
)

/**
 * Assessment of content appropriateness
 */
data class ContentAssessment(
    val isAppropriate: Boolean,
    val complexityScore: Int,
    val concerns: List<String>,
    val suggestions: List<String>
)
