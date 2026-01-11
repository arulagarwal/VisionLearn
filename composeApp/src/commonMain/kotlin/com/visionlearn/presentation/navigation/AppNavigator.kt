package com.visionlearn.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.SlideTransition
import com.visionlearn.domain.repository.ProfileRepository
import com.visionlearn.presentation.screens.home.HomeScreen
import com.visionlearn.presentation.screens.onboarding.OnboardingScreen
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * Main navigation graph for VisionLearn
 */
@Composable
fun AppNavigator() {
    Navigator(
        screen = SplashScreen(),
        onBackPressed = { currentScreen ->
            currentScreen !is HomeScreen
        }
    ) { navigator ->
        SlideTransition(navigator)
    }
}

/**
 * Splash screen - checks for existing profile and routes accordingly
 */
class SplashScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val profileRepository: ProfileRepository = koinInject()
        
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var hasProfile by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            try {
                // Check if there's an existing profile
                try {
                    val profiles = profileRepository.getAllProfiles()
                    hasProfile = profiles.isNotEmpty()
                    
                    // If we have profiles, ensure one is active
                    if (hasProfile) {
                        try {
                            val activeProfile = profileRepository.getActiveProfile()
                            if (activeProfile == null && profiles.isNotEmpty()) {
                                profileRepository.setActiveProfile(profiles.first().id)
                            }
                        } catch (_: Exception) {
                            // Non-fatal, continue
                        }
                    }
                } catch (_: Exception) {
                    hasProfile = false
                }
                
                isLoading = false
                delay(500)
                
                // Navigate based on profile status
                if (hasProfile) {
                    navigator.replaceAll(HomeScreen())
                } else {
                    navigator.replaceAll(OnboardingScreen())
                }
            } catch (e: Exception) {
                error = e.message
                isLoading = false
                delay(1500)
                try {
                    navigator.replaceAll(OnboardingScreen())
                } catch (_: Exception) { }
            }
        }
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "👁️",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "VisionLearn",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Accessible Learning for CVI",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                error?.let { err ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Error: $err",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
