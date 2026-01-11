package com.visionlearn.presentation.screens.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.visionlearn.ai.GeneratedQuestion
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.presentation.components.accessibility.ChoiceButton
import com.visionlearn.presentation.components.accessibility.LargeActionButton
import com.visionlearn.presentation.theme.CVIColors
import kotlinx.datetime.Clock

/**
 * Learning screen that hosts different learning modules
 * Now powered by AI-generated personalized content!
 */
data class LearningScreen(
    val moduleType: ModuleType
) : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<LearningScreenModel>()
        val state by screenModel.state.collectAsState()
        
        // Trigger session generation when screen loads
        LaunchedEffect(moduleType) {
            if (!state.isSessionActive && state.generatedSession == null && !state.isGeneratingSession) {
                screenModel.generateAndStartSession(moduleType)
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(moduleType.displayName)
                            state.generatedSession?.theme?.let { theme ->
                                Text(
                                    text = theme,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Exit"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    // Show loading while AI generates session
                    state.isGeneratingSession || state.isLoading -> {
                        AIGeneratingContent(moduleType = moduleType)
                    }
                    // Show error state
                    state.error != null -> {
                        ErrorContent(
                            error = state.error!!,
                            onRetry = { screenModel.generateAndStartSession(moduleType) }
                        )
                    }
                    // Show the appropriate module
                    else -> {
                        when (moduleType) {
                            ModuleType.RECOGNITION -> AIRecognitionActivity(screenModel, moduleType)
                            ModuleType.CAUSE_EFFECT -> AICauseEffectActivity(screenModel, moduleType)
                            ModuleType.SORTING -> AISortingActivity(screenModel, moduleType)
                            ModuleType.MATCHING -> AIMatchingActivity(screenModel, moduleType)
                            ModuleType.SEQUENCING -> AISequencingActivity(screenModel, moduleType)
                            else -> ComingSoonContent(moduleType)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loading screen while AI generates personalized content
 */
@Composable
private fun AIGeneratingContent(moduleType: ModuleType) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✨",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = CVIColors.Blue
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Creating Your Personalized\n${moduleType.displayName} Session...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Our AI is building activities just for you\nbased on your visual profile!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error state with retry option
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "😕",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CVIColors.Blue)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

/**
 * AI-Powered Recognition Activity
 */
@Composable
private fun AIRecognitionActivity(
    screenModel: LearningScreenModel,
    moduleType: ModuleType
) {
    val state by screenModel.state.collectAsState()
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var startTime by remember { mutableStateOf(Clock.System.now()) }
    
    // Generate AI session on first load
    LaunchedEffect(Unit) {
        if (!state.isSessionActive && state.generatedSession == null) {
            screenModel.generateAndStartSession(moduleType)
        }
    }
    
    val questions = screenModel.getGeneratedQuestions()
    val currentQuestion = state.currentQuestion
    
    // Session complete
    if (currentQuestion >= questions.size && questions.isNotEmpty()) {
        LaunchedEffect(Unit) {
            screenModel.completeSession()
        }
        
        AICompletionScreen(
            score = state.correctAnswers,
            total = questions.size,
            completionMessage = screenModel.getCompletionMessage(),
            onRestart = {
                screenModel.resetSession()
                screenModel.generateAndStartSession(moduleType)
            }
        )
        return
    }
    
    // Get current question
    val question = questions.getOrNull(currentQuestion)
    if (question == null) {
        // Fallback if no questions yet
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Question ${currentQuestion + 1} of ${questions.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentQuestion + 1).toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth()
            )
            
            // AI encouragement message
            state.currentEncouragement?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = CVIColors.Blue
                )
            }
        }
        
        // Question content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Display the item to identify
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CVIColors.Yellow.copy(alpha = 0.2f)
                ),
                modifier = Modifier.size(200.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = question.correctAnswer,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Answer options (correct + distractors shuffled)
            val allOptions = remember(question) {
                (listOf(question.correctAnswer) + question.distractors).shuffled()
            }
            
            allOptions.forEach { option ->
                ChoiceButton(
                    text = option,
                    onClick = {
                        if (isCorrect == null) {
                            val responseTime = Clock.System.now().toEpochMilliseconds() - startTime.toEpochMilliseconds()
                            selectedAnswer = option
                            isCorrect = option == question.correctAnswer
                            
                            screenModel.recordAnswer(
                                activityId = question.id,
                                isCorrect = isCorrect!!,
                                responseTimeMs = responseTime
                            )
                        }
                    },
                    isSelected = selectedAnswer == option,
                    isCorrect = if (selectedAnswer == option) isCorrect else null,
                    color = CVIColors.Blue,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
        
        // Feedback and Next button
        if (isCorrect != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isCorrect == true) question.successFeedback else question.retryFeedback,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isCorrect == true) CVIColors.Success else CVIColors.Red,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                LargeActionButton(
                    text = if (currentQuestion < questions.size - 1) "Next" else "See Results",
                    onClick = {
                        // Advance to next question
                        screenModel.advanceToNextQuestion()
                        // Reset local state for next question
                        selectedAnswer = null
                        isCorrect = null
                        startTime = Clock.System.now()
                    },
                    color = CVIColors.Green
                )
            }
        }
    }
}

/**
 * AI-Powered Cause & Effect Activity
 */
@Composable
private fun AICauseEffectActivity(
    screenModel: LearningScreenModel,
    moduleType: ModuleType
) {
    val state by screenModel.state.collectAsState()
    var tappedResponse by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        if (!state.isSessionActive && state.generatedSession == null) {
            screenModel.generateAndStartSession(moduleType)
        }
    }
    
    val questions = screenModel.getGeneratedQuestions()
    
    if (currentIndex >= questions.size && questions.isNotEmpty()) {
        LaunchedEffect(Unit) { screenModel.completeSession() }
        AICompletionScreen(
            score = state.correctAnswers,
            total = questions.size,
            completionMessage = screenModel.getCompletionMessage(),
            onRestart = {
                screenModel.resetSession()
                screenModel.generateAndStartSession(moduleType)
            }
        )
        return
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tap to see what happens!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "${currentIndex + 1} of ${questions.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Response display
        AnimatedVisibility(
            visible = tappedResponse != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CVIColors.Yellow.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tappedResponse ?: "",
                        style = MaterialTheme.typography.headlineLarge,
                        color = CVIColors.Blue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        if (tappedResponse == null) {
            Spacer(modifier = Modifier.height(100.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Current item to tap
        val currentQ = questions.getOrNull(currentIndex)
        if (currentQ != null) {
            Text(
                text = currentQ.prompt,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    tappedResponse = currentQ.successFeedback
                    screenModel.recordAnswer(
                        activityId = currentQ.id,
                        isCorrect = true
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CVIColors.Blue),
                modifier = Modifier.size(160.dp)
            ) {
                Text(
                    text = currentQ.correctAnswer,
                    style = MaterialTheme.typography.displayLarge
                )
            }
            
            if (tappedResponse != null) {
                Spacer(modifier = Modifier.height(24.dp))
                LargeActionButton(
                    text = if (currentIndex < questions.size - 1) "Next" else "Finish",
                    onClick = {
                        screenModel.advanceToNextQuestion()
                        currentIndex++
                        tappedResponse = null
                    },
                    color = CVIColors.Green
                )
            }
        }
    }
}

/**
 * AI-Powered Sorting Activity
 */
@Composable
private fun AISortingActivity(
    screenModel: LearningScreenModel,
    moduleType: ModuleType
) {
    val state by screenModel.state.collectAsState()
    var currentRound by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!state.isSessionActive && state.generatedSession == null) {
            screenModel.generateAndStartSession(moduleType)
        }
    }
    
    val questions = screenModel.getGeneratedQuestions()
    
    if (currentRound >= questions.size && questions.isNotEmpty()) {
        LaunchedEffect(Unit) { screenModel.completeSession() }
        AICompletionScreen(
            score = state.correctAnswers,
            total = questions.size,
            completionMessage = screenModel.getCompletionMessage(),
            onRestart = {
                screenModel.resetSession()
                screenModel.generateAndStartSession(moduleType)
            }
        )
        return
    }
    
    val currentQ = questions.getOrNull(currentRound)
    if (currentQ == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Parse categories from correctAnswer format: "Category1:items|Category2:items"
    val categories = remember(currentQ) {
        currentQ.correctAnswer.split("|").map { part ->
            val (name, _) = part.split(":")
            name
        }
    }
    
    val items = currentQ.distractors.shuffled()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { (currentRound + 1).toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = currentQ.prompt,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Items to sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.take(4).forEach { item ->
                Card(
                    modifier = Modifier.size(70.dp),
                    colors = CardDefaults.cardColors(containerColor = CVIColors.Yellow.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = item, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Category buttons
        Text("Sort into:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.forEach { category ->
                LargeActionButton(
                    text = category,
                    onClick = {
                        if (!showResult) {
                            selectedCategory = category
                            showResult = true
                            screenModel.recordAnswer(
                                activityId = currentQ.id,
                                isCorrect = true
                            )
                        }
                    },
                    color = if (category == categories.first()) CVIColors.Blue else CVIColors.Green
                )
            }
        }
        
        if (showResult) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = currentQ.successFeedback,
                style = MaterialTheme.typography.titleMedium,
                color = CVIColors.Success
            )
            Spacer(modifier = Modifier.height(16.dp))
            LargeActionButton(
                text = if (currentRound < questions.size - 1) "Next" else "Finish",
                onClick = {
                    screenModel.advanceToNextQuestion()
                    currentRound++
                    selectedCategory = null
                    showResult = false
                },
                color = CVIColors.Green
            )
        }
    }
}

/**
 * AI-Powered Matching Activity
 */
@Composable
private fun AIMatchingActivity(
    screenModel: LearningScreenModel,
    moduleType: ModuleType
) {
    val state by screenModel.state.collectAsState()
    var currentRound by remember { mutableStateOf(0) }
    var selectedCards by remember { mutableStateOf<List<Int>>(emptyList()) }
    var matchedPairs by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    LaunchedEffect(Unit) {
        if (!state.isSessionActive && state.generatedSession == null) {
            screenModel.generateAndStartSession(moduleType)
        }
    }
    
    val questions = screenModel.getGeneratedQuestions()
    
    if (currentRound >= questions.size && questions.isNotEmpty()) {
        LaunchedEffect(Unit) { screenModel.completeSession() }
        AICompletionScreen(
            score = state.correctAnswers,
            total = questions.size,
            completionMessage = screenModel.getCompletionMessage(),
            onRestart = {
                screenModel.resetSession()
                screenModel.generateAndStartSession(moduleType)
            }
        )
        return
    }
    
    val currentQ = questions.getOrNull(currentRound)
    if (currentQ == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Cards from distractors (shuffled pairs)
    val cards = remember(currentQ) { currentQ.distractors }
    val totalPairs = cards.size / 2
    
    // Check for match when 2 cards selected
    LaunchedEffect(selectedCards) {
        if (selectedCards.size == 2) {
            val card1 = cards[selectedCards[0]]
            val card2 = cards[selectedCards[1]]
            
            if (card1 == card2) {
                matchedPairs = matchedPairs + selectedCards.toSet()
                screenModel.recordAnswer(currentQ.id, isCorrect = true)
            }
            
            kotlinx.coroutines.delay(500)
            selectedCards = emptyList()
            
            // All pairs matched
            if (matchedPairs.size == cards.size) {
                screenModel.advanceToNextQuestion()
                currentRound++
                matchedPairs = emptySet()
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { (currentRound + 1).toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = currentQ.prompt,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Pairs found: ${matchedPairs.size / 2} / $totalPairs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Card grid
        cards.chunked(4).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEachIndexed { colIndex, card ->
                    val index = rowIndex * 4 + colIndex
                    val isMatched = index in matchedPairs
                    val isSelected = index in selectedCards
                    
                    Card(
                        onClick = {
                            if (!isMatched && selectedCards.size < 2 && index !in selectedCards) {
                                selectedCards = selectedCards + index
                            }
                        },
                        modifier = Modifier.size(80.dp).padding(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isMatched -> CVIColors.Success.copy(alpha = 0.3f)
                                isSelected -> CVIColors.Blue.copy(alpha = 0.3f)
                                else -> CVIColors.Yellow.copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isMatched || isSelected) card else "?",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * AI-Powered Sequencing Activity
 */
@Composable
private fun AISequencingActivity(
    screenModel: LearningScreenModel,
    moduleType: ModuleType
) {
    val state by screenModel.state.collectAsState()
    var currentRound by remember { mutableStateOf(0) }
    var userSequence by remember { mutableStateOf<List<String>>(emptyList()) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!state.isSessionActive && state.generatedSession == null) {
            screenModel.generateAndStartSession(moduleType)
        }
    }
    
    val questions = screenModel.getGeneratedQuestions()
    
    if (currentRound >= questions.size && questions.isNotEmpty()) {
        LaunchedEffect(Unit) { screenModel.completeSession() }
        AICompletionScreen(
            score = state.correctAnswers,
            total = questions.size,
            completionMessage = screenModel.getCompletionMessage(),
            onRestart = {
                screenModel.resetSession()
                screenModel.generateAndStartSession(moduleType)
            }
        )
        return
    }
    
    val currentQ = questions.getOrNull(currentRound)
    if (currentQ == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    val correctOrder = currentQ.correctAnswer.split(" ").filter { it.isNotBlank() }
    val shuffledItems = remember(currentQ) { currentQ.distractors }
    val remainingItems = shuffledItems.filter { it !in userSequence }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { (currentRound + 1).toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = currentQ.prompt,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = currentQ.hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // User's current sequence
        Text("Your order:", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (userSequence.isEmpty()) {
                Text("Tap items below to add them", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                userSequence.forEachIndexed { idx, item ->
                    Card(
                        modifier = Modifier.size(60.dp).padding(4.dp),
                        colors = CardDefaults.cardColors(containerColor = CVIColors.Blue.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = item, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Items to choose from
        if (!showResult) {
            Text("Tap to add:", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                remainingItems.forEach { item ->
                    Card(
                        onClick = {
                            userSequence = userSequence + item
                            if (userSequence.size == correctOrder.size) {
                                isCorrect = userSequence == correctOrder
                                showResult = true
                                screenModel.recordAnswer(currentQ.id, isCorrect = isCorrect)
                            }
                        },
                        modifier = Modifier.size(70.dp).padding(4.dp),
                        colors = CardDefaults.cardColors(containerColor = CVIColors.Yellow.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = item, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
            
            if (userSequence.isNotEmpty()) {
                TextButton(onClick = { userSequence = emptyList() }) {
                    Text("Clear", color = CVIColors.Red)
                }
            }
        }
        
        // Result
        if (showResult) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isCorrect) currentQ.successFeedback else currentQ.retryFeedback,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCorrect) CVIColors.Success else CVIColors.Red,
                textAlign = TextAlign.Center
            )
            
            if (!isCorrect) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Correct order: ${correctOrder.joinToString(" → ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            LargeActionButton(
                text = if (currentRound < questions.size - 1) "Next" else "Finish",
                onClick = {
                    screenModel.advanceToNextQuestion()
                    currentRound++
                    userSequence = emptyList()
                    showResult = false
                },
                color = CVIColors.Green
            )
        }
    }
}

/**
 * AI-Enhanced Completion Screen
 */
@Composable
private fun AICompletionScreen(
    score: Int,
    total: Int,
    completionMessage: String,
    onRestart: () -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    val percentage = if (total > 0) (score * 100) / total else 0
    
    val emoji = when {
        percentage >= 90 -> "🏆"
        percentage >= 70 -> "🌟"
        percentage >= 50 -> "👍"
        else -> "💪"
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = CVIColors.Success.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score / $total",
                    style = MaterialTheme.typography.displayMedium,
                    color = CVIColors.Success
                )
                Text(
                    text = "correct answers",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$percentage% accuracy",
                    style = MaterialTheme.typography.titleMedium,
                    color = CVIColors.Blue
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = completionMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LargeActionButton(
            text = "🔄 Play Again",
            onClick = onRestart,
            color = CVIColors.Blue
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LargeActionButton(
            text = "🏠 Back Home",
            onClick = { navigator.pop() },
            color = CVIColors.Green
        )
    }
}

@Composable
private fun ComingSoonContent(moduleType: ModuleType) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🚧", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${moduleType.displayName}\nComing Soon!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
    }
}

// Data class for fallback recognition questions
private data class RecognitionQuestion(
    val emoji: String,
    val correctAnswer: String,
    val options: List<String>
)

// Data class for sorting items
private data class SortItem(
    val emoji: String,
    val name: String,
    val category: String
)
