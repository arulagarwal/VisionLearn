package com.visionlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.visionlearn.di.AppConfig

/**
 * Main activity for VisionLearn Android app
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Gemini API key from BuildConfig
        AppConfig.geminiApiKey = BuildConfig.GEMINI_API_KEY
        
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                App()
            }
        }
    }
}
