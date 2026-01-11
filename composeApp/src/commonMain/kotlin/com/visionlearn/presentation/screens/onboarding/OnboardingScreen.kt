package com.visionlearn.presentation.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.visionlearn.di.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.visionlearn.domain.model.CVIPhase
import com.visionlearn.presentation.components.accessibility.AccessibleButton
import com.visionlearn.presentation.screens.home.HomeScreen
import com.visionlearn.presentation.theme.CVIColors

/**
 * Onboarding screen for new users
 * Guides through CVI profile setup based on Perkins CVI Protocol
 */
class OnboardingScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<OnboardingScreenModel>()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        
        // Navigate to home when complete
        LaunchedEffect(state.isComplete) {
            if (state.isComplete) {
                navigator.replaceAll(HomeScreen())
            }
        }
        
        // Show error snackbar
        LaunchedEffect(state.error) {
            state.error?.let { error ->
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
                screenModel.clearError()
            }
        }
        
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(24.dp)
            ) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Error card if there's an error
                state.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Step content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (state.currentStep) {
                        0 -> WelcomeStep()
                        1 -> NameInputStep(
                            name = state.childName,
                            onNameChange = screenModel::updateChildName
                        )
                        2 -> PhaseSelectionStep(
                            selectedPhase = state.selectedPhase,
                            onPhaseSelected = screenModel::selectPhase
                        )
                        3 -> ColorPreferenceStep(
                            selectedColors = state.selectedColors,
                            onColorToggle = screenModel::toggleColor
                        )
                    }
                }
                
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (state.currentStep > 0) {
                        AccessibleButton(
                            text = "Back",
                            onClick = screenModel::previousStep
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    if (state.currentStep < state.totalSteps - 1) {
                        AccessibleButton(
                            text = "Next",
                            onClick = screenModel::nextStep,
                            enabled = state.canProceed,
                            backgroundColor = CVIColors.Blue
                        )
                    } else {
                        AccessibleButton(
                            text = if (state.isLoading) "Saving..." else "Get Started",
                            onClick = screenModel::completeOnboarding,
                            enabled = state.canProceed && !state.isLoading,
                            backgroundColor = CVIColors.Green
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "👋",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to VisionLearn",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "An accessible learning app designed for children with Cortical Visual Impairment (CVI).",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = CVIColors.Blue.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎯 Based on Perkins CVI Protocol",
                    style = MaterialTheme.typography.titleMedium,
                    color = CVIColors.Blue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Let's create a personalized visual profile based on the 16 Visual Behaviors framework.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NameInputStep(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "👶",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "What's the child's name?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            textStyle = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This helps personalize the learning experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhaseSelectionStep(
    selectedPhase: CVIPhase?,
    onPhaseSelected: (CVIPhase) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📊",
            style = MaterialTheme.typography.displayMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Select CVI Phase",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Based on the CVI Range assessment (0-10)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CVIPhase.entries.forEach { phase ->
            PhaseCard(
                phase = phase,
                isSelected = selectedPhase == phase,
                onClick = { onPhaseSelected(phase) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Don't know the phase? Select Phase II for a balanced starting point.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun PhaseCard(
    phase: CVIPhase,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (phase) {
        CVIPhase.PHASE_I -> CVIColors.Red
        CVIPhase.PHASE_II -> CVIColors.Yellow
        CVIPhase.PHASE_III -> CVIColors.Green
    }
    
    val emoji = when (phase) {
        CVIPhase.PHASE_I -> "🔴"
        CVIPhase.PHASE_II -> "🟡"
        CVIPhase.PHASE_III -> "🟢"
    }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.2f) 
                else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phase.name.replace("_", " "),
                    style = MaterialTheme.typography.titleLarge,
                    color = color
                )
                Text(
                    text = "Range: ${phase.range.first}-${phase.range.last}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = phase.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineMedium,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun ColorPreferenceStep(
    selectedColors: List<String>,
    onColorToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎨",
            style = MaterialTheme.typography.displayMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Preferred Colors",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Select colors the child responds to best.\nChildren with CVI often prefer red and yellow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Color selection grid
        CVIColors.availableColors.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (name, color) ->
                    ColorButton(
                        name = name,
                        color = color,
                        isSelected = name in selectedColors,
                        onClick = { onColorToggle(name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Selected: ${selectedColors.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ColorButton(
    name: String,
    color: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(4.dp, CVIColors.Black)
        } else null,
        modifier = Modifier.size(80.dp)
    ) {
        if (isSelected) {
            Text(
                text = "✓",
                color = CVIColors.getContrastColor(color),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
