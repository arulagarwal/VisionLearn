package com.visionlearn.domain.repository

import com.visionlearn.domain.model.Activity
import com.visionlearn.domain.model.LearningModule
import com.visionlearn.domain.model.ModuleType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing learning modules and activities
 */
interface ActivityRepository {
    
    // ===== Module Operations =====
    
    /**
     * Get all available modules
     */
    fun getAllModules(): Flow<List<LearningModule>>
    
    /**
     * Get modules by type
     */
    fun getModulesByType(type: ModuleType): Flow<List<LearningModule>>
    
    /**
     * Get template modules (pre-built activities)
     */
    fun getTemplateModules(): Flow<List<LearningModule>>
    
    /**
     * Get a single module by ID
     */
    suspend fun getModuleById(id: String): LearningModule?
    
    /**
     * Create a new module
     */
    suspend fun createModule(module: LearningModule): Result<LearningModule>
    
    /**
     * Delete a module
     */
    suspend fun deleteModule(id: String): Result<Unit>
    
    // ===== Activity Operations =====
    
    /**
     * Get activities for a module
     */
    fun getActivitiesByModule(moduleId: String): Flow<List<Activity>>
    
    /**
     * Get a single activity by ID
     */
    suspend fun getActivityById(id: String): Activity?
    
    /**
     * Create a new activity
     */
    suspend fun createActivity(activity: Activity): Result<Activity>
    
    /**
     * Delete an activity
     */
    suspend fun deleteActivity(id: String): Result<Unit>
}
