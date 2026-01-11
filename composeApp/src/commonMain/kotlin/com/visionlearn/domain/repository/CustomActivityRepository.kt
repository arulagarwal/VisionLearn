package com.visionlearn.domain.repository

import com.visionlearn.domain.model.ModuleType
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing a custom learning activity created by parent/teacher
 */
data class CustomActivity(
    val id: String,
    val title: String,
    val moduleType: ModuleType,
    val images: List<CustomActivityImage>,
    val createdAt: Long,
    val isFromTemplate: Boolean = false,
    val templateId: String? = null
)

/**
 * Data class for images in a custom activity
 */
data class CustomActivityImage(
    val id: String,
    val emoji: String,
    val name: String,
    val category: String? = null,
    val aiDescription: String? = null,
    val complexityScore: Int = 2
)

/**
 * Repository interface for managing custom learning activities
 */
interface CustomActivityRepository {
    
    /**
     * Get all custom activities
     */
    fun getAllActivities(): Flow<List<CustomActivity>>
    
    /**
     * Get activities by module type
     */
    suspend fun getActivitiesByType(moduleType: ModuleType): List<CustomActivity>
    
    /**
     * Get a specific activity by ID
     */
    suspend fun getActivityById(id: String): CustomActivity?
    
    /**
     * Create a new activity
     */
    suspend fun createActivity(activity: CustomActivity): Result<CustomActivity>
    
    /**
     * Update an existing activity
     */
    suspend fun updateActivity(activity: CustomActivity): Result<CustomActivity>
    
    /**
     * Delete an activity
     */
    suspend fun deleteActivity(id: String): Result<Unit>
    
    /**
     * Get count of custom activities
     */
    suspend fun getActivityCount(): Int
}
