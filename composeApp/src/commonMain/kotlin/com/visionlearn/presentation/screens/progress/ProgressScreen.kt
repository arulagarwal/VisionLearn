package com.visionlearn.presentation.screens.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import com.visionlearn.presentation.theme.CVIColors

/**
 * Progress tracking screen showing learning history and stats
 */
class ProgressScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ProgressScreenModel>()
        val state by screenModel.state.collectAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Progress") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = screenModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    
                    // Child name header
                    if (state.childName.isNotEmpty()) {
                        item {
                            Text(
                                text = "${state.childName}'s Progress",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    
                    // Overall Stats
                    item {
                        Text(
                            text = "Overall Progress",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    item {
                        OverallStatsCard(
                            sessions = state.totalSessions,
                            activities = state.totalActivities,
                            accuracy = state.averageAccuracy
                        )
                    }
                    
                    // Achievement Badge
                    item {
                        AchievementCard(accuracy = state.averageAccuracy)
                    }
                    
                    // Recent Sessions
                    item {
                        Text(
                            text = "Recent Sessions",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    if (state.recentSessions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "📚",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No sessions yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Start learning to see your progress here!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.recentSessions) { session ->
                            SessionCard(session)
                        }
                    }
                    
                    // Error display
                    state.error?.let { error ->
                        item {
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
                    }
                    
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun OverallStatsCard(
    sessions: Int,
    activities: Int,
    accuracy: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                value = sessions.toString(),
                label = "Total Sessions",
                emoji = "📚"
            )
            StatColumn(
                value = activities.toString(),
                label = "Activities",
                emoji = "✨"
            )
            StatColumn(
                value = "$accuracy%",
                label = "Avg Accuracy",
                emoji = "🎯"
            )
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    emoji: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AchievementCard(accuracy: Int) {
    val (emoji, title, message, color) = when {
        accuracy >= 90 -> Achievement("🌟", "Star Learner!", "Amazing work!", CVIColors.Yellow)
        accuracy >= 75 -> Achievement("🎉", "Great Progress!", "Keep it up!", CVIColors.Green)
        accuracy >= 50 -> Achievement("👍", "Good Start!", "You're improving!", CVIColors.Blue)
        else -> Achievement("🌱", "Just Starting!", "Every step counts!", CVIColors.Purple)
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = color
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary) {
    val accuracyColor = when {
        session.accuracy >= 80 -> CVIColors.Success
        session.accuracy >= 60 -> CVIColors.Yellow
        else -> CVIColors.Orange
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.moduleName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = session.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${session.activitiesCompleted} activities",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = accuracyColor.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = "${session.accuracy}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = accuracyColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private data class Achievement(
    val emoji: String,
    val title: String,
    val message: String,
    val color: androidx.compose.ui.graphics.Color
)
