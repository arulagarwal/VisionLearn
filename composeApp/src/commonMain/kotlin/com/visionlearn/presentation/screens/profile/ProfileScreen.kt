package com.visionlearn.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.visionlearn.di.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.visionlearn.domain.model.CVIPhase
import com.visionlearn.domain.model.InputMethod
import com.visionlearn.presentation.theme.CVIColors

/**
 * Profile settings screen for managing visual profile
 */
class ProfileScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ProfileScreenModel>()
        val state by screenModel.state.collectAsState()
        
        // Show success snackbar
        LaunchedEffect(state.saveSuccess) {
            if (state.saveSuccess) {
                // Could show snackbar here
                screenModel.clearSaveSuccess()
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Visual Profile") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Child Info Section
                    SectionHeader("Child Information")
                    
                    OutlinedTextField(
                        value = state.childName,
                        onValueChange = screenModel::updateChildName,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // CVI Phase Section
                    SectionHeader("CVI Phase")
                    
                    CVIPhase.entries.forEach { phase ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.cviPhase == phase,
                                onClick = { screenModel.updateCVIPhase(phase) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = phase.name.replace("_", " "),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = phase.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Preferred Colors Section
                    SectionHeader("Preferred Colors")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CVIColors.availableColors.forEach { (name, color) ->
                            FilterChip(
                                selected = name in state.preferredColors,
                                onClick = { screenModel.toggleColor(name) },
                                label = { Text(name.replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Session Preferences
                    SectionHeader("Session Preferences")
                    
                    Text(
                        text = "Session Duration: ${state.sessionDuration} minutes",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Slider(
                        value = state.sessionDuration.toFloat(),
                        onValueChange = { screenModel.updateSessionDuration(it.toInt()) },
                        valueRange = 5f..30f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Input Method
                    Text(
                        text = "Input Method",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InputMethod.entries.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.inputMethod == method,
                                onClick = { screenModel.updateInputMethod(method) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = method.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Feedback Options
                    SectionHeader("Feedback Options")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audio Feedback",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = state.audioEnabled,
                            onCheckedChange = screenModel::updateAudioEnabled
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = state.hapticEnabled,
                            onCheckedChange = screenModel::updateHapticEnabled
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Speech Rate: ${((state.ttsRate * 10).toInt() / 10.0)}x",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Slider(
                        value = state.ttsRate,
                        onValueChange = screenModel::updateTTSRate,
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Save Button
                    Button(
                        onClick = screenModel::saveProfile,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CVIColors.Green
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CVIColors.White
                            )
                        } else {
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    // Error display
                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CVIColors.Red.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = error,
                                color = CVIColors.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
}
