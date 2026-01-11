package com.visionlearn.domain.repository

import com.visionlearn.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing learning sessions
 */
interface SessionRepository {
    
    /**
     * Get sessions for a profile
     */
    fun getSessionsByProfile(profileId: String): Flow<List<Session>>
    
    /**
     * Get recent sessions
     */
    suspend fun getRecentSessions(profileId: String, limit: Int = 10): List<Session>
    
    /**
     * Get a session by ID
     */
    suspend fun getSessionById(sessionId: String): Session?
    
    /**
     * Create a new session
     */
    suspend fun createSession(session: Session): Result<Session>
    
    /**
     * Update a session (e.g., when completed)
     */
    suspend fun updateSession(session: Session): Result<Session>
    
    /**
     * Delete a session
     */
    suspend fun deleteSession(sessionId: String): Result<Unit>
}
