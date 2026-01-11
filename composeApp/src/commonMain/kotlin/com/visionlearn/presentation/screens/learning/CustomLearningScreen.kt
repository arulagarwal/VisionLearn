package com.visionlearn.presentation.screens.learning

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.visionlearn.ai.GeneratedQuestion
import com.visionlearn.ai.GeneratedSession
import com.visionlearn.presentation.theme.CVIColors
import kotlinx.coroutines.delay

/**
 * Learning screen for custom activities created by users
 * Simpler than the AI-powered LearningScreen - works directly with GeneratedSession
 */
class CustomLearningScreen(
    private val session: GeneratedSession
) : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        var currentQuestionIndex by remember { mutableStateOf(0) }
        var correctAnswers by remember { mutableStateOf(0) }
        var isCompleted by remember { mutableStateOf(false) }
        var showFeedback by remember { mutableStateOf(false) }
        var lastAnswerCorrect by remember { mutableStateOf(false) }
        var encouragement by remember { mutableStateOf(session.encouragementMessages.firstOrNull() ?: "") }
        
        val questions = session.questions
        val currentQuestion = questions.getOrNull(currentQuestionIndex)
        val progress = if (questions.isNotEmpty()) {
            (currentQuestionIndex.toFloat() / questions.size.toFloat())
        } else 0f
        
        // Handle answer
        fun onAnswer(answer: String) {
            val isCorrect = answer == currentQuestion?.correctAnswer
            lastAnswerCorrect = isCorrect
            if (isCorrect) {
                correctAnswers++
                encouragement = session.encouragementMessages.random()
            }
            showFeedback = true
        }
        
        // Move to next question after feedback
        LaunchedEffect(showFeedback) {
            if (showFeedback) {
                delay(1500)
                showFeedback = false
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                } else {
                    isCompleted = true
                }
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(session.theme) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        Text(
                            text = "${currentQuestionIndex + 1}/${questions.size}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black)
            ) {
                when {
                    isCompleted -> {
                        CompletionContent(
                            correctAnswers = correctAnswers,
                            totalQuestions = questions.size,
                            message = session.completionMessage,
                            onFinish = { navigator.pop() },
                            onRestart = {
                                currentQuestionIndex = 0
                                correctAnswers = 0
                                isCompleted = false
                            }
                        )
                    }
                    currentQuestion != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = CVIColors.Green
                            )
                            
                            // Encouragement
                            if (encouragement.isNotEmpty()) {
                                Text(
                                    text = encouragement,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CVIColors.Yellow,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                            
                            // Question content
                            QuestionContent(
                                question = currentQuestion,
                                showFeedback = showFeedback,
                                isCorrect = lastAnswerCorrect,
                                onAnswer = { onAnswer(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    question: GeneratedQuestion,
    showFeedback: Boolean,
    isCorrect: Boolean,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Prompt
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            
            // Answer choices
            val allChoices = remember(question) {
                (listOf(question.correctAnswer) + question.distractors).shuffled()
            }
            
            if (allChoices.size == 1) {
                // Single item (cause & effect style)
                Card(
                    onClick = { onAnswer(question.correctAnswer) },
                    modifier = Modifier.size(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CVIColors.Yellow.copy(alpha = 0.2f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = question.correctAnswer,
                            fontSize = 80.sp
                        )
                    }
                }
            } else {
                // Multiple choices
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    allChoices.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { choice ->
                                AnswerCard(
                                    answer = choice,
                                    onClick = { onAnswer(choice) },
                                    enabled = !showFeedback
                                )
                            }
                        }
                    }
                }
            }
            
            // Hint
            Text(
                text = "💡 ${question.hint}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
        
        // Feedback overlay
        AnimatedVisibility(
            visible = showFeedback,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isCorrect) CVIColors.Success.copy(alpha = 0.9f)
                        else CVIColors.Red.copy(alpha = 0.9f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isCorrect) "✅" else "❌",
                        fontSize = 80.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isCorrect) question.successFeedback else question.retryFeedback,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerCard(
    answer: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = CVIColors.Yellow.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = answer,
                fontSize = 60.sp
            )
        }
    }
}

@Composable
private fun CompletionContent(
    correctAnswers: Int,
    totalQuestions: Int,
    message: String,
    onFinish: () -> Unit,
    onRestart: () -> Unit
) {
    val percentage = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
    val emoji = when {
        percentage >= 80 -> "🏆"
        percentage >= 60 -> "⭐"
        percentage >= 40 -> "👍"
        else -> "💪"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 100.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = CVIColors.Yellow,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Score display
        Card(
            colors = CardDefaults.cardColors(
                containerColor = CVIColors.Success.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$correctAnswers / $totalQuestions",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Correct Answers",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = CVIColors.Green
                )
                Text(
                    text = "$percentage% Accuracy",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CVIColors.Green,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Again")
            }
            
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CVIColors.Green)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done")
            }
        }
    }
}
