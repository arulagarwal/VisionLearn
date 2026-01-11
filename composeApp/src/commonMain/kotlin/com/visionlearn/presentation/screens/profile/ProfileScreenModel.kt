package com.visionlearn.presentation.screens.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.visionlearn.domain.model.*
import com.visionlearn.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for profile screen
 */
data class ProfileState(
    val profile: VisualProfile? = null,
    val childName: String = "",
    val childAge: Int? = null,
    val cviPhase: CVIPhase = CVIPhase.PHASE_II,
    val sessionDuration: Int = 15,
    val inputMethod: InputMethod = InputMethod.TOUCH,
    val audioEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val ttsRate: Float = 1.0f,
    val preferredColors: List<String> = listOf("yellow"),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * ScreenModel for profile screen
 */
class ProfileScreenModel(
    private val profileRepository: ProfileRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init {
        loadProfile()
    }
    
    private fun loadProfile() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val profile = profileRepository.getActiveProfile()
                
                if (profile != null) {
                    _state.update {
                        it.copy(
                            profile = profile,
                            childName = profile.childName,
                            childAge = profile.childAge,
                            cviPhase = profile.cviPhase,
                            sessionDuration = profile.sessionDurationMinutes,
                            inputMethod = profile.inputMethod,
                            audioEnabled = profile.audioFeedbackEnabled,
                            hapticEnabled = profile.hapticFeedbackEnabled,
                            ttsRate = profile.ttsRate,
                            preferredColors = profile.preferredColors,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isLoading = false, error = "Failed to load profile: ${e.message}") 
                }
            }
        }
    }
    
    fun updateChildName(name: String) {
        _state.update { it.copy(childName = name, saveSuccess = false) }
    }
    
    fun updateChildAge(age: Int?) {
        _state.update { it.copy(childAge = age, saveSuccess = false) }
    }
    
    fun updateCVIPhase(phase: CVIPhase) {
        _state.update { it.copy(cviPhase = phase, saveSuccess = false) }
    }
    
    fun updateSessionDuration(duration: Int) {
        _state.update { it.copy(sessionDuration = duration, saveSuccess = false) }
    }
    
    fun updateInputMethod(method: InputMethod) {
        _state.update { it.copy(inputMethod = method, saveSuccess = false) }
    }
    
    fun updateAudioEnabled(enabled: Boolean) {
        _state.update { it.copy(audioEnabled = enabled, saveSuccess = false) }
    }
    
    fun updateHapticEnabled(enabled: Boolean) {
        _state.update { it.copy(hapticEnabled = enabled, saveSuccess = false) }
    }
    
    fun updateTTSRate(rate: Float) {
        _state.update { it.copy(ttsRate = rate, saveSuccess = false) }
    }
    
    fun toggleColor(color: String) {
        _state.update { current ->
            val newColors = if (color in current.preferredColors) {
                current.preferredColors - color
            } else {
                current.preferredColors + color
            }
            current.copy(
                preferredColors = newColors.ifEmpty { listOf("yellow") },
                saveSuccess = false
            )
        }
    }
    
    fun saveProfile() {
        val currentState = _state.value
        val currentProfile = currentState.profile ?: return
        
        _state.update { it.copy(isSaving = true, error = null) }
        
        screenModelScope.launch {
            try {
                val updatedProfile = currentProfile.copy(
                    childName = currentState.childName,
                    childAge = currentState.childAge,
                    cviPhase = currentState.cviPhase,
                    sessionDurationMinutes = currentState.sessionDuration,
                    inputMethod = currentState.inputMethod,
                    audioFeedbackEnabled = currentState.audioEnabled,
                    hapticFeedbackEnabled = currentState.hapticEnabled,
                    ttsRate = currentState.ttsRate,
                    preferredColors = currentState.preferredColors
                )
                
                profileRepository.updateProfile(updatedProfile)
                
                _state.update { 
                    it.copy(
                        profile = updatedProfile,
                        isSaving = false,
                        saveSuccess = true
                    ) 
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}") 
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }
}
