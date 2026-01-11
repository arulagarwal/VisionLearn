package com.visionlearn.ai

import com.visionlearn.domain.model.*
import kotlinx.datetime.Clock

/**
 * Local session generator - rule-based fallback when Gemini API is unavailable
 * Generates predefined learning sessions based on module type and profile
 */
class LocalSessionGenerator : AISessionGenerator {
    
    override suspend fun generateSession(
        profile: VisualProfile,
        moduleType: ModuleType,
        previousPerformance: SessionPerformance?
    ): Result<GeneratedSession> = runCatching {
        
        val difficulty = calculateDifficulty(profile, previousPerformance)
        val questionCount = calculateQuestionCount(profile, previousPerformance)
        
        val questions = when (moduleType) {
            ModuleType.RECOGNITION -> generateRecognitionQuestions(profile, difficulty, questionCount)
            ModuleType.CAUSE_EFFECT -> generateCauseEffectQuestions(profile, difficulty, questionCount)
            ModuleType.SORTING -> generateSortingQuestions(profile, difficulty, questionCount)
            ModuleType.MATCHING -> generateMatchingQuestions(profile, difficulty, questionCount)
            ModuleType.SEQUENCING -> generateSequencingQuestions(profile, difficulty, questionCount)
            else -> generateRecognitionQuestions(profile, difficulty, questionCount)
        }
        
        GeneratedSession(
            id = "local_${Clock.System.now().toEpochMilliseconds()}",
            moduleType = moduleType.name,
            theme = getThemeForModule(moduleType),
            difficultyLevel = difficulty,
            questions = questions,
            estimatedDurationMinutes = profile.sessionDurationMinutes,
            encouragementMessages = listOf(
                "Great job! Keep going! 🌟",
                "You're doing amazing! ✨",
                "Wonderful work! 🎉",
                "Fantastic! Keep it up! 👏",
                "You're a star! ⭐"
            ),
            completionMessage = "Amazing job completing this session! You're making great progress! 🏆"
        )
    }
    
    override suspend fun adaptSession(
        profile: VisualProfile,
        moduleType: ModuleType,
        questionsCompleted: Int,
        correctSoFar: Int,
        averageResponseTimeMs: Long
    ): Result<List<GeneratedQuestion>> = runCatching {
        
        val accuracy = if (questionsCompleted > 0) (correctSoFar * 100) / questionsCompleted else 50
        val adjustedDifficulty = when {
            accuracy < 50 -> 1
            accuracy > 85 -> 3
            else -> 2
        }
        
        generateRecognitionQuestions(profile, adjustedDifficulty, 3)
    }
    
    override suspend fun generateWarmUp(
        profile: VisualProfile
    ): Result<GeneratedQuestion> = runCatching {
        
        val color = profile.getPrimaryColor()
        val (emoji, name) = getSimpleObject(color)
        
        GeneratedQuestion(
            id = "warmup_${Clock.System.now().toEpochMilliseconds()}",
            type = "recognition",
            prompt = "Find the $name!",
            promptAudio = "Let's warm up! Can you find the $name?",
            correctAnswer = emoji,
            distractors = emptyList(),
            hint = "It's the $color one!",
            successFeedback = "Perfect! You found it! Let's start learning! 🎉",
            retryFeedback = "That's the $name! Tap on it!",
            visualConfig = VisualConfig(
                backgroundColor = "black",
                accentColor = color,
                useAnimation = true,
                itemSize = "large",
                displayTimeoutMs = 10000
            )
        )
    }
    
    // --- Recognition Questions ---
    
    private fun generateRecognitionQuestions(
        profile: VisualProfile,
        difficulty: Int,
        count: Int
    ): List<GeneratedQuestion> {
        val items = getRecognitionItems(difficulty)
        val distractorCount = when {
            profile.arrayComplexity <= 2 -> 1
            profile.arrayComplexity <= 3 -> 2
            else -> 3
        }
        
        return items.shuffled().take(count).mapIndexed { index, item ->
            val distractors = items
                .filter { it.emoji != item.emoji }
                .shuffled()
                .take(distractorCount)
                .map { it.emoji }
            
            GeneratedQuestion(
                id = "recog_$index",
                type = "recognition",
                prompt = "Find the ${item.name}",
                promptAudio = "Can you find the ${item.name}?",
                correctAnswer = item.emoji,
                distractors = distractors,
                hint = item.hint,
                successFeedback = "Great job! That's the ${item.name}! ${item.emoji}",
                retryFeedback = "That's not the ${item.name}. Look for ${item.hint.lowercase()}",
                visualConfig = createVisualConfig(profile)
            )
        }
    }
    
    // --- Cause & Effect Questions ---
    
    private fun generateCauseEffectQuestions(
        profile: VisualProfile,
        difficulty: Int,
        count: Int
    ): List<GeneratedQuestion> {
        val actions = listOf(
            CauseEffectItem("🐕", "dog", "Woof! Woof!", "The dog barks!"),
            CauseEffectItem("🐱", "cat", "Meow!", "The cat meows!"),
            CauseEffectItem("🔔", "bell", "Ring ring!", "The bell rings!"),
            CauseEffectItem("🎺", "trumpet", "Ta-da!", "The trumpet plays!"),
            CauseEffectItem("🎸", "guitar", "Strum strum!", "The guitar plays!"),
            CauseEffectItem("🚗", "car", "Beep beep!", "The car honks!"),
            CauseEffectItem("🐔", "chicken", "Cluck cluck!", "The chicken clucks!"),
            CauseEffectItem("🦁", "lion", "Roar!", "The lion roars!"),
            CauseEffectItem("⭐", "star", "Twinkle twinkle!", "The star shines!"),
            CauseEffectItem("🌈", "rainbow", "Wow! Colors!", "A rainbow appears!")
        )
        
        return actions.shuffled().take(count).mapIndexed { index, item ->
            GeneratedQuestion(
                id = "cause_$index",
                type = "cause_effect",
                prompt = "Tap the ${item.name}!",
                promptAudio = "Tap the ${item.name} to see what happens!",
                correctAnswer = item.emoji,
                distractors = emptyList(),
                hint = "Touch the ${item.name}!",
                successFeedback = "${item.sound} ${item.effect}",
                retryFeedback = "Tap the ${item.name}!",
                visualConfig = createVisualConfig(profile).copy(useAnimation = true)
            )
        }
    }
    
    // --- Sorting Questions ---
    
    private fun generateSortingQuestions(
        profile: VisualProfile,
        difficulty: Int,
        count: Int
    ): List<GeneratedQuestion> {
        val sortingSets = listOf(
            SortingSet("Fruits", "🍎🍌🍊", "Vegetables", "🥕🥦🌽"),
            SortingSet("Animals", "🐕🐱🐰", "Vehicles", "🚗🚌🚁"),
            SortingSet("Big", "🐘🦁🐻", "Small", "🐭🐰🐦"),
            SortingSet("Sky", "☀️🌙⭐", "Ground", "🌳🌷🌻"),
            SortingSet("Hot", "☀️🔥🌶️", "Cold", "❄️🧊⛄")
        )
        
        return sortingSets.shuffled().take(count).mapIndexed { index, set ->
            val allItems = set.category1Items + set.category2Items
            
            GeneratedQuestion(
                id = "sort_$index",
                type = "sorting",
                prompt = "Sort into ${set.category1Name} and ${set.category2Name}",
                promptAudio = "Can you sort these into ${set.category1Name} and ${set.category2Name}?",
                correctAnswer = "${set.category1Name}:${set.category1Items}|${set.category2Name}:${set.category2Items}",
                distractors = allItems.split("").filter { it.isNotEmpty() },
                hint = "Put ${set.category1Name} on the left, ${set.category2Name} on the right",
                successFeedback = "Perfect sorting! You got them all! 🎉",
                retryFeedback = "Try again! ${set.category1Items.first()} goes with ${set.category1Name}",
                visualConfig = createVisualConfig(profile)
            )
        }
    }
    
    // --- Matching Questions ---
    
    private fun generateMatchingQuestions(
        profile: VisualProfile,
        difficulty: Int,
        count: Int
    ): List<GeneratedQuestion> {
        val pairs = listOf(
            "🍎" to "🍎", "🐕" to "🐕", "🚗" to "🚗", "⭐" to "⭐",
            "🌙" to "🌙", "🎈" to "🎈", "🌺" to "🌺", "🐱" to "🐱"
        )
        
        val pairCount = when {
            profile.arrayComplexity <= 2 -> 2
            profile.arrayComplexity <= 3 -> 3
            else -> 4
        }
        
        return (0 until count).map { index ->
            val selectedPairs = pairs.shuffled().take(pairCount)
            
            GeneratedQuestion(
                id = "match_$index",
                type = "matching",
                prompt = "Find the matching pairs!",
                promptAudio = "Can you find all the matching pairs?",
                correctAnswer = selectedPairs.joinToString(",") { "${it.first}-${it.second}" },
                distractors = selectedPairs.flatMap { listOf(it.first, it.second) }.shuffled(),
                hint = "Look for two of the same!",
                successFeedback = "You found all the matches! 🎉",
                retryFeedback = "Keep looking! Find two that are the same.",
                visualConfig = createVisualConfig(profile)
            )
        }
    }
    
    // --- Sequencing Questions ---
    
    private fun generateSequencingQuestions(
        profile: VisualProfile,
        difficulty: Int,
        count: Int
    ): List<GeneratedQuestion> {
        val sequences = listOf(
            SequenceItem("1️⃣ 2️⃣ 3️⃣", "numbers", "Count: one, two, three"),
            SequenceItem("🌱 🌿 🌳", "tree growing", "First a seed, then a sprout, then a tree"),
            SequenceItem("🥚 🐣 🐔", "chicken life", "First an egg, then a chick, then a chicken"),
            SequenceItem("☀️ 🌤️ 🌙", "day to night", "Morning sun, afternoon clouds, night moon"),
            SequenceItem("🚶 🏃 🚴", "going faster", "Walk, run, bike - getting faster!")
        )
        
        return sequences.shuffled().take(count).mapIndexed { index, seq ->
            val items = seq.sequence.split(" ")
            
            GeneratedQuestion(
                id = "seq_$index",
                type = "sequencing",
                prompt = "Put in order: ${seq.name}",
                promptAudio = "Can you put these in the right order? ${seq.description}",
                correctAnswer = seq.sequence,
                distractors = items.shuffled(),
                hint = seq.description,
                successFeedback = "Perfect order! ${seq.description} 🎉",
                retryFeedback = "Try again! Think about what comes first.",
                visualConfig = createVisualConfig(profile)
            )
        }
    }
    
    // --- Helper Methods ---
    
    private fun calculateDifficulty(profile: VisualProfile, performance: SessionPerformance?): Int {
        val baseDifficulty = when (profile.cviPhase) {
            CVIPhase.PHASE_I -> 1
            CVIPhase.PHASE_II -> 2
            CVIPhase.PHASE_III -> 3
        }
        
        val adjustment = when {
            performance == null -> 0
            performance.accuracy >= 85 -> 1
            performance.accuracy <= 40 -> -1
            else -> 0
        }
        
        return (baseDifficulty + adjustment).coerceIn(1, 5)
    }
    
    private fun calculateQuestionCount(profile: VisualProfile, performance: SessionPerformance?): Int {
        val baseCount = when (profile.cviPhase) {
            CVIPhase.PHASE_I -> 4
            CVIPhase.PHASE_II -> 6
            CVIPhase.PHASE_III -> 8
        }
        
        val adjustment = when {
            performance == null -> 0
            performance.accuracy >= 80 -> 2
            performance.accuracy <= 40 -> -2
            else -> 0
        }
        
        return (baseCount + adjustment).coerceIn(3, 10)
    }
    
    private fun createVisualConfig(profile: VisualProfile): VisualConfig {
        return VisualConfig(
            backgroundColor = "black",
            accentColor = profile.getPrimaryColor(),
            useAnimation = profile.shouldEnableAnimations(),
            itemSize = when (profile.cviPhase) {
                CVIPhase.PHASE_I -> "large"
                CVIPhase.PHASE_II -> "medium"
                CVIPhase.PHASE_III -> "medium"
            },
            displayTimeoutMs = profile.getResponseTimeout()
        )
    }
    
    private fun getThemeForModule(moduleType: ModuleType): String {
        return when (moduleType) {
            ModuleType.RECOGNITION -> "Object Recognition Practice"
            ModuleType.CAUSE_EFFECT -> "Touch & Discover"
            ModuleType.SORTING -> "Sorting Fun"
            ModuleType.MATCHING -> "Memory Match"
            ModuleType.SEQUENCING -> "Order & Sequence"
            else -> "Learning Activity"
        }
    }
    
    private fun getSimpleObject(color: String): Pair<String, String> {
        return when (color.lowercase()) {
            "yellow" -> "⭐" to "star"
            "red" -> "🍎" to "apple"
            "blue" -> "🔵" to "blue circle"
            "green" -> "🌳" to "tree"
            "orange" -> "🍊" to "orange"
            "purple" -> "🍇" to "grapes"
            else -> "⭐" to "star"
        }
    }
    
    private fun getRecognitionItems(difficulty: Int): List<RecognitionItem> {
        val easyItems = listOf(
            RecognitionItem("🍎", "apple", "It's red and round"),
            RecognitionItem("🍌", "banana", "It's yellow and long"),
            RecognitionItem("🐕", "dog", "It says woof"),
            RecognitionItem("🐱", "cat", "It says meow"),
            RecognitionItem("🚗", "car", "It has wheels"),
            RecognitionItem("⭐", "star", "It shines bright")
        )
        
        val mediumItems = listOf(
            RecognitionItem("🦁", "lion", "King of the jungle"),
            RecognitionItem("🐘", "elephant", "It has a trunk"),
            RecognitionItem("🚌", "bus", "Many people ride it"),
            RecognitionItem("🌺", "flower", "It grows in gardens"),
            RecognitionItem("🏠", "house", "People live in it"),
            RecognitionItem("📚", "book", "You read it")
        )
        
        return when {
            difficulty <= 2 -> easyItems
            else -> easyItems + mediumItems
        }
    }
    
    // Data classes for content generation
    private data class RecognitionItem(val emoji: String, val name: String, val hint: String)
    private data class CauseEffectItem(val emoji: String, val name: String, val sound: String, val effect: String)
    private data class SortingSet(val category1Name: String, val category1Items: String, val category2Name: String, val category2Items: String)
    private data class SequenceItem(val sequence: String, val name: String, val description: String)
}
