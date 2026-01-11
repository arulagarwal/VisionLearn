package com.visionlearn.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a learning session
 */
@Serializable
data class Session(
    val id: String,
    val profileId: String,
    val moduleType: ModuleType? = null,
    val startedAt: Instant = Clock.System.now(),
    val endedAt: Instant? = null,
    val totalActivities: Int = 0,
    val completedActivities: Int = 0,
    val correctAnswers: Int = 0,
    val averageResponseTimeMs: Long? = null,
    val notes: String? = null
) {
    val isCompleted: Boolean get() = endedAt != null
    
    val accuracyPercentage: Float
        get() = if (completedActivities > 0) {
            (correctAnswers.toFloat() / completedActivities) * 100
        } else 0f
    
    val durationMs: Long?
        get() = endedAt?.let { it.toEpochMilliseconds() - startedAt.toEpochMilliseconds() }
    
    companion object {
        fun create(
            profileId: String,
            moduleType: ModuleType? = null
        ): Session {
            return Session(
                id = "session_${Clock.System.now().toEpochMilliseconds()}",
                profileId = profileId,
                moduleType = moduleType,
                startedAt = Clock.System.now()
            )
        }
    }
}

/**
 * Represents a single progress record for an activity
 */
@Serializable
data class ProgressRecord(
    val id: String,
    val sessionId: String,
    val profileId: String,
    val activityId: String,
    val activityType: String? = null,
    val startedAt: Instant = Clock.System.now(),
    val completedAt: Instant? = null,
    val responseTimeMs: Long? = null,
    val isCorrect: Boolean? = null,
    val score: Float? = null,
    val attempts: Int = 1
) {
    val isComplete: Boolean get() = completedAt != null
    
    companion object {
        fun create(
            sessionId: String,
            profileId: String,
            activityId: String,
            activityType: String? = null
        ): ProgressRecord {
            return ProgressRecord(
                id = "progress_${Clock.System.now().toEpochMilliseconds()}",
                sessionId = sessionId,
                profileId = profileId,
                activityId = activityId,
                activityType = activityType,
                startedAt = Clock.System.now()
            )
        }
    }
}

/**
 * AI-generated insights from progress data
 */
@Serializable
data class ProgressInsights(
    val profileId: String,
    val periodStartDate: Long,
    val periodEndDate: Long,
    val totalSessions: Int,
    val totalActivitiesCompleted: Int,
    val averageAccuracy: Float,
    val averageResponseTimeMs: Long,
    val averageSessionDurationMs: Long,
    val strongestVisualBehaviors: List<String>,
    val areasForImprovement: List<String>,
    val recommendations: List<String>,
    val summary: String = ""
)
