package com.visionlearn.presentation.theme

import androidx.compose.ui.graphics.Color

// ===== Primary Brand Colors =====
val VisionPrimary = Color(0xFF6750A4)
val VisionOnPrimary = Color(0xFFFFFFFF)
val VisionPrimaryContainer = Color(0xFFE9DDFF)
val VisionOnPrimaryContainer = Color(0xFF22005D)

// ===== Secondary Colors =====
val VisionSecondary = Color(0xFF625B71)
val VisionOnSecondary = Color(0xFFFFFFFF)
val VisionSecondaryContainer = Color(0xFFE8DEF8)
val VisionOnSecondaryContainer = Color(0xFF1E192B)

// ===== Background & Surface =====
val VisionBackground = Color(0xFFFFFBFF)
val VisionOnBackground = Color(0xFF1C1B1E)
val VisionSurface = Color(0xFFFFFBFF)
val VisionOnSurface = Color(0xFF1C1B1E)
val VisionSurfaceVariant = Color(0xFFE7E0EB)

// ===== Error Colors =====
val VisionError = Color(0xFFBA1A1A)
val VisionOnError = Color(0xFFFFFFFF)

// ===== CVI-Friendly High Contrast Colors =====
object CVIColors {
    // Primary colors often preferred by children with CVI
    val Yellow = Color(0xFFFFEB3B)
    val Red = Color(0xFFF44336)
    val Blue = Color(0xFF2196F3)
    val Green = Color(0xFF4CAF50)
    val Orange = Color(0xFFFF9800)
    val Purple = Color(0xFF9C27B0)
    val Pink = Color(0xFFE91E63)
    
    // High contrast backgrounds
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val DarkGray = Color(0xFF212121)
    
    // Success/Feedback colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFC107)
    val Info = Color(0xFF2196F3)
    
    /**
     * Get Color from string name
     */
    fun fromName(name: String): Color = when (name.lowercase()) {
        "yellow" -> Yellow
        "red" -> Red
        "blue" -> Blue
        "green" -> Green
        "orange" -> Orange
        "purple" -> Purple
        "pink" -> Pink
        "black" -> Black
        "white" -> White
        else -> Yellow // Default to yellow (commonly preferred)
    }
    
    /**
     * Get contrasting text color for a background
     */
    fun getContrastColor(background: Color): Color {
        // Simple luminance calculation
        val luminance = (0.299 * background.red + 0.587 * background.green + 0.114 * background.blue)
        return if (luminance > 0.5) Black else White
    }
    
    /**
     * Available color options for profile selection
     */
    val availableColors = listOf(
        "yellow" to Yellow,
        "red" to Red,
        "blue" to Blue,
        "green" to Green,
        "orange" to Orange,
        "purple" to Purple,
        "pink" to Pink
    )
}

// ===== Dark Theme Colors =====
val VisionPrimaryDark = Color(0xFFCFBCFF)
val VisionOnPrimaryDark = Color(0xFF381E72)
val VisionBackgroundDark = Color(0xFF1C1B1E)
val VisionOnBackgroundDark = Color(0xFFE6E1E6)
val VisionSurfaceDark = Color(0xFF1C1B1E)
val VisionOnSurfaceDark = Color(0xFFE6E1E6)
