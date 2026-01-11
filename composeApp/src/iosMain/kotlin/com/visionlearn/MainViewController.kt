package com.visionlearn

import androidx.compose.ui.window.ComposeUIViewController
import com.visionlearn.di.initKoin

/**
 * Helper object for iOS initialization - accessible from Swift
 */
object KoinHelper {
    fun doInitKoin() {
        try {
            initKoin()
        } catch (_: Exception) {
            // Koin may already be initialized
        }
    }
}

/**
 * Creates the main view controller for iOS
 */
fun MainViewController() = ComposeUIViewController { 
    App() 
}
