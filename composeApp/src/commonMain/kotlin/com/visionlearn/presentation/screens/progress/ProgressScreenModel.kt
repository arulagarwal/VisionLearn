package com.visionlearn.presentation.screens.progress

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.domain.model.Session
import com.visionlearn.domain.repository.ProfileRepository
import com.visionlearn.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Session summary for display
 */
data class SessionSummary(
    val date: String,
    val moduleName: String,
    val activitiesCompleted: Int,
    val accuracy: Int
)

/**
 * State for progress screen
 */
data class ProgressState(
    val childName: String = "",
    val totalSessions: Int = 0,
    val totalActivities: Int = 0,
    val averageAccuracy: Int = 0,
    val recentSessions: List<SessionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ScreenModel for progress screen
 */
class ProgressScreenModel(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state.asStateFlow()
    
    init {
        loadProgress()
    }
    
    private fun loadProgress() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val profile = profileRepository.getActiveProfile()
                
                if (profile != null) {
                    val sessions = sessionRepository.getRecentSessions(profile.id, 20)
                    
                    // Calculate stats
                    val totalSessions = sessions.size
                    val totalActivities = sessions.sumOf { it.completedActivities }
                    val avgAccuracy = if (sessions.isNotEmpty()) {
                        sessions.map { it.accuracyPercentage }.average().toInt()
                    } else 0
                    
                    // Convert sessions to summaries
                    val summaries = sessions.take(10).map { session ->
                        SessionSummary(
                            date = formatDate(session),
                            moduleName = session.moduleType?.displayName ?: "Learning",
                            activitiesCompleted = session.completedActivities,
                            accuracy = session.accuracyPercentage.toInt()
                        )
                    }
                    
                    _state.update {
                        it.copy(
                            childName = profile.childName,
                            totalSessions = totalSessions,
                            totalActivities = totalActivities,
                            averageAccuracy = avgAccuracy,
                            recentSessions = summaries,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isLoading = false, error = "Failed to load progress: ${e.message}") 
                }
            }
        }
    }
    
    fun refresh() {
        loadProgress()
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun formatDate(session: Session): String {
        val now = Clock.System.now()
        val sessionDate = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault())
        
        return when {
            sessionDate.date == today.date -> "Today"
            sessionDate.date.toEpochDays() == today.date.toEpochDays() - 1 -> "Yesterday"
            else -> "${sessionDate.month.name.take(3)} ${sessionDate.dayOfMonth}"
        }
    }
}
