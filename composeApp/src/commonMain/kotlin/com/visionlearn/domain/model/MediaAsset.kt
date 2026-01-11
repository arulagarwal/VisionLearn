package com.visionlearn.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a media asset (image, audio, or video)
 */
@Serializable
data class MediaAsset(
    val id: String,
    val activityId: String? = null,
    val userId: String,
    val filePath: String,
    val type: MediaType,
    val originalComplexity: Int? = null, // AI-assessed complexity 1-5
    val aiDescription: String? = null,
    val aiSimplifiedPath: String? = null,
    val altText: String? = null,
    val dominantColors: List<String> = emptyList(),
    val createdAt: Long = 0
)

/**
 * Result from AI image analysis
 */
@Serializable
data class ImageAnalysisResult(
    val shortDescription: String,
    val detailedDescription: String,
    val identifiedObjects: List<String>,
    val dominantColors: List<String>,
    val complexityScore: Int, // 1-5
    val recommendedPhase: CVIPhase,
    val simplificationSuggestions: List<String>
)
