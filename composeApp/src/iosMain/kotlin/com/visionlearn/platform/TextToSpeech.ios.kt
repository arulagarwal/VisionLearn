package com.visionlearn.platform

import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityAnnouncementNotification

/**
 * iOS implementation of Text-to-Speech using AVSpeechSynthesizer
 */
actual class TextToSpeech {
    
    private var synthesizer: AVSpeechSynthesizer? = null
    private var currentRate: Float = 0.5f  // AVSpeech default rate
    private var currentPitch: Float = 1.0f
    
    private fun getSynthesizer(): AVSpeechSynthesizer {
        return synthesizer ?: AVSpeechSynthesizer().also { synthesizer = it }
    }
    
    actual fun speak(text: String, flush: Boolean) {
        if (flush) {
            // Create new synthesizer to effectively stop previous
            synthesizer = AVSpeechSynthesizer()
        }
        
        val utterance = AVSpeechUtterance(string = text).apply {
            rate = currentRate
            pitchMultiplier = currentPitch
            voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        }
        
        getSynthesizer().speakUtterance(utterance)
    }
    
    actual fun stop() {
        // Create new synthesizer to stop speech
        synthesizer = AVSpeechSynthesizer()
    }
    
    actual fun setRate(rate: Float) {
        // AVSpeech rate: 0.0 (slowest) to 1.0 (fastest), default ~0.5
        // Convert from our 0.5-2.0 scale to AVSpeech scale
        currentRate = (rate * 0.25f).coerceIn(0.1f, 1.0f)
    }
    
    actual fun setPitch(pitch: Float) {
        // AVSpeech pitch: 0.5 to 2.0, default 1.0
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
    }
    
    actual val isSpeaking: Boolean
        get() = synthesizer?.isSpeaking() ?: false
    
    actual val isAvailable: Boolean
        get() = true // AVSpeechSynthesizer is always available on iOS
    
    actual fun shutdown() {
        synthesizer = null
    }
}

/**
 * iOS implementation of accessibility announcements using UIAccessibility
 */
actual class AccessibilityAnnouncer {
    
    actual fun announce(message: String) {
        UIAccessibilityPostNotification(
            UIAccessibilityAnnouncementNotification,
            message
        )
    }
}
