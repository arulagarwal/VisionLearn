package com.visionlearn

import androidx.compose.runtime.Composable
import com.visionlearn.presentation.navigation.AppNavigator
import com.visionlearn.presentation.theme.VisionLearnTheme
import org.koin.compose.KoinContext

/**
 * Main application composable
 * Entry point for the VisionLearn app
 */
@Composable
fun App() {
    KoinContext {
        VisionLearnTheme {
            AppNavigator()
        }
    }
}
