package com.visionlearn.ai

import com.visionlearn.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

/**
 * Gemini-powered session generator
 * Creates personalized learning sessions using AI
 */
class GeminiSessionGenerator(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val json: Json
) : AISessionGenerator {
    
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val model = "gemini-2.0-flash"
    
    override suspend fun generateSession(
        profile: VisualProfile,
        moduleType: ModuleType,
        previousPerformance: SessionPerformance?
    ): Result<GeneratedSession> = runCatching {
        
        val questionCount = calculateQuestionCount(profile, previousPerformance)
        val difficulty = calculateDifficulty(profile, previousPerformance)
        
        val prompt = buildSessionPrompt(profile, moduleType, questionCount, difficulty, previousPerformance)
        
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.7)
                put("maxOutputTokens", 4096)
            }
        }
        
        val response = makeRequest(requestBody)
        parseSessionResponse(response, moduleType, difficulty)
    }
    
    override suspend fun adaptSession(
        profile: VisualProfile,
        moduleType: ModuleType,
        questionsCompleted: Int,
        correctSoFar: Int,
        averageResponseTimeMs: Long
    ): Result<List<GeneratedQuestion>> = runCatching {
        
        val accuracy = if (questionsCompleted > 0) (correctSoFar * 100) / questionsCompleted else 50
        val needsEasier = accuracy < 50
        val needsHarder = accuracy > 85
        
        val adaptationPrompt = """
            Generate 3 follow-up questions for a ${moduleType.displayName} activity.
            
            CURRENT PERFORMANCE:
            - Completed: $questionsCompleted questions
            - Accuracy: $accuracy%
            - Avg response time: ${averageResponseTimeMs}ms
            
            ADAPTATION NEEDED:
            ${when {
                needsEasier -> "Make questions EASIER - simpler choices, more familiar items"
                needsHarder -> "Make questions HARDER - add more distractors, introduce novelty"
                else -> "Keep current difficulty level"
            }}
            
            CHILD PROFILE:
            - CVI Phase: ${profile.cviPhase}
            - Preferred colors: ${profile.preferredColors.joinToString(", ")}
            - Max items on screen: ${profile.getMaxDisplayItems()}
            
            Return JSON array of 3 questions:
            [
                {
                    "id": "adapt_1",
                    "type": "${moduleType.name.lowercase()}",
                    "prompt": "Question text",
                    "promptAudio": "TTS version",
                    "correctAnswer": "answer",
                    "distractors": ["wrong1", "wrong2"],
                    "hint": "helpful hint",
                    "successFeedback": "Great job!",
                    "retryFeedback": "Try again!",
                    "visualConfig": {
                        "backgroundColor": "black",
                        "accentColor": "${profile.getPrimaryColor()}",
                        "useAnimation": ${profile.shouldEnableAnimations()},
                        "itemSize": "${if (needsEasier) "large" else "medium"}",
                        "displayTimeoutMs": ${profile.getResponseTimeout()}
                    }
                }
            ]
            
            Return ONLY the JSON array.
        """.trimIndent()
        
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", adaptationPrompt) }
                    }
                }
            }
        }
        
        val response = makeRequest(requestBody)
        parseQuestionsArray(response)
    }
    
    override suspend fun generateWarmUp(
        profile: VisualProfile
    ): Result<GeneratedQuestion> = runCatching {
        
        val warmUpPrompt = """
            Generate a simple warm-up question for a child with CVI (${profile.cviPhase}).
            
            This should be:
            - Very easy (guaranteed success)
            - Use preferred color: ${profile.getPrimaryColor()}
            - Single object recognition
            - Encouraging tone
            
            Return JSON:
            {
                "id": "warmup_${Clock.System.now().toEpochMilliseconds()}",
                "type": "recognition",
                "prompt": "Can you find the [object]?",
                "promptAudio": "Let's start! Can you find the [object]?",
                "correctAnswer": "[emoji]",
                "distractors": [],
                "hint": "It's the [color] one!",
                "successFeedback": "Amazing! You found it! Let's keep going!",
                "retryFeedback": "That's the [color] [object]! Tap it!",
                "visualConfig": {
                    "backgroundColor": "black",
                    "accentColor": "${profile.getPrimaryColor()}",
                    "useAnimation": true,
                    "itemSize": "large",
                    "displayTimeoutMs": 10000
                }
            }
            
            Return ONLY the JSON object.
        """.trimIndent()
        
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", warmUpPrompt) }
                    }
                }
            }
        }
        
        val response = makeRequest(requestBody)
        parseQuestionObject(response)
    }
    
    // --- Helper Methods ---
    
    private fun buildSessionPrompt(
        profile: VisualProfile,
        moduleType: ModuleType,
        questionCount: Int,
        difficulty: Int,
        performance: SessionPerformance?
    ): String {
        val moduleSpecificInstructions = when (moduleType) {
            ModuleType.RECOGNITION -> """
                IMAGE RECOGNITION SESSION:
                - Show one emoji/image, ask child to identify it
                - Use 2-4 answer choices based on array complexity
                - Start with familiar objects, gradually add novelty
                - Example themes: Animals, Food, Vehicles, Household items
            """.trimIndent()
            
            ModuleType.CAUSE_EFFECT -> """
                CAUSE & EFFECT SESSION:
                - Create interactive "tap to see/hear" activities
                - Each tap reveals something fun (animal sound, color change, animation)
                - Questions should be simple actions: "Tap to hear the dog bark!"
                - Focus on building visual attention through immediate feedback
            """.trimIndent()
            
            ModuleType.SORTING -> """
                SORTING SESSION:
                - Present items to sort into 2 categories
                - Use clear, contrasting categories (fruits vs vegetables, animals vs vehicles)
                - 4-6 items total, drag to correct category
                - Give immediate feedback for each sort
            """.trimIndent()
            
            ModuleType.MATCHING -> """
                MATCHING SESSION:
                - Memory card matching game
                - ${profile.getMaxDisplayItems() / 2} pairs maximum
                - Use high-contrast, simple images
                - Cards flip to reveal content
            """.trimIndent()
            
            ModuleType.SEQUENCING -> """
                SEQUENCING SESSION:
                - Arrange 3-4 items in order (numbers, story steps, size)
                - Clear visual cues for correct order
                - Audio prompts explain the sequence
                - Start simple: 1-2-3, then stories
            """.trimIndent()
            
            else -> "Create engaging learning questions appropriate for the module type."
        }
        
        return """
            Create a personalized ${moduleType.displayName} learning session for a child with CVI.
            
            ═══════════════════════════════════════════════════
            CHILD'S VISUAL PROFILE (Perkins CVI Protocol)
            ═══════════════════════════════════════════════════
            
            CVI Phase: ${profile.cviPhase.name} (${profile.cviPhase.description})
            
            Visual Behaviors (1=most impacted, 5=least impacted):
            • Visual Attention: ${profile.visualAttention}/5
            • Visual Recognition: ${profile.visualRecognition}/5
            • Object Complexity Tolerance: ${profile.objectComplexity}/5
            • Array Complexity Tolerance: ${profile.arrayComplexity}/5
            • Novelty Tolerance: ${profile.noveltyTolerance}/5
            • Movement Need: ${profile.movementNeed}/5
            • Visual Latency: ${profile.visualLatency}
            
            Preferences:
            • Preferred Colors: ${profile.preferredColors.joinToString(", ")}
            • Max items on screen: ${profile.getMaxDisplayItems()}
            • Response timeout: ${profile.getResponseTimeout()}ms
            • Animations: ${if (profile.shouldEnableAnimations()) "ENABLED" else "DISABLED"}
            
            ${performance?.let { """
            ═══════════════════════════════════════════════════
            PREVIOUS PERFORMANCE
            ═══════════════════════════════════════════════════
            • Sessions completed: ${it.totalSessionsCompleted}
            • Last accuracy: ${it.accuracy}%
            • Avg response time: ${it.avgResponseTimeMs}ms
            • Good at: ${it.successfulThemes.joinToString(", ")}
            • Needs practice: ${it.challengingAreas.joinToString(", ")}
            """ } ?: ""}
            
            ═══════════════════════════════════════════════════
            SESSION REQUIREMENTS
            ═══════════════════════════════════════════════════
            
            • Total questions: $questionCount
            • Difficulty level: $difficulty/5
            • Duration target: ${profile.sessionDurationMinutes} minutes
            
            $moduleSpecificInstructions
            
            ═══════════════════════════════════════════════════
            OUTPUT FORMAT
            ═══════════════════════════════════════════════════
            
            Return a JSON object with this structure:
            {
                "id": "session_${Clock.System.now().toEpochMilliseconds()}",
                "moduleType": "${moduleType.name}",
                "theme": "Descriptive theme name",
                "difficultyLevel": $difficulty,
                "questions": [
                    {
                        "id": "q1",
                        "type": "${moduleType.name.lowercase()}",
                        "prompt": "Question displayed on screen",
                        "promptAudio": "TTS-friendly version with natural speech",
                        "correctAnswer": "The correct answer (emoji or text)",
                        "distractors": ["wrong1", "wrong2"],
                        "hint": "A helpful hint if child struggles",
                        "successFeedback": "Encouraging message on correct answer",
                        "retryFeedback": "Gentle prompt to try again",
                        "visualConfig": {
                            "backgroundColor": "black",
                            "accentColor": "${profile.getPrimaryColor()}",
                            "useAnimation": ${profile.shouldEnableAnimations()},
                            "itemSize": "${when (profile.cviPhase) { CVIPhase.PHASE_I -> "large"; CVIPhase.PHASE_II -> "medium"; CVIPhase.PHASE_III -> "medium" }}",
                            "displayTimeoutMs": ${profile.getResponseTimeout()}
                        }
                    }
                ],
                "estimatedDurationMinutes": ${profile.sessionDurationMinutes},
                "encouragementMessages": [
                    "You're doing great!",
                    "Keep going!",
                    "Awesome work!"
                ],
                "completionMessage": "Amazing job completing this session!"
            }
            
            IMPORTANT:
            - Use emojis for visual answers (🍎, 🐕, 🚗, etc.)
            - Keep text prompts SHORT and CLEAR
            - Audio prompts should sound natural when spoken aloud
            - Ensure high contrast (bright colors on dark backgrounds)
            - NO text-only answers for young children
            
            Return ONLY the JSON object, no additional text.
        """.trimIndent()
    }
    
    private fun calculateQuestionCount(profile: VisualProfile, performance: SessionPerformance?): Int {
        val baseCount = when (profile.cviPhase) {
            CVIPhase.PHASE_I -> 4
            CVIPhase.PHASE_II -> 6
            CVIPhase.PHASE_III -> 8
        }
        
        // Adjust based on past performance
        val adjustment = when {
            performance == null -> 0
            performance.accuracy >= 80 -> 2
            performance.accuracy <= 40 -> -2
            else -> 0
        }
        
        return (baseCount + adjustment).coerceIn(3, 10)
    }
    
    private fun calculateDifficulty(profile: VisualProfile, performance: SessionPerformance?): Int {
        val baseDifficulty = when (profile.cviPhase) {
            CVIPhase.PHASE_I -> 1
            CVIPhase.PHASE_II -> 2
            CVIPhase.PHASE_III -> 3
        }
        
        // Adjust based on performance
        val adjustment = when {
            performance == null -> 0
            performance.accuracy >= 85 -> 1
            performance.accuracy <= 40 -> -1
            else -> 0
        }
        
        return (baseDifficulty + adjustment).coerceIn(1, 5)
    }
    
    private suspend fun makeRequest(requestBody: JsonObject): String {
        val response: HttpResponse = httpClient.post("$baseUrl/models/$model:generateContent") {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }
        
        if (!response.status.isSuccess()) {
            throw Exception("Gemini API error: ${response.status} - ${response.bodyAsText()}")
        }
        
        return response.bodyAsText()
    }
    
    private fun extractTextFromResponse(responseText: String): String {
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        return responseJson["candidates"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
            ?.trim()
            ?.replace("```json", "")
            ?.replace("```", "")
            ?.trim()
            ?: throw Exception("Could not extract text from response")
    }
    
    private fun parseSessionResponse(responseText: String, moduleType: ModuleType, difficulty: Int): GeneratedSession {
        val text = extractTextFromResponse(responseText)
        
        return try {
            val parsed = json.parseToJsonElement(text).jsonObject
            
            GeneratedSession(
                id = parsed["id"]?.jsonPrimitive?.content ?: "session_${Clock.System.now().toEpochMilliseconds()}",
                moduleType = parsed["moduleType"]?.jsonPrimitive?.content ?: moduleType.name,
                theme = parsed["theme"]?.jsonPrimitive?.content ?: "Learning Activity",
                difficultyLevel = parsed["difficultyLevel"]?.jsonPrimitive?.intOrNull ?: difficulty,
                questions = parsed["questions"]?.jsonArray?.mapNotNull { parseQuestion(it) } ?: emptyList(),
                estimatedDurationMinutes = parsed["estimatedDurationMinutes"]?.jsonPrimitive?.intOrNull ?: 10,
                encouragementMessages = parsed["encouragementMessages"]?.jsonArray?.map { it.jsonPrimitive.content } 
                    ?: listOf("Great job!", "Keep going!", "You're doing amazing!"),
                completionMessage = parsed["completionMessage"]?.jsonPrimitive?.content 
                    ?: "Fantastic work completing this session!"
            )
        } catch (e: Exception) {
            // Return a fallback session if parsing fails
            createFallbackSession(moduleType, difficulty)
        }
    }
    
    private fun parseQuestion(element: JsonElement): GeneratedQuestion? {
        return try {
            val obj = element.jsonObject
            GeneratedQuestion(
                id = obj["id"]?.jsonPrimitive?.content ?: "q_${Clock.System.now().toEpochMilliseconds()}",
                type = obj["type"]?.jsonPrimitive?.content ?: "recognition",
                prompt = obj["prompt"]?.jsonPrimitive?.content ?: "What is this?",
                promptAudio = obj["promptAudio"]?.jsonPrimitive?.content ?: obj["prompt"]?.jsonPrimitive?.content ?: "What is this?",
                correctAnswer = obj["correctAnswer"]?.jsonPrimitive?.content ?: "🍎",
                distractors = obj["distractors"]?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf("🍌", "🍊"),
                hint = obj["hint"]?.jsonPrimitive?.content ?: "Look carefully!",
                successFeedback = obj["successFeedback"]?.jsonPrimitive?.content ?: "Great job!",
                retryFeedback = obj["retryFeedback"]?.jsonPrimitive?.content ?: "Try again!",
                visualConfig = parseVisualConfig(obj["visualConfig"])
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseVisualConfig(element: JsonElement?): VisualConfig {
        return try {
            val obj = element?.jsonObject ?: return VisualConfig()
            VisualConfig(
                backgroundColor = obj["backgroundColor"]?.jsonPrimitive?.content ?: "black",
                accentColor = obj["accentColor"]?.jsonPrimitive?.content ?: "yellow",
                useAnimation = obj["useAnimation"]?.jsonPrimitive?.booleanOrNull ?: false,
                itemSize = obj["itemSize"]?.jsonPrimitive?.content ?: "large",
                displayTimeoutMs = obj["displayTimeoutMs"]?.jsonPrimitive?.longOrNull ?: 5000
            )
        } catch (e: Exception) {
            VisualConfig()
        }
    }
    
    private fun parseQuestionsArray(responseText: String): List<GeneratedQuestion> {
        val text = extractTextFromResponse(responseText)
        return try {
            json.parseToJsonElement(text).jsonArray.mapNotNull { parseQuestion(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseQuestionObject(responseText: String): GeneratedQuestion {
        val text = extractTextFromResponse(responseText)
        return parseQuestion(json.parseToJsonElement(text)) 
            ?: createFallbackWarmUp()
    }
    
    private fun createFallbackSession(moduleType: ModuleType, difficulty: Int): GeneratedSession {
        return GeneratedSession(
            id = "fallback_${Clock.System.now().toEpochMilliseconds()}",
            moduleType = moduleType.name,
            theme = "Practice Session",
            difficultyLevel = difficulty,
            questions = listOf(
                GeneratedQuestion(
                    id = "fb_1",
                    type = moduleType.name.lowercase(),
                    prompt = "Find the apple",
                    promptAudio = "Can you find the apple?",
                    correctAnswer = "🍎",
                    distractors = listOf("🍌", "🍊"),
                    hint = "It's red!",
                    successFeedback = "Great job!",
                    retryFeedback = "Try again!",
                    visualConfig = VisualConfig()
                )
            ),
            estimatedDurationMinutes = 5,
            encouragementMessages = listOf("Great job!", "Keep going!"),
            completionMessage = "Well done!"
        )
    }
    
    private fun createFallbackWarmUp(): GeneratedQuestion {
        return GeneratedQuestion(
            id = "warmup_fallback",
            type = "recognition",
            prompt = "Find the star",
            promptAudio = "Let's warm up! Can you find the star?",
            correctAnswer = "⭐",
            distractors = emptyList(),
            hint = "It's bright and yellow!",
            successFeedback = "Perfect! Let's start learning!",
            retryFeedback = "Tap the yellow star!",
            visualConfig = VisualConfig(useAnimation = true, itemSize = "large")
        )
    }
}
