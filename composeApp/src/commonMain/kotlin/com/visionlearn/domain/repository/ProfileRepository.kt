package com.visionlearn.domain.repository

import com.visionlearn.domain.model.User
import com.visionlearn.domain.model.VisualProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing visual profiles
 */
interface ProfileRepository {
    /**
     * Get all profiles for a user
     */
    suspend fun getProfilesByUser(userId: String): List<VisualProfile>
    
    /**
     * Get a specific profile by ID
     */
    suspend fun getProfile(profileId: String): VisualProfile?
    
    /**
     * Get the currently active profile
     */
    suspend fun getActiveProfile(): VisualProfile?
    
    /**
     * Observe the active profile for reactive updates
     */
    fun observeActiveProfile(): Flow<VisualProfile?>
    
    /**
     * Get all profiles
     */
    suspend fun getAllProfiles(): List<VisualProfile>
    
    /**
     * Save a new profile
     */
    suspend fun saveProfile(profile: VisualProfile)
    
    /**
     * Update an existing profile
     */
    suspend fun updateProfile(profile: VisualProfile)
    
    /**
     * Set a profile as the active profile
     */
    suspend fun setActiveProfile(profileId: String)
    
    /**
     * Delete a profile
     */
    suspend fun deleteProfile(profileId: String)
    
    /**
     * Create a default user (for first-time setup)
     */
    suspend fun createDefaultUser(userId: String, name: String)
    
    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): User?
}
