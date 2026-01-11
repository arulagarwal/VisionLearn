package com.visionlearn.domain.repository

import com.visionlearn.domain.model.ProgressRecord
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing progress records
 */
interface ProgressRepository {
    
    /**
     * Observe progress records for a profile
     */
    fun observeProgressByProfile(profileId: String): Flow<List<ProgressRecord>>
    
    /**
     * Get progress records for a profile
     */
    suspend fun getProgressByProfile(profileId: String): List<ProgressRecord>
    
    /**
     * Get progress records for a session
     */
    suspend fun getProgressBySession(sessionId: String): List<ProgressRecord>
    
    /**
     * Get recent progress records
     */
    suspend fun getRecentProgress(profileId: String, limit: Int = 50): List<ProgressRecord>
    
    /**
     * Record progress for an activity
     */
    suspend fun recordProgress(record: ProgressRecord): Result<ProgressRecord>
    
    /**
     * Update an existing progress record
     */
    suspend fun updateProgress(record: ProgressRecord): Result<ProgressRecord>
    
    /**
     * Get total correct answers for a profile
     */
    suspend fun getTotalCorrect(profileId: String): Int
    
    /**
     * Get total attempts for a profile
     */
    suspend fun getTotalAttempts(profileId: String): Int
    
    /**
     * Calculate accuracy percentage
     */
    suspend fun getAccuracyPercentage(profileId: String): Float
}
