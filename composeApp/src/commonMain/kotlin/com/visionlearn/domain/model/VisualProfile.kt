package com.visionlearn.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a child's complete visual profile based on the Perkins CVI Protocol.
 * Contains all 16 Visual Behaviors and session preferences.
 */
@Serializable
data class VisualProfile(
    val id: String,
    val userId: String,
    val childName: String,
    val childAge: Int? = null,
    val cviPhase: CVIPhase = CVIPhase.PHASE_II,
    
    // ===== 16 Visual Behaviors (scale 1-5, where 1 = most impacted) =====
    
    // 1. Visual Attention - ability to maintain gaze
    val visualAttention: Int = 3,
    
    // 2. Visual Recognition - identifying and making meaning
    val visualRecognition: Int = 3,
    
    // 3. Visual Curiosity - interest in exploring visually
    val visualCuriosity: Int = 3,
    
    // 4. Object Complexity - difficulty with complex objects
    val objectComplexity: Int = 3,
    
    // 5. Array Complexity - difficulty with cluttered environments
    val arrayComplexity: Int = 3,
    
    // 6. Sensory Complexity - overwhelm from multi-sensory input
    val sensoryComplexity: Int = 3,
    
    // 7. Light - need for or sensitivity to light
    val lightPreference: LightPreference = LightPreference.NEUTRAL,
    
    // 8. Color - preference for specific colors
    val preferredColors: List<String> = listOf("yellow", "red"),
    
    // 9. Movement - need movement to notice objects
    val movementNeed: Int = 3,
    
    // 10. Visual Latency - delay in visual recognition
    val visualLatency: LatencyLevel = LatencyLevel.MILD,
    
    // 11. Visual Field Preference - where the child sees best
    val visualFieldPreference: VisualField = VisualField.CENTER,
    
    // 12. Distance Viewing - difficulty seeing at distance (1=close only, 5=any distance)
    val distanceViewing: Int = 3,
    
    // 13. Visual-Motor Integration - coordinating vision with reaching
    val visualMotorIntegration: Int = 3,
    
    // 14. Visual Reflexes - blink reflex status
    val visualReflexes: ReflexStatus = ReflexStatus.TYPICAL,
    
    // 15. Novelty - difficulty with unfamiliar items (1=familiar only, 5=accepts novel)
    val noveltyTolerance: Int = 3,
    
    // 16. Faces/People - difficulty recognizing faces (1=most difficulty)
    val faceRecognition: Int = 3,
    
    // ===== Session Preferences =====
    val sessionDurationMinutes: Int = 15,
    val inputMethod: InputMethod = InputMethod.TOUCH,
    val audioFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val ttsRate: Float = 1.0f,
    
    // ===== Timestamps =====
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    /**
     * Calculate recommended response timeout based on visual latency
     */
    fun getResponseTimeout(): Long = when (visualLatency) {
        LatencyLevel.NONE -> 3000L
        LatencyLevel.MILD -> 5000L
        LatencyLevel.MODERATE -> 8000L
        LatencyLevel.SIGNIFICANT -> 12000L
    }
    
    /**
     * Get recommended number of items to display based on array complexity
     */
    fun getMaxDisplayItems(): Int = when {
        arrayComplexity <= 1 -> 1
        arrayComplexity == 2 -> 2
        arrayComplexity == 3 -> 4
        arrayComplexity == 4 -> 6
        else -> 9
    }
    
    /**
     * Get recommended card/object size based on CVI phase
     */
    fun getRecommendedSizeDp(): Int = when (cviPhase) {
        CVIPhase.PHASE_I -> 280
        CVIPhase.PHASE_II -> 200
        CVIPhase.PHASE_III -> 150
    }
    
    /**
     * Get recommended font size based on CVI phase
     */
    fun getRecommendedFontSizeSp(): Int = when (cviPhase) {
        CVIPhase.PHASE_I -> 28
        CVIPhase.PHASE_II -> 24
        CVIPhase.PHASE_III -> 20
    }
    
    /**
     * Check if animations should be enabled based on movement need
     */
    fun shouldEnableAnimations(): Boolean = movementNeed >= 3
    
    /**
     * Get primary preferred color or default
     */
    fun getPrimaryColor(): String = preferredColors.firstOrNull() ?: "yellow"
    
    companion object {
        /**
         * Create a new profile with generated ID
         */
        fun create(
            userId: String,
            childName: String,
            cviPhase: CVIPhase = CVIPhase.PHASE_II,
            preferredColors: List<String> = listOf("yellow")
        ): VisualProfile {
            val now = Clock.System.now()
            return VisualProfile(
                id = generateId(),
                userId = userId,
                childName = childName,
                cviPhase = cviPhase,
                preferredColors = preferredColors,
                createdAt = now,
                updatedAt = now
            )
        }
        
        private fun generateId(): String {
            return "profile_${Clock.System.now().toEpochMilliseconds()}"
        }
    }
}
