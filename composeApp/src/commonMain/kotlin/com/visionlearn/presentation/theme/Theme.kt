package com.visionlearn.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.visionlearn.domain.model.CVIPhase
import com.visionlearn.domain.model.VisualProfile

private val LightColorScheme = lightColorScheme(
    primary = VisionPrimary,
    onPrimary = VisionOnPrimary,
    primaryContainer = VisionPrimaryContainer,
    onPrimaryContainer = VisionOnPrimaryContainer,
    secondary = VisionSecondary,
    onSecondary = VisionOnSecondary,
    secondaryContainer = VisionSecondaryContainer,
    onSecondaryContainer = VisionOnSecondaryContainer,
    background = VisionBackground,
    onBackground = VisionOnBackground,
    surface = VisionSurface,
    onSurface = VisionOnSurface,
    surfaceVariant = VisionSurfaceVariant,
    error = VisionError,
    onError = VisionOnError
)

private val DarkColorScheme = darkColorScheme(
    primary = VisionPrimaryDark,
    onPrimary = VisionOnPrimaryDark,
    background = VisionBackgroundDark,
    onBackground = VisionOnBackgroundDark,
    surface = VisionSurfaceDark,
    onSurface = VisionOnSurfaceDark,
    error = VisionError,
    onError = VisionOnError
)

/**
 * Accessibility configuration based on visual profile
 */
data class AccessibilityConfig(
    val fontSizes: FontSizeSet = AccessibleFontSizes.phaseII,
    val minTouchTargetSize: Int = 48, // dp
    val animationsEnabled: Boolean = true,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val audioFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true
) {
    companion object {
        fun fromProfile(profile: VisualProfile?): AccessibilityConfig {
            if (profile == null) return AccessibilityConfig()
            
            return AccessibilityConfig(
                fontSizes = when (profile.cviPhase) {
                    CVIPhase.PHASE_I -> AccessibleFontSizes.phaseI
                    CVIPhase.PHASE_II -> AccessibleFontSizes.phaseII
                    CVIPhase.PHASE_III -> AccessibleFontSizes.phaseIII
                },
                minTouchTargetSize = when (profile.cviPhase) {
                    CVIPhase.PHASE_I -> 72
                    CVIPhase.PHASE_II -> 56
                    CVIPhase.PHASE_III -> 48
                },
                animationsEnabled = profile.shouldEnableAnimations(),
                highContrast = profile.arrayComplexity <= 2,
                reducedMotion = profile.movementNeed <= 2,
                audioFeedbackEnabled = profile.audioFeedbackEnabled,
                hapticFeedbackEnabled = profile.hapticFeedbackEnabled
            )
        }
    }
}

/**
 * CompositionLocal for accessibility configuration
 */
val LocalAccessibilityConfig = staticCompositionLocalOf { AccessibilityConfig() }

/**
 * CompositionLocal for current visual profile
 */
val LocalVisualProfile = staticCompositionLocalOf<VisualProfile?> { null }

/**
 * Main theme composable for VisionLearn
 */
@Composable
fun VisionLearnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    profile: VisualProfile? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accessibilityConfig = AccessibilityConfig.fromProfile(profile)
    
    CompositionLocalProvider(
        LocalAccessibilityConfig provides accessibilityConfig,
        LocalVisualProfile provides profile
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VisionTypography,
            content = content
        )
    }
}

/**
 * High contrast theme for CVI Phase I users
 */
@Composable
fun HighContrastTheme(
    profile: VisualProfile,
    content: @Composable () -> Unit
) {
    val primaryColor = CVIColors.fromName(profile.getPrimaryColor())
    val backgroundColor = CVIColors.Black
    val contentColor = CVIColors.White
    
    val highContrastScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = CVIColors.getContrastColor(primaryColor),
        background = backgroundColor,
        onBackground = contentColor,
        surface = backgroundColor,
        onSurface = contentColor
    )
    
    val accessibilityConfig = AccessibilityConfig.fromProfile(profile)
    
    CompositionLocalProvider(
        LocalAccessibilityConfig provides accessibilityConfig,
        LocalVisualProfile provides profile
    ) {
        MaterialTheme(
            colorScheme = highContrastScheme,
            typography = VisionTypography,
            content = content
        )
    }
}
