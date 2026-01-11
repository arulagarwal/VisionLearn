package com.visionlearn.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.visionlearn.di.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.visionlearn.ai.GeneratedQuestion
import com.visionlearn.ai.GeneratedSession
import com.visionlearn.ai.VisualConfig
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.domain.repository.CustomActivity
import com.visionlearn.presentation.components.accessibility.ModuleCard
import com.visionlearn.presentation.screens.creator.CreatorScreen
import com.visionlearn.presentation.screens.learning.LearningScreen
import com.visionlearn.presentation.screens.learning.CustomLearningScreen
import com.visionlearn.presentation.screens.profile.ProfileScreen
import com.visionlearn.presentation.screens.progress.ProgressScreen
import com.visionlearn.presentation.theme.CVIColors

/**
 * Home screen showing available learning modules and custom activities
 */
class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<HomeScreenModel>()
        val state by screenModel.state.collectAsState()
        
        LaunchedEffect(Unit) {
            screenModel.initialize()
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Hi, ${state.childName}! 👋",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "What shall we learn today?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(ProgressScreen()) }) {
                            Icon(Icons.Default.Star, contentDescription = "Progress")
                        }
                        IconButton(onClick = { navigator.push(ProfileScreen()) }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navigator.push(CreatorScreen()) },
                    containerColor = CVIColors.Green,
                    contentColor = CVIColors.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create") }
                )
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { screenModel.refresh() },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    
                    // Quick Stats Card
                    item {
                        QuickStatsCard(
                            sessions = state.totalSessions,
                            activities = state.totalActivities,
                            accuracy = state.averageAccuracy
                        )
                    }
                    
                    // Error display
                    if (state.error != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "Error: ${state.error}",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    // Custom Activities Section (if any exist)
                    if (state.hasCustomActivities) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Activities (${state.customActivities.size})",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                TextButton(onClick = { navigator.push(CreatorScreen()) }) {
                                    Text("See All")
                                }
                            }
                        }
                        
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.customActivities.take(5)) { activity ->
                                    CustomActivityCard(
                                        activity = activity,
                                        onClick = {
                                            // Convert to GeneratedSession and start
                                            val session = activity.toGeneratedSession()
                                            navigator.push(CustomLearningScreen(session))
                                        }
                                    )
                                }
                                
                                // "Create More" card
                                item {
                                    CreateMoreCard(
                                        onClick = { navigator.push(CreatorScreen()) }
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Text(
                            text = "Learning Modules",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // Built-in Learning Modules
                    items(learningModules) { module ->
                        ModuleCard(
                            title = module.title,
                            description = module.description,
                            accentColor = module.color,
                            iconContent = {
                                Text(
                                    text = module.emoji,
                                    style = MaterialTheme.typography.displaySmall
                                )
                            },
                            onClick = {
                                navigator.push(LearningScreen(module.type))
                            }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun QuickStatsCard(
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
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(value = sessions.toString(), label = "Sessions", emoji = "📚")
            StatItem(value = activities.toString(), label = "Activities", emoji = "✨")
            StatItem(value = "$accuracy%", label = "Accuracy", emoji = "🎯")
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CustomActivityCard(
    activity: CustomActivity,
    onClick: () -> Unit
) {
    val color = getColorForModule(activity.moduleType)
    
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show first emoji from images
                val firstEmoji = activity.images.firstOrNull()?.emoji ?: "📚"
                Text(
                    text = firstEmoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                
                // Template badge
                if (activity.isFromTemplate) {
                    Surface(
                        color = CVIColors.Purple.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "T",
                            style = MaterialTheme.typography.labelSmall,
                            color = CVIColors.Purple,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Column {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${activity.moduleType.displayName} • ${activity.images.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Play button
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CreateMoreCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp).height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create More",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create\nMore",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Extension to convert CustomActivity to GeneratedSession
private fun CustomActivity.toGeneratedSession(): GeneratedSession {
    val questions = images.mapIndexed { index, img ->
        // Create recognition-style questions
        val otherImages = images.filter { it.id != img.id }.shuffled().take(2)
        
        GeneratedQuestion(
            id = "custom_${id}_$index",
            type = moduleType.name.lowercase(),
            prompt = "Find the ${img.name}",
            promptAudio = "Can you find the ${img.name}?",
            correctAnswer = img.emoji,
            distractors = otherImages.map { it.emoji },
            hint = img.aiDescription ?: "Look carefully!",
            successFeedback = "Great job! You found the ${img.name}! 🎉",
            retryFeedback = "Try again! Look for the ${img.name}",
            visualConfig = VisualConfig(
                accentColor = "yellow",
                itemSize = "large"
            )
        )
    }
    
    return GeneratedSession(
        id = "session_$id",
        moduleType = moduleType.name,
        theme = title,
        difficultyLevel = 2,
        questions = questions,
        estimatedDurationMinutes = (questions.size * 2).coerceIn(5, 15),
        encouragementMessages = listOf(
            "Great job! 🌟",
            "You're doing amazing! ✨",
            "Keep going! 💪",
            "Wonderful! 🎉"
        ),
        completionMessage = "Amazing work completing '$title'! 🏆"
    )
}

private fun getColorForModule(moduleType: ModuleType): Color {
    return when (moduleType) {
        ModuleType.RECOGNITION -> CVIColors.Yellow
        ModuleType.CAUSE_EFFECT -> CVIColors.Blue
        ModuleType.SORTING -> CVIColors.Green
        ModuleType.MATCHING -> CVIColors.Purple
        ModuleType.SEQUENCING -> CVIColors.Orange
        else -> CVIColors.Blue
    }
}

private data class ModuleInfo(
    val type: ModuleType,
    val title: String,
    val description: String,
    val emoji: String,
    val color: Color
)

private val learningModules = listOf(
    ModuleInfo(
        type = ModuleType.RECOGNITION,
        title = "Image Recognition",
        description = "Identify objects with audio feedback",
        emoji = "🖼️",
        color = CVIColors.Yellow
    ),
    ModuleInfo(
        type = ModuleType.CAUSE_EFFECT,
        title = "Cause & Effect",
        description = "Touch to see and hear responses",
        emoji = "✨",
        color = CVIColors.Blue
    ),
    ModuleInfo(
        type = ModuleType.SORTING,
        title = "Sorting",
        description = "Group objects by category",
        emoji = "📦",
        color = CVIColors.Green
    ),
    ModuleInfo(
        type = ModuleType.MATCHING,
        title = "Matching",
        description = "Find matching pairs",
        emoji = "🎴",
        color = CVIColors.Purple
    ),
    ModuleInfo(
        type = ModuleType.SEQUENCING,
        title = "Sequencing",
        description = "Arrange items in order",
        emoji = "📋",
        color = CVIColors.Orange
    )
)
