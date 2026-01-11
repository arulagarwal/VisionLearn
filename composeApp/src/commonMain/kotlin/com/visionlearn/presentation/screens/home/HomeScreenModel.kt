package com.visionlearn.presentation.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.visionlearn.domain.model.VisualProfile
import com.visionlearn.domain.repository.CustomActivity
import com.visionlearn.domain.repository.CustomActivityRepository
import com.visionlearn.domain.repository.ProfileRepository
import com.visionlearn.domain.repository.ProgressRepository
import com.visionlearn.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for home screen
 */
data class HomeState(
    val profile: VisualProfile? = null,
    val totalSessions: Int = 0,
    val totalActivities: Int = 0,
    val averageAccuracy: Int = 0,
    val customActivities: List<CustomActivity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val childName: String
        get() = profile?.childName ?: "Friend"
    
    val hasCustomActivities: Boolean
        get() = customActivities.isNotEmpty()
}

/**
 * ScreenModel for home screen
 * Now with custom activities support and robust error handling
 */
class HomeScreenModel(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val progressRepository: ProgressRepository,
    private val customActivityRepository: CustomActivityRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    init {
        // Don't call loadData() in init - let the screen trigger it
        // This prevents crashes during construction
    }
    
    fun initialize() {
        loadData()
        observeCustomActivities()
    }
    
    private fun loadData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                
                // Load active profile
                var profile: VisualProfile? = null
                try {
                    profile = profileRepository.getActiveProfile()
                } catch (e: Exception) {
                }
                
                // If no active profile, try to get any profile
                if (profile == null) {
                    try {
                        val allProfiles = profileRepository.getAllProfiles()
                        profile = allProfiles.firstOrNull()
                        
                        // Set it as active if found
                        if (profile != null) {
                            try {
                                profileRepository.setActiveProfile(profile.id)
                            } catch (e: Exception) {
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                
                
                // Load stats if profile exists
                val stats = if (profile != null) {
                    loadStats(profile.id)
                } else {
                    StatsData()
                }
                
                
                _state.update {
                    it.copy(
                        profile = profile,
                        totalSessions = stats.sessions,
                        totalActivities = stats.activities,
                        averageAccuracy = stats.accuracy,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun observeCustomActivities() {
        screenModelScope.launch {
            try {
                customActivityRepository.getAllActivities()
                    .catch { e ->
                        emit(emptyList())
                    }
                    .collect { activities ->
                        _state.update { it.copy(customActivities = activities) }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun loadStats(profileId: String): StatsData {
        return try {
            // Get completed sessions (where endedAt is not null)
            val sessions = try {
                sessionRepository.getRecentSessions(profileId, 1000)
            } catch (e: Exception) {
                emptyList()
            }
            
            val completedSessions = sessions.filter { it.endedAt != null }
            
            // Get all progress records for accuracy calculation
            val progress = try {
                progressRepository.getProgressByProfile(profileId)
            } catch (e: Exception) {
                emptyList()
            }
            
            val totalCorrect = progress.count { it.isCorrect == true }
            val totalAttempts = progress.size
            val accuracy = if (totalAttempts > 0) {
                (totalCorrect * 100) / totalAttempts
            } else {
                0
            }
            
            
            StatsData(
                sessions = completedSessions.size,
                activities = totalAttempts,
                accuracy = accuracy
            )
        } catch (e: Exception) {
            e.printStackTrace()
            StatsData()
        }
    }
    
    fun refresh() {
        loadData()
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

private data class StatsData(
    val sessions: Int = 0,
    val activities: Int = 0,
    val accuracy: Int = 0
)
