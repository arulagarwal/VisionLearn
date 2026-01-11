package com.visionlearn.platform

import android.content.Context
import android.speech.tts.TextToSpeech as AndroidTTS
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.util.Locale

/**
 * Android implementation of Text-to-Speech
 */
actual class TextToSpeech(private val context: Context) {
    
    private var tts: AndroidTTS? = null
    private var isInitialized = false
    private var pendingRate = 1.0f
    private var pendingPitch = 1.0f
    
    init {
        tts = AndroidTTS(context) { status ->
            if (status == AndroidTTS.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(pendingRate)
                tts?.setPitch(pendingPitch)
            }
        }
    }
    
    actual fun speak(text: String, flush: Boolean) {
        if (!isInitialized) return
        
        val queueMode = if (flush) AndroidTTS.QUEUE_FLUSH else AndroidTTS.QUEUE_ADD
        tts?.speak(text, queueMode, null, text.hashCode().toString())
    }
    
    actual fun stop() {
        tts?.stop()
    }
    
    actual fun setRate(rate: Float) {
        pendingRate = rate.coerceIn(0.1f, 3.0f)
        if (isInitialized) {
            tts?.setSpeechRate(pendingRate)
        }
    }
    
    actual fun setPitch(pitch: Float) {
        pendingPitch = pitch.coerceIn(0.1f, 2.0f)
        if (isInitialized) {
            tts?.setPitch(pendingPitch)
        }
    }
    
    actual val isSpeaking: Boolean
        get() = tts?.isSpeaking ?: false
    
    actual val isAvailable: Boolean
        get() = isInitialized
    
    actual fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

/**
 * Android implementation of accessibility announcements
 */
actual class AccessibilityAnnouncer(private val context: Context) {
    
    private val accessibilityManager: AccessibilityManager? by lazy {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    
    actual fun announce(message: String) {
        if (accessibilityManager?.isEnabled != true) return
        
        val event = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
            className = context.javaClass.name
            packageName = context.packageName
            text.add(message)
        }
        
        accessibilityManager?.sendAccessibilityEvent(event)
    }
}
