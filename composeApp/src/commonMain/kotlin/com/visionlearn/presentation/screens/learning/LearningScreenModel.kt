package com.visionlearn.presentation.screens.learning

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.visionlearn.ai.AISessionGenerator
import com.visionlearn.ai.GeneratedQuestion
import com.visionlearn.ai.GeneratedSession
import com.visionlearn.ai.SessionPerformance
import com.visionlearn.ai.VisualConfig
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.domain.model.ProgressRecord
import com.visionlearn.domain.model.Session
import com.visionlearn.domain.repository.ProfileRepository
import com.visionlearn.domain.repository.ProgressRepository
import com.visionlearn.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

/**
 * State for learning session
 */
data class LearningState(
    val profileId: String? = null,
    val sessionId: String? = null,
    val currentQuestion: Int = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val isSessionActive: Boolean = false,
    val isLoading: Boolean = true,
    val isGeneratingSession: Boolean = false,
    val error: String? = null,
    val generatedSession: GeneratedSession? = null,
    val currentEncouragement: String? = null
)

/**
 * ScreenModel for learning activities with AI-powered session generation
 * Includes timeout and fallback for reliability
 */
class LearningScreenModel(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val progressRepository: ProgressRepository,
    private val sessionGenerator: AISessionGenerator
) : ScreenModel {
    
    private val _state = MutableStateFlow(LearningState())
    val state: StateFlow<LearningState> = _state.asStateFlow()
    
    private var currentSession: Session? = null
    
    companion object {
        private const val AI_TIMEOUT_MS = 12000L // 12 second timeout
    }
    
    /**
     * Generate an AI-powered session with timeout and fallback
     */
    fun generateAndStartSession(moduleType: ModuleType) {
        screenModelScope.launch {
            try {
                _state.update { it.copy(isGeneratingSession = true, isLoading = true, error = null) }
                
                // Get profile
                var profile = profileRepository.getActiveProfile()
                if (profile == null) {
                    profile = profileRepository.getAllProfiles().firstOrNull()
                }
                
                if (profile == null) {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            isGeneratingSession = false,
                            error = "No profile found. Please complete setup first."
                        ) 
                    }
                    return@launch
                }
                
                val recentSessions = try {
                    sessionRepository.getRecentSessions(profile.id, 5)
                } catch (e: Exception) {
                    emptyList()
                }
                val previousPerformance = calculatePerformance(recentSessions)
                
                
                // Try AI generation with timeout
                val sessionResult = withTimeoutOrNull(AI_TIMEOUT_MS) {
                    try {
                        sessionGenerator.generateSession(
                            profile = profile,
                            moduleType = moduleType,
                            previousPerformance = previousPerformance
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val generatedSession = sessionResult?.getOrNull()
                
                if (generatedSession != null && generatedSession.questions.isNotEmpty()) {
                    startWithSession(profile.id, moduleType, generatedSession)
                } else {
                    val fallback = createFallbackSession(moduleType)
                    startWithSession(profile.id, moduleType, fallback)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                
                // Last resort fallback
                try {
                    val profile = profileRepository.getActiveProfile() 
                        ?: profileRepository.getAllProfiles().firstOrNull()
                    if (profile != null) {
                        val fallback = createFallbackSession(moduleType)
                        startWithSession(profile.id, moduleType, fallback)
                    } else {
                        _state.update { 
                            it.copy(isLoading = false, isGeneratingSession = false, error = e.message) 
                        }
                    }
                } catch (e2: Exception) {
                    _state.update { 
                        it.copy(isLoading = false, isGeneratingSession = false, error = e.message) 
                    }
                }
            }
        }
    }
    
    /**
     * Start a session with generated content
     */
    private suspend fun startWithSession(
        profileId: String,
        moduleType: ModuleType,
        generatedSession: GeneratedSession
    ) {
        val session = Session.create(
            profileId = profileId,
            moduleType = moduleType
        ).copy(totalActivities = generatedSession.questions.size)
        
        try {
            sessionRepository.createSession(session)
        } catch (e: Exception) {
        }
        
        currentSession = session
        _state.update {
            it.copy(
                profileId = profileId,
                sessionId = session.id,
                totalQuestions = generatedSession.questions.size,
                currentQuestion = 0,
                correctAnswers = 0,
                isSessionActive = true,
                isLoading = false,
                isGeneratingSession = false,
                generatedSession = generatedSession,
                currentEncouragement = generatedSession.encouragementMessages.firstOrNull(),
                error = null
            )
        }
    }
    
    /**
     * Start with a custom activity (from Creator)
     */
    fun startCustomSession(customSession: GeneratedSession) {
        screenModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                
                var profile = profileRepository.getActiveProfile()
                if (profile == null) {
                    profile = profileRepository.getAllProfiles().firstOrNull()
                }
                
                val profileId = profile?.id ?: "default"
                val moduleType = try {
                    ModuleType.valueOf(customSession.moduleType)
                } catch (e: Exception) {
                    ModuleType.RECOGNITION
                }
                
                startWithSession(profileId, moduleType, customSession)
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isLoading = false, error = e.message) 
                }
            }
        }
    }
    
    fun getGeneratedQuestions(): List<GeneratedQuestion> {
        return _state.value.generatedSession?.questions ?: emptyList()
    }
    
    fun hasAISession(): Boolean {
        return _state.value.generatedSession != null
    }
    
    fun recordAnswer(activityId: String, isCorrect: Boolean, responseTimeMs: Long? = null) {
        val currentState = _state.value
        if (currentState.sessionId == null || currentState.profileId == null) return
        
        screenModelScope.launch {
            try {
                val record = ProgressRecord.create(
                    sessionId = currentState.sessionId,
                    profileId = currentState.profileId,
                    activityId = activityId,
                    activityType = "question"
                ).copy(
                    completedAt = Clock.System.now(),
                    responseTimeMs = responseTimeMs,
                    isCorrect = isCorrect,
                    score = if (isCorrect) 100f else 0f
                )
                
                progressRepository.recordProgress(record)
                
                val encouragements = currentState.generatedSession?.encouragementMessages ?: emptyList()
                val newEncouragement = if (isCorrect && encouragements.isNotEmpty()) {
                    encouragements.random()
                } else currentState.currentEncouragement
                
                // Only update correctAnswers here, NOT currentQuestion
                // currentQuestion is advanced when user clicks "Next"
                _state.update {
                    it.copy(
                        correctAnswers = if (isCorrect) it.correctAnswers + 1 else it.correctAnswers,
                        currentEncouragement = newEncouragement
                    )
                }
            } catch (e: Exception) {
            }
        }
    }
    
    /**
     * Advance to the next question - called when user clicks "Next"
     */
    fun advanceToNextQuestion() {
        _state.update {
            it.copy(currentQuestion = it.currentQuestion + 1)
        }
    }
    
    fun completeSession() {
        val currentState = _state.value
        val session = currentSession ?: return
        
        screenModelScope.launch {
            try {
                val updatedSession = session.copy(
                    endedAt = Clock.System.now(),
                    completedActivities = currentState.currentQuestion,
                    correctAnswers = currentState.correctAnswers
                )
                sessionRepository.updateSession(updatedSession)
                _state.update { it.copy(isSessionActive = false) }
            } catch (e: Exception) {
            }
        }
    }
    
    fun getCompletionMessage(): String {
        return _state.value.generatedSession?.completionMessage 
            ?: "Great job completing this session! 🎉"
    }
    
    fun resetSession() {
        currentSession = null
        _state.update { LearningState(isLoading = false) }
    }
    
    private fun calculatePerformance(sessions: List<Session>): SessionPerformance? {
        if (sessions.isEmpty()) return null
        val totalCorrect = sessions.sumOf { it.correctAnswers }
        val totalActivities = sessions.sumOf { it.completedActivities }
        val accuracy = if (totalActivities > 0) (totalCorrect * 100) / totalActivities else 0
        return SessionPerformance(
            accuracy = accuracy,
            avgResponseTimeMs = 3000L,
            successfulThemes = emptyList(),
            challengingAreas = emptyList(),
            totalSessionsCompleted = sessions.size
        )
    }
    
    /**
     * Create fallback questions when AI fails
     */
    private fun createFallbackSession(moduleType: ModuleType): GeneratedSession {
        val questions = when (moduleType) {
            ModuleType.RECOGNITION -> listOf(
                GeneratedQuestion("fb_1", "recognition", "Find the apple", "Can you find the apple?",
                    "🍎", listOf("🍌", "🍊"), "It's red!", "Great job! 🎉", "Look for red!", VisualConfig()),
                GeneratedQuestion("fb_2", "recognition", "Find the dog", "Can you find the dog?",
                    "🐕", listOf("🐱", "🐰"), "It barks!", "Woof woof! 🐕", "Which one barks?", VisualConfig()),
                GeneratedQuestion("fb_3", "recognition", "Find the car", "Can you find the car?",
                    "🚗", listOf("🚌", "✈️"), "It has wheels!", "Beep beep! 🚗", "Four wheels!", VisualConfig()),
                GeneratedQuestion("fb_4", "recognition", "Find the star", "Can you find the star?",
                    "⭐", listOf("🌙", "☀️"), "It twinkles!", "Twinkle! ⭐", "Five points!", VisualConfig())
            )
            ModuleType.CAUSE_EFFECT -> listOf(
                GeneratedQuestion("ce_1", "cause_effect", "Tap the dog!", "Tap to hear it bark!",
                    "🐕", emptyList(), "Touch it!", "Woof! Woof! 🐕", "Tap!", VisualConfig(useAnimation = true)),
                GeneratedQuestion("ce_2", "cause_effect", "Tap the bell!", "Tap to hear it ring!",
                    "🔔", emptyList(), "Touch it!", "Ring ring! 🔔", "Tap!", VisualConfig(useAnimation = true)),
                GeneratedQuestion("ce_3", "cause_effect", "Tap the cat!", "Tap to hear it meow!",
                    "🐱", emptyList(), "Touch it!", "Meow! 🐱", "Tap!", VisualConfig(useAnimation = true)),
                GeneratedQuestion("ce_4", "cause_effect", "Tap the star!", "Tap to see it sparkle!",
                    "⭐", emptyList(), "Touch it!", "Sparkle! ✨", "Tap!", VisualConfig(useAnimation = true))
            )
            ModuleType.SORTING -> listOf(
                GeneratedQuestion("sort_1", "sorting", "Sort: Fruits vs Vegetables", "Sort these items!",
                    "Fruits:🍎🍌|Vegetables:🥕🥦", listOf("🍎", "🍌", "🥕", "🥦"),
                    "Fruits are sweet!", "Perfect! 🎉", "Sweet vs not sweet!", VisualConfig()),
                GeneratedQuestion("sort_2", "sorting", "Sort: Animals vs Vehicles", "Sort these!",
                    "Animals:🐕🐱|Vehicles:🚗🚌", listOf("🐕", "🐱", "🚗", "🚌"),
                    "Animals are alive!", "Great! 🌟", "Alive vs machines!", VisualConfig())
            )
            ModuleType.MATCHING -> listOf(
                GeneratedQuestion("match_1", "matching", "Find matching pairs!", "Match the same items!",
                    "🍎-🍎,🐕-🐕,⭐-⭐", listOf("🍎", "🍎", "🐕", "🐕", "⭐", "⭐"),
                    "Find two same!", "All matched! 🎉", "Keep looking!", VisualConfig()),
                GeneratedQuestion("match_2", "matching", "Match the pairs!", "Find the matches!",
                    "🌙-🌙,🎈-🎈", listOf("🌙", "🌙", "🎈", "🎈"),
                    "Same pictures!", "Perfect! ⭐", "Try again!", VisualConfig())
            )
            ModuleType.SEQUENCING -> listOf(
                GeneratedQuestion("seq_1", "sequencing", "Order: 1, 2, 3", "Put numbers in order!",
                    "1️⃣ 2️⃣ 3️⃣", listOf("2️⃣", "1️⃣", "3️⃣"),
                    "Count up!", "1, 2, 3! 🎉", "Start from one!", VisualConfig()),
                GeneratedQuestion("seq_2", "sequencing", "Order: Small to Big", "Smallest to biggest!",
                    "🐭 🐱 🐘", listOf("🐘", "🐭", "🐱"),
                    "Start tiny!", "Perfect order! 🌟", "Tiny first!", VisualConfig())
            )
            else -> listOf(
                GeneratedQuestion("def_1", "recognition", "Find the heart", "Find the heart!",
                    "❤️", listOf("⭐", "🌙"), "It's red!", "Love it! ❤️", "Red shape!", VisualConfig())
            )
        }
        
        return GeneratedSession(
            id = "fallback_${Clock.System.now().toEpochMilliseconds()}",
            moduleType = moduleType.name,
            theme = "${moduleType.displayName} Practice",
            difficultyLevel = 2,
            questions = questions,
            estimatedDurationMinutes = 5,
            encouragementMessages = listOf("Great job! 🌟", "Keep going! 💪", "You're amazing! ✨", "Wonderful! 🎉"),
            completionMessage = "Fantastic work! You completed the session! 🏆"
        )
    }
}
