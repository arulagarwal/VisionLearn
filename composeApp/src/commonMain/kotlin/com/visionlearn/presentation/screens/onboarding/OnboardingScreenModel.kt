package com.visionlearn.presentation.screens.onboarding

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.visionlearn.domain.model.CVIPhase
import com.visionlearn.domain.model.VisualProfile
import com.visionlearn.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * State for onboarding flow
 */
data class OnboardingState(
    val currentStep: Int = 0,
    val childName: String = "",
    val selectedPhase: CVIPhase? = null,
    val selectedColors: List<String> = listOf("yellow"),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
) {
    val totalSteps: Int = 4
    
    val canProceed: Boolean
        get() = when (currentStep) {
            0 -> true // Welcome
            1 -> childName.isNotBlank() // Name
            2 -> selectedPhase != null // Phase
            3 -> selectedColors.isNotEmpty() // Colors
            else -> true
        }
    
    val progress: Float
        get() = (currentStep + 1).toFloat() / totalSteps
}

/**
 * ScreenModel for onboarding flow
 * Manages profile creation state with robust error handling
 */
class OnboardingScreenModel(
    private val profileRepository: ProfileRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    
    fun nextStep() {
        _state.update { 
            if (it.currentStep < it.totalSteps - 1) {
                it.copy(currentStep = it.currentStep + 1)
            } else {
                it
            }
        }
    }
    
    fun previousStep() {
        _state.update {
            if (it.currentStep > 0) {
                it.copy(currentStep = it.currentStep - 1)
            } else {
                it
            }
        }
    }
    
    fun updateChildName(name: String) {
        _state.update { it.copy(childName = name) }
    }
    
    fun selectPhase(phase: CVIPhase) {
        _state.update { it.copy(selectedPhase = phase) }
    }
    
    fun toggleColor(color: String) {
        _state.update { current ->
            val newColors = if (color in current.selectedColors) {
                current.selectedColors - color
            } else {
                current.selectedColors + color
            }
            current.copy(selectedColors = newColors.ifEmpty { listOf("yellow") })
        }
    }
    
    fun completeOnboarding() {
        val currentState = _state.value
        
        if (currentState.childName.isBlank() || currentState.selectedPhase == null) {
            _state.update { it.copy(error = "Please complete all fields") }
            return
        }
        
        _state.update { it.copy(isLoading = true, error = null) }
        
        screenModelScope.launch {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val userId = "user_$now"
                
                
                // Try to create user, but don't fail if it already exists
                try {
                    profileRepository.createDefaultUser(userId, "Parent")
                } catch (e: Exception) {
                    // Continue anyway - user might already exist
                }
                
                // Create visual profile
                val profile = VisualProfile.create(
                    userId = userId,
                    childName = currentState.childName,
                    cviPhase = currentState.selectedPhase!!,
                    preferredColors = currentState.selectedColors
                )
                
                
                try {
                    profileRepository.saveProfile(profile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
                
                try {
                    profileRepository.setActiveProfile(profile.id)
                } catch (e: Exception) {
                    // Non-fatal - continue
                }
                
                _state.update { it.copy(isLoading = false, isComplete = true) }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Failed to save profile: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
