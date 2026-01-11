package com.visionlearn.domain.model

/**
 * Represents the three phases of CVI (Cortical Visual Impairment)
 * based on the CVI Range assessment scale (0-10)
 */
enum class CVIPhase(val range: IntRange, val description: String) {
    PHASE_I(0..3, "Building Visual Behavior - Most severe impact"),
    PHASE_II(4..6, "Integrating Vision with Function - Moderate impact"),
    PHASE_III(7..10, "Resolving CVI Characteristics - Least impact");
    
    companion object {
        fun fromScore(score: Int): CVIPhase = when {
            score <= 3 -> PHASE_I
            score <= 6 -> PHASE_II
            else -> PHASE_III
        }
    }
}

/**
 * Light sensitivity/preference levels
 */
enum class LightPreference(val description: String) {
    NEEDS_MORE("Benefits from additional lighting/backlighting"),
    SENSITIVE("Light-sensitive, prefers dim environments"),
    NEUTRAL("No strong preference")
}

/**
 * Visual latency levels - time between presentation and recognition
 */
enum class LatencyLevel(val typicalDelayMs: Long, val description: String) {
    NONE(0, "No noticeable delay"),
    MILD(1500, "Slight delay (1-2 seconds)"),
    MODERATE(3000, "Moderate delay (2-4 seconds)"),
    SIGNIFICANT(5000, "Significant delay (4+ seconds)")
}

/**
 * Visual field preference - where the child sees best
 */
enum class VisualField(val description: String) {
    CENTER("Central vision"),
    LEFT("Left visual field"),
    RIGHT("Right visual field"),
    UPPER("Upper visual field"),
    LOWER("Lower visual field"),
    UPPER_LEFT("Upper left quadrant"),
    UPPER_RIGHT("Upper right quadrant"),
    LOWER_LEFT("Lower left quadrant"),
    LOWER_RIGHT("Lower right quadrant")
}

/**
 * Visual reflex status
 */
enum class ReflexStatus(val description: String) {
    TYPICAL("Normal visual reflexes"),
    DELAYED("Delayed visual reflexes"),
    ABSENT("Absent visual reflexes")
}

/**
 * Input method preference for interacting with the app
 */
enum class InputMethod(val description: String) {
    TOUCH("Standard touch interaction"),
    SWITCH_SINGLE("Single switch scanning"),
    SWITCH_DUAL("Two-switch step scanning"),
    VOICE("Voice commands"),
    EYE_GAZE("Eye gaze tracking")
}

/**
 * User role in the application
 */
enum class UserRole {
    PARENT,
    TEACHER,
    THERAPIST,
    CHILD
}

/**
 * Learning module types
 */
enum class ModuleType(val displayName: String, val description: String) {
    RECOGNITION("Image Recognition", "Identify single objects"),
    SORTING("Sorting", "Group objects by category"),
    MATCHING("Matching", "Find matching pairs"),
    SEQUENCING("Sequencing", "Arrange items in order"),
    CAUSE_EFFECT("Cause & Effect", "Touch to see response"),
    CUSTOM("Custom", "Parent/teacher created activity")
}

/**
 * Media asset types
 */
enum class MediaType {
    IMAGE,
    AUDIO,
    VIDEO
}
