package com.visionlearn.ai

import com.visionlearn.domain.model.*
import kotlinx.datetime.Clock

/**
 * Local AI service that works completely offline
 * Uses rule-based logic instead of cloud AI
 * 
 * Benefits:
 * - No API key needed
 * - Works offline
 * - Zero cost
 * - Privacy-preserving
 * - Fast response times
 */
class LocalAIService : AIService {
    
    override suspend fun analyzeImage(imageBytes: ByteArray): Result<ImageAnalysisResult> {
        // Rule-based image analysis placeholder
        // In production, integrate TensorFlow Lite MobileNet
        return Result.success(
            ImageAnalysisResult(
                shortDescription = "Image uploaded successfully",
                detailedDescription = "An image has been uploaded for analysis. The content appears suitable for visual learning activities.",
                identifiedObjects = listOf("object"),
                dominantColors = listOf("yellow", "blue"),
                complexityScore = 3,
                recommendedPhase = CVIPhase.PHASE_II,
                simplificationSuggestions = listOf(
                    "Use high contrast colors",
                    "Focus on single objects",
                    "Add clear borders"
                )
            )
        )
    }
    
    override suspend fun simplifyDescription(
        description: String,
        targetPhase: CVIPhase
    ): Result<String> {
        val simplified = when (targetPhase) {
            CVIPhase.PHASE_I -> simplifyForPhaseI(description)
            CVIPhase.PHASE_II -> simplifyForPhaseII(description)
            CVIPhase.PHASE_III -> description // Full complexity OK
        }
        return Result.success(simplified)
    }
    
    private fun simplifyForPhaseI(text: String): String {
        // Phase I: Maximum simplification
        // - Very short sentences
        // - Basic vocabulary
        // - Single concepts
        val words = text.split(" ")
            .filter { it.length <= 6 } // Simple words only
            .take(5)
        
        return if (words.isNotEmpty()) {
            words.joinToString(" ").let { 
                if (!it.endsWith(".")) "$it." else it 
            }
        } else {
            text.split(".").firstOrNull()?.take(30)?.plus(".") ?: text
        }
    }
    
    private fun simplifyForPhaseII(text: String): String {
        // Phase II: Moderate simplification
        // - Short sentences
        // - Clear structure
        // - 2-3 concepts max
        return text.split(".")
            .take(2)
            .joinToString(". ")
            .trim()
            .let { if (!it.endsWith(".")) "$it." else it }
    }
    
    override suspend fun generateAudioDescription(
        imageDescription: String,
        profile: VisualProfile
    ): Result<String> {
        // Generate TTS-friendly description based on profile
        val baseDescription = when (profile.cviPhase) {
            CVIPhase.PHASE_I -> simplifyForPhaseI(imageDescription)
            CVIPhase.PHASE_II -> simplifyForPhaseII(imageDescription)
            CVIPhase.PHASE_III -> imageDescription
        }
        
        // Add pauses for better TTS
        val withPauses = baseDescription
            .replace(",", ", ")
            .replace(".", ". ")
        
        // Add color cues if user has preferred colors
        val colorCue = if (profile.preferredColors.isNotEmpty()) {
            "Look for the ${profile.preferredColors.first()} color. "
        } else ""
        
        return Result.success(colorCue + withPauses)
    }
    
    override suspend fun suggestActivities(
        profile: VisualProfile
    ): Result<List<ActivitySuggestion>> {
        val suggestions = mutableListOf<ActivitySuggestion>()
        
        // Rule-based suggestions based on visual behaviors
        
        // Visual Attention recommendations
        if (profile.visualAttention <= 2) {
            suggestions.add(
                ActivitySuggestion(
                    moduleType = "CAUSE_EFFECT",
                    title = "Simple Touch Response",
                    description = "Build visual attention with immediate feedback",
                    targetBehaviors = listOf("Visual Attention"),
                    estimatedDifficulty = 1,
                    reasoning = "Low attention score benefits from cause-effect activities with instant visual rewards"
                )
            )
        }
        
        // Visual Recognition recommendations
        if (profile.visualRecognition <= 3) {
            suggestions.add(
                ActivitySuggestion(
                    moduleType = "RECOGNITION",
                    title = "Familiar Object Recognition",
                    description = "Practice identifying everyday objects",
                    targetBehaviors = listOf("Visual Recognition"),
                    estimatedDifficulty = 2,
                    reasoning = "Building recognition skills with familiar, high-contrast images"
                )
            )
        }
        
        // Array Complexity recommendations
        if (profile.arrayComplexity >= 3) {
            suggestions.add(
                ActivitySuggestion(
                    moduleType = "SORTING",
                    title = "Category Sorting",
                    description = "Group objects by type",
                    targetBehaviors = listOf("Array Complexity", "Visual Recognition"),
                    estimatedDifficulty = 3,
                    reasoning = "Ready for activities with multiple items based on array complexity score"
                )
            )
        }
        
        // Matching for visual memory
        if (profile.visualRecognition >= 2 && profile.visualAttention >= 2) {
            suggestions.add(
                ActivitySuggestion(
                    moduleType = "MATCHING",
                    title = "Memory Matching",
                    description = "Find matching pairs of images",
                    targetBehaviors = listOf("Visual Memory", "Visual Recognition"),
                    estimatedDifficulty = 2,
                    reasoning = "Good foundation for memory activities"
                )
            )
        }
        
        // Sequencing for higher phases
        if (profile.cviPhase == CVIPhase.PHASE_III || 
            (profile.visualRecognition >= 3 && profile.objectComplexity >= 3)) {
            suggestions.add(
                ActivitySuggestion(
                    moduleType = "SEQUENCING",
                    title = "Story Sequencing",
                    description = "Arrange events in order",
                    targetBehaviors = listOf("Sequential Processing", "Visual Memory"),
                    estimatedDifficulty = 4,
                    reasoning = "Ready for sequential reasoning based on profile"
                )
            )
        }
        
        // Default suggestion if none matched
        if (suggestions.isEmpty()) {
            suggestions.add(
                ActivitySuggestion(
                    moduleType = "CAUSE_EFFECT",
                    title = "Exploration Mode",
                    description = "Free exploration with visual feedback",
                    targetBehaviors = listOf("Visual Curiosity"),
                    estimatedDifficulty = 1,
                    reasoning = "Start with exploratory activities to assess preferences"
                )
            )
        }
        
        return Result.success(suggestions.take(3)) // Return top 3
    }
    
    override suspend fun generateProgressInsights(
        profile: VisualProfile,
        records: List<ProgressRecord>
    ): Result<ProgressInsights> {
        val now = Clock.System.now().toEpochMilliseconds()
        val startDate = records.minOfOrNull { it.startedAt.toEpochMilliseconds() } ?: now
        val endDate = records.maxOfOrNull { it.completedAt?.toEpochMilliseconds() ?: it.startedAt.toEpochMilliseconds() } ?: now
        
        if (records.isEmpty()) {
            return Result.success(
                ProgressInsights(
                    profileId = profile.id,
                    periodStartDate = now,
                    periodEndDate = now,
                    totalSessions = 0,
                    totalActivitiesCompleted = 0,
                    averageAccuracy = 0f,
                    averageResponseTimeMs = 0L,
                    averageSessionDurationMs = 0L,
                    strongestVisualBehaviors = emptyList(),
                    areasForImprovement = listOf("Visual Attention", "Visual Recognition"),
                    recommendations = listOf(
                        "Start with simple touch-response activities",
                        "Use preferred colors (${profile.preferredColors.joinToString()})"
                    ),
                    summary = "No activities completed yet. Try starting with a Cause & Effect activity!"
                )
            )
        }
        
        // Calculate statistics
        val totalAttempts = records.size
        val correctCount = records.count { it.isCorrect == true }
        val accuracy = if (totalAttempts > 0) (correctCount.toFloat() / totalAttempts) * 100 else 0f
        
        // Analyze response times
        val avgResponseTime = records
            .mapNotNull { it.responseTimeMs }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong() ?: 0L
        
        // Identify strengths and areas for improvement
        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()
        
        when {
            accuracy >= 80 -> strengths.add("Visual Recognition")
            accuracy >= 60 -> strengths.add("Visual Attention")
            accuracy < 40 -> improvements.add("Visual Recognition")
        }
        
        when {
            avgResponseTime in 1..3000 -> strengths.add("Response Speed")
            avgResponseTime > 8000 -> improvements.add("Processing Speed")
        }
        
        // Generate recommendations
        val recommendations = mutableListOf<String>()
        if (accuracy < 60) {
            recommendations.add("Try activities at a lower difficulty level")
            recommendations.add("Focus on single-object recognition first")
        }
        if (accuracy >= 80) {
            recommendations.add("Ready for more challenging activities")
            recommendations.add("Try introducing new activity types")
        }
        recommendations.add("Continue practicing for 10-15 minutes daily")
        
        // Generate summary
        val summary = buildString {
            append("Completed $totalAttempts activities with ${accuracy.toInt()}% accuracy. ")
            if (strengths.isNotEmpty()) {
                append("Strengths: ${strengths.joinToString()}. ")
            }
            if (improvements.isNotEmpty()) {
                append("Areas to work on: ${improvements.joinToString()}.")
            }
        }
        
        // Get unique session count
        val uniqueSessions = records.map { it.sessionId }.distinct().size
        
        return Result.success(
            ProgressInsights(
                profileId = profile.id,
                periodStartDate = startDate,
                periodEndDate = endDate,
                totalSessions = uniqueSessions,
                totalActivitiesCompleted = totalAttempts,
                averageAccuracy = accuracy,
                averageResponseTimeMs = avgResponseTime,
                averageSessionDurationMs = 0L, // Would need session data
                strongestVisualBehaviors = strengths,
                areasForImprovement = improvements,
                recommendations = recommendations.take(3),
                summary = summary
            )
        )
    }
    
    override suspend fun assessContentAppropriateness(
        content: String,
        profile: VisualProfile
    ): Result<ContentAssessment> {
        val concerns = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        // Check content length
        val wordCount = content.split(" ").size
        val maxWords = when (profile.cviPhase) {
            CVIPhase.PHASE_I -> 10
            CVIPhase.PHASE_II -> 25
            CVIPhase.PHASE_III -> 50
        }
        
        if (wordCount > maxWords) {
            concerns.add("Content may be too long for ${profile.cviPhase}")
            suggestions.add("Simplify to $maxWords words or fewer")
        }
        
        // Check for complex vocabulary
        val complexWords = content.split(" ").count { it.length > 8 }
        if (complexWords > 3 && profile.cviPhase != CVIPhase.PHASE_III) {
            concerns.add("Contains complex vocabulary")
            suggestions.add("Use simpler words")
        }
        
        // Calculate complexity score (1-5)
        val complexityScore = when {
            wordCount <= 10 && complexWords == 0 -> 1
            wordCount <= 20 && complexWords <= 2 -> 2
            wordCount <= 35 && complexWords <= 4 -> 3
            wordCount <= 50 -> 4
            else -> 5
        }
        
        // Determine appropriateness
        val isAppropriate = when (profile.cviPhase) {
            CVIPhase.PHASE_I -> complexityScore <= 2
            CVIPhase.PHASE_II -> complexityScore <= 3
            CVIPhase.PHASE_III -> complexityScore <= 5
        }
        
        if (!isAppropriate) {
            suggestions.add("Consider using the text simplification feature")
        }
        
        return Result.success(
            ContentAssessment(
                isAppropriate = isAppropriate,
                complexityScore = complexityScore,
                concerns = concerns,
                suggestions = suggestions
            )
        )
    }
}
