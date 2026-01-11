package com.visionlearn.platform

/**
 * Multiplatform Text-to-Speech interface
 * Provides audio feedback for accessibility
 */
expect class TextToSpeech {
    /**
     * Speak the given text aloud
     * @param text The text to speak
     * @param flush If true, stops any current speech first
     */
    fun speak(text: String, flush: Boolean = true)
    
    /**
     * Stop any current speech
     */
    fun stop()
    
    /**
     * Set the speech rate
     * @param rate Speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double)
     */
    fun setRate(rate: Float)
    
    /**
     * Set the pitch
     * @param pitch Pitch (0.5 = low, 1.0 = normal, 2.0 = high)
     */
    fun setPitch(pitch: Float)
    
    /**
     * Check if TTS is currently speaking
     */
    val isSpeaking: Boolean
    
    /**
     * Check if TTS is available on this device
     */
    val isAvailable: Boolean
    
    /**
     * Release resources when done
     */
    fun shutdown()
}

/**
 * Queue mode for TTS
 */
enum class TTSQueueMode {
    FLUSH,  // Stop current speech and speak immediately
    ADD     // Add to queue after current speech
}

/**
 * Helper for accessibility announcements
 */
expect class AccessibilityAnnouncer {
    /**
     * Make an accessibility announcement
     * Will be read by screen readers (TalkBack/VoiceOver)
     */
    fun announce(message: String)
}
