package com.visionlearn.ai

import com.visionlearn.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Gemini AI Service - Uses Google's free tier API
 * 
 * Free tier limits:
 * - 15 requests per minute (RPM)
 * - 1,000,000 tokens per minute (TPM)
 * - 1,500 requests per day (RPD)
 * 
 * No credit card required!
 */
class GeminiAIService(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val json: Json
) : AIService {
    
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val model = "gemini-2.0-flash" // Fast and free
    
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun analyzeImage(imageBytes: ByteArray): Result<ImageAnalysisResult> {
        return runCatching {
            val base64Image = Base64.encode(imageBytes)
            
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", """
                                    Analyze this image for a child with Cortical Visual Impairment (CVI).
                                    
                                    Provide a JSON response with these fields:
                                    {
                                        "shortDescription": "Brief 1-sentence description",
                                        "detailedDescription": "2-3 sentence detailed description",
                                        "identifiedObjects": ["list", "of", "objects"],
                                        "dominantColors": ["color1", "color2"],
                                        "complexityScore": 1-5 (1=very simple, 5=very complex),
                                        "recommendedPhase": "PHASE_I" or "PHASE_II" or "PHASE_III",
                                        "simplificationSuggestions": ["suggestion1", "suggestion2"]
                                    }
                                    
                                    Consider:
                                    - Visual clutter (fewer objects = better for CVI)
                                    - Contrast levels
                                    - Color preferences (yellow, red are often preferred)
                                    - Background simplicity
                                    
                                    Return ONLY the JSON, no other text.
                                """.trimIndent())
                            }
                            addJsonObject {
                                putJsonObject("inline_data") {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                }
                            }
                        }
                    }
                }
            }
            
            val response = makeRequest(requestBody)
            parseImageAnalysisResponse(response)
        }
    }
    
    override suspend fun simplifyDescription(
        description: String,
        targetPhase: CVIPhase
    ): Result<String> {
        return runCatching {
            val phaseGuidelines = when (targetPhase) {
                CVIPhase.PHASE_I -> "Use maximum 5 words. Single simple concept. Very basic vocabulary."
                CVIPhase.PHASE_II -> "Use maximum 10 words. 1-2 short sentences. Simple vocabulary."
                CVIPhase.PHASE_III -> "Use maximum 20 words. Can include more detail but keep it clear."
            }
            
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", """
                                    Simplify this description for a child with CVI at $targetPhase:
                                    
                                    Original: "$description"
                                    
                                    Guidelines: $phaseGuidelines
                                    
                                    Return ONLY the simplified text, nothing else.
                                """.trimIndent())
                            }
                        }
                    }
                }
            }
            
            val response = makeRequest(requestBody)
            extractTextFromResponse(response)
        }
    }
    
    override suspend fun generateAudioDescription(
        imageDescription: String,
        profile: VisualProfile
    ): Result<String> {
        return runCatching {
            val colorHint = if (profile.preferredColors.isNotEmpty()) {
                "Mention if ${profile.preferredColors.joinToString(" or ")} colors are present."
            } else ""
            
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", """
                                    Convert this image description to a spoken audio description for a child with CVI:
                                    
                                    Description: "$imageDescription"
                                    Child's CVI Phase: ${profile.cviPhase}
                                    $colorHint
                                    
                                    Guidelines:
                                    - Use natural, conversational language
                                    - Add brief pauses (use commas)
                                    - Start with the most important element
                                    - Keep it encouraging and engaging
                                    - Maximum ${if (profile.cviPhase == CVIPhase.PHASE_I) "10" else "20"} words
                                    
                                    Return ONLY the audio description text.
                                """.trimIndent())
                            }
                        }
                    }
                }
            }
            
            val response = makeRequest(requestBody)
            extractTextFromResponse(response)
        }
    }
    
    override suspend fun suggestActivities(
        profile: VisualProfile
    ): Result<List<ActivitySuggestion>> {
        return runCatching {
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", """
                                    Suggest learning activities for a child with CVI based on their profile:
                                    
                                    CVI Phase: ${profile.cviPhase}
                                    Visual Attention: ${profile.visualAttention}/5
                                    Visual Recognition: ${profile.visualRecognition}/5
                                    Object Complexity Tolerance: ${profile.objectComplexity}/5
                                    Array Complexity Tolerance: ${profile.arrayComplexity}/5
                                    Preferred Colors: ${profile.preferredColors.joinToString(", ")}
                                    Movement Need: ${profile.movementNeed}/5
                                    
                                    Available activity types: RECOGNITION, CAUSE_EFFECT, SORTING, MATCHING, SEQUENCING
                                    
                                    Return a JSON array of 3 suggestions:
                                    [
                                        {
                                            "moduleType": "ACTIVITY_TYPE",
                                            "title": "Activity Title",
                                            "description": "Brief description",
                                            "targetBehaviors": ["behavior1", "behavior2"],
                                            "estimatedDifficulty": 1-5,
                                            "reasoning": "Why this activity suits the profile"
                                        }
                                    ]
                                    
                                    Return ONLY the JSON array.
                                """.trimIndent())
                            }
                        }
                    }
                }
            }
            
            val response = makeRequest(requestBody)
            parseActivitySuggestions(response)
        }
    }
    
    override suspend fun generateProgressInsights(
        profile: VisualProfile,
        records: List<ProgressRecord>
    ): Result<ProgressInsights> {
        return runCatching {
            if (records.isEmpty()) {
                return@runCatching createEmptyInsights(profile)
            }
            
            val totalAttempts = records.size
            val correctCount = records.count { it.isCorrect == true }
            val accuracy = if (totalAttempts > 0) (correctCount * 100) / totalAttempts else 0
            val avgResponseTime = records.mapNotNull { it.responseTimeMs }.average().toLong()
            
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", """
                                    Generate progress insights for a child with CVI:
                                    
                                    Profile: ${profile.cviPhase}, Age: ${profile.childAge ?: "unknown"}
                                    Total Activities: $totalAttempts
                                    Correct: $correctCount ($accuracy%)
                                    Average Response Time: ${avgResponseTime}ms
                                    
                                    Return a JSON object:
                                    {
                                        "summary": "2-3 sentence encouraging summary",
                                        "strongestVisualBehaviors": ["strength1", "strength2"],
                                        "areasForImprovement": ["area1", "area2"],
                                        "recommendations": ["recommendation1", "recommendation2", "recommendation3"]
                                    }
                                    
                                    Be encouraging and specific. Return ONLY the JSON.
                                """.trimIndent())
                            }
                        }
                    }
                }
            }
            
            val response = makeRequest(requestBody)
            parseProgressInsights(response, profile, records)
        }
    }
    
    override suspend fun assessContentAppropriateness(
        content: String,
        profile: VisualProfile
    ): Result<ContentAssessment> {
        return runCatching {
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", """
                                    Assess if this content is appropriate for a child with CVI:
                                    
                                    Content: "$content"
                                    CVI Phase: ${profile.cviPhase}
                                    Object Complexity Tolerance: ${profile.objectComplexity}/5
                                    
                                    Return a JSON object:
                                    {
                                        "isAppropriate": true/false,
                                        "complexityScore": 1-5,
                                        "concerns": ["concern1", "concern2"],
                                        "suggestions": ["suggestion1", "suggestion2"]
                                    }
                                    
                                    Return ONLY the JSON.
                                """.trimIndent())
                            }
                        }
                    }
                }
            }
            
            val response = makeRequest(requestBody)
            parseContentAssessment(response)
        }
    }
    
    // --- Private helper methods ---
    
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
            ?: throw Exception("Could not extract text from response")
    }
    
    private fun extractJsonFromText(text: String): String {
        // Remove markdown code blocks if present
        return text
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }
    
    private fun parseImageAnalysisResponse(responseText: String): ImageAnalysisResult {
        val text = extractTextFromResponse(responseText)
        val jsonText = extractJsonFromText(text)
        val parsed = json.parseToJsonElement(jsonText).jsonObject
        
        return ImageAnalysisResult(
            shortDescription = parsed["shortDescription"]?.jsonPrimitive?.content ?: "Image analyzed",
            detailedDescription = parsed["detailedDescription"]?.jsonPrimitive?.content ?: "",
            identifiedObjects = parsed["identifiedObjects"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            dominantColors = parsed["dominantColors"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            complexityScore = parsed["complexityScore"]?.jsonPrimitive?.intOrNull ?: 3,
            recommendedPhase = try {
                CVIPhase.valueOf(parsed["recommendedPhase"]?.jsonPrimitive?.content ?: "PHASE_II")
            } catch (e: Exception) {
                CVIPhase.PHASE_II
            },
            simplificationSuggestions = parsed["simplificationSuggestions"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        )
    }
    
    private fun parseActivitySuggestions(responseText: String): List<ActivitySuggestion> {
        val text = extractTextFromResponse(responseText)
        val jsonText = extractJsonFromText(text)
        val parsed = json.parseToJsonElement(jsonText).jsonArray
        
        return parsed.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                ActivitySuggestion(
                    moduleType = obj["moduleType"]?.jsonPrimitive?.content ?: "CAUSE_EFFECT",
                    title = obj["title"]?.jsonPrimitive?.content ?: "Activity",
                    description = obj["description"]?.jsonPrimitive?.content ?: "",
                    targetBehaviors = obj["targetBehaviors"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    estimatedDifficulty = obj["estimatedDifficulty"]?.jsonPrimitive?.intOrNull ?: 2,
                    reasoning = obj["reasoning"]?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun parseProgressInsights(
        responseText: String,
        profile: VisualProfile,
        records: List<ProgressRecord>
    ): ProgressInsights {
        val text = extractTextFromResponse(responseText)
        val jsonText = extractJsonFromText(text)
        val parsed = json.parseToJsonElement(jsonText).jsonObject
        
        val startDate = records.minOfOrNull { it.startedAt.toEpochMilliseconds() } ?: 0L
        val endDate = records.maxOfOrNull { it.completedAt?.toEpochMilliseconds() ?: it.startedAt.toEpochMilliseconds() } ?: 0L
        val totalAttempts = records.size
        val correctCount = records.count { it.isCorrect == true }
        val accuracy = if (totalAttempts > 0) (correctCount.toFloat() / totalAttempts) * 100 else 0f
        val avgResponseTime = records.mapNotNull { it.responseTimeMs }.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: 0L
        
        return ProgressInsights(
            profileId = profile.id,
            periodStartDate = startDate,
            periodEndDate = endDate,
            totalSessions = records.map { it.sessionId }.distinct().size,
            totalActivitiesCompleted = totalAttempts,
            averageAccuracy = accuracy,
            averageResponseTimeMs = avgResponseTime,
            averageSessionDurationMs = 0L,
            strongestVisualBehaviors = parsed["strongestVisualBehaviors"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            areasForImprovement = parsed["areasForImprovement"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            recommendations = parsed["recommendations"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            summary = parsed["summary"]?.jsonPrimitive?.content ?: "Keep up the great work!"
        )
    }
    
    private fun parseContentAssessment(responseText: String): ContentAssessment {
        val text = extractTextFromResponse(responseText)
        val jsonText = extractJsonFromText(text)
        val parsed = json.parseToJsonElement(jsonText).jsonObject
        
        return ContentAssessment(
            isAppropriate = parsed["isAppropriate"]?.jsonPrimitive?.booleanOrNull ?: true,
            complexityScore = parsed["complexityScore"]?.jsonPrimitive?.intOrNull ?: 3,
            concerns = parsed["concerns"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            suggestions = parsed["suggestions"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        )
    }
    
    private fun createEmptyInsights(profile: VisualProfile): ProgressInsights {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return ProgressInsights(
            profileId = profile.id,
            periodStartDate = now,
            periodEndDate = now,
            totalSessions = 0,
            totalActivitiesCompleted = 0,
            averageAccuracy = 0f,
            averageResponseTimeMs = 0L,
            averageSessionDurationMs = 0L,
            strongestVisualBehaviors = emptyList(),
            areasForImprovement = listOf("Visual Attention", "Visual Recognition"),
            recommendations = listOf(
                "Start with simple touch-response activities",
                "Use preferred colors (${profile.preferredColors.joinToString()})",
                "Practice for 10-15 minutes daily"
            ),
            summary = "No activities completed yet. Let's start learning!"
        )
    }
}
