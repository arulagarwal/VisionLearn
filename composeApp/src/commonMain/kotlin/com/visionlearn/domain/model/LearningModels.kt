package com.visionlearn.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a user of the application (parent, teacher, therapist, or child).
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val role: UserRole = UserRole.PARENT,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    companion object {
        fun create(name: String, role: UserRole = UserRole.PARENT): User {
            return User(
                id = "user_${Clock.System.now().toEpochMilliseconds()}",
                name = name,
                role = role
            )
        }
    }
}

/**
 * Represents a learning module containing multiple activities.
 */
@Serializable
data class LearningModule(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: ModuleType,
    val difficulty: Int = 1, // 1-5
    val minCVIPhase: CVIPhase = CVIPhase.PHASE_I,
    val createdBy: String,
    val isTemplate: Boolean = false,
    val isPublic: Boolean = false,
    val thumbnailPath: String? = null,
    val estimatedDurationMinutes: Int = 5,
    val targetVisualBehaviors: List<String> = emptyList(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    /**
     * Check if this module is appropriate for the given profile.
     */
    fun isAppropriateFor(profile: VisualProfile): Boolean {
        return when (profile.cviPhase) {
            CVIPhase.PHASE_I -> minCVIPhase == CVIPhase.PHASE_I
            CVIPhase.PHASE_II -> minCVIPhase != CVIPhase.PHASE_III
            CVIPhase.PHASE_III -> true
        }
    }
    
    companion object {
        fun create(
            title: String,
            type: ModuleType,
            createdBy: String,
            description: String? = null
        ): LearningModule {
            return LearningModule(
                id = "module_${Clock.System.now().toEpochMilliseconds()}",
                title = title,
                type = type,
                createdBy = createdBy,
                description = description
            )
        }
    }
}

/**
 * Represents a single activity within a learning module.
 */
@Serializable
data class Activity(
    val id: String,
    val moduleId: String,
    val title: String,
    val instructions: String? = null,
    val audioInstructionPath: String? = null,
    val content: ActivityContent,
    val orderIndex: Int = 0,
    val targetVisualBehaviors: List<String> = emptyList(),
    val createdAt: Instant = Clock.System.now()
) {
    companion object {
        fun create(
            moduleId: String,
            title: String,
            content: ActivityContent,
            orderIndex: Int = 0
        ): Activity {
            return Activity(
                id = "activity_${Clock.System.now().toEpochMilliseconds()}",
                moduleId = moduleId,
                title = title,
                content = content,
                orderIndex = orderIndex
            )
        }
    }
}

/**
 * Content for different activity types.
 */
@Serializable
sealed class ActivityContent {
    
    /**
     * Image recognition activity - identify a single object.
     */
    @Serializable
    data class ImageRecognition(
        val imageUrl: String,
        val correctAnswer: String,
        val aiDescription: String? = null,
        val options: List<String> = emptyList(), // Empty = open response
        val audioPrompt: String? = null
    ) : ActivityContent()
    
    /**
     * Sorting activity - categorize items.
     */
    @Serializable
    data class Sorting(
        val items: List<SortableItem>,
        val categories: List<String>,
        val instructions: String? = null
    ) : ActivityContent()
    
    /**
     * Matching activity - find pairs.
     */
    @Serializable
    data class Matching(
        val pairs: List<MatchPair>,
        val showAllAtOnce: Boolean = false
    ) : ActivityContent()
    
    /**
     * Sequencing activity - arrange in order.
     */
    @Serializable
    data class Sequencing(
        val items: List<SequenceItem>,
        val correctOrder: List<Int>
    ) : ActivityContent()
    
    /**
     * Cause and effect activity - touch to see response.
     */
    @Serializable
    data class CauseEffect(
        val triggerImageUrl: String,
        val responseImageUrl: String? = null,
        val responseAudioUrl: String? = null,
        val responseAnimation: String? = null
    ) : ActivityContent()
}

@Serializable
data class SortableItem(
    val id: String,
    val imageUrl: String,
    val label: String,
    val category: String,
    val audioLabel: String? = null
)

@Serializable
data class MatchPair(
    val id: String,
    val firstImageUrl: String,
    val secondImageUrl: String,
    val label: String? = null
)

@Serializable
data class SequenceItem(
    val id: String,
    val imageUrl: String,
    val label: String,
    val position: Int
)
