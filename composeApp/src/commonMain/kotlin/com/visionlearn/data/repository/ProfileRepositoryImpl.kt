package com.visionlearn.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.visionlearn.data.local.VisionLearnDatabase
import com.visionlearn.domain.model.*
import com.visionlearn.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import com.visionlearn.data.local.VisualProfile as VisualProfileEntity

/**
 * Implementation of ProfileRepository using SQLDelight
 */
class ProfileRepositoryImpl(
    private val database: VisionLearnDatabase,
    private val json: Json
) : ProfileRepository {
    
    private val queries get() = database.visionLearnDatabaseQueries
    
    override suspend fun getProfilesByUser(userId: String): List<VisualProfile> = 
        withContext(Dispatchers.IO) {
            queries.getProfilesByUser(userId).executeAsList().map { it.toDomain(json) }
        }
    
    override suspend fun getProfile(profileId: String): VisualProfile? =
        withContext(Dispatchers.IO) {
            queries.getProfileById(profileId).executeAsOneOrNull()?.toDomain(json)
        }
    
    override suspend fun getActiveProfile(): VisualProfile? =
        withContext(Dispatchers.IO) {
            queries.getActiveProfile().executeAsOneOrNull()?.toDomain(json)
        }
    
    override fun observeActiveProfile(): Flow<VisualProfile?> =
        queries.getActiveProfile()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain(json) }
    
    override suspend fun getAllProfiles(): List<VisualProfile> =
        withContext(Dispatchers.IO) {
            queries.getAllProfiles().executeAsList().map { it.toDomain(json) }
        }
    
    override suspend fun saveProfile(profile: VisualProfile) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        val colorsJson = json.encodeToString(
            ListSerializer(String.serializer()),
            profile.preferredColors
        )
        
        // Check if this is the first profile - if so, make it active
        val existingProfiles = queries.getAllProfiles().executeAsList()
        val shouldBeActive = existingProfiles.isEmpty()
        
        queries.insertProfile(
            id = profile.id,
            userId = profile.userId,
            childName = profile.childName,
            childAge = profile.childAge?.toLong(),
            cviPhase = profile.cviPhase.name,
            visualAttention = profile.visualAttention.toLong(),
            visualRecognition = profile.visualRecognition.toLong(),
            visualCuriosity = profile.visualCuriosity.toLong(),
            objectComplexity = profile.objectComplexity.toLong(),
            arrayComplexity = profile.arrayComplexity.toLong(),
            sensoryComplexity = profile.sensoryComplexity.toLong(),
            lightPreference = profile.lightPreference.name,
            preferredColors = colorsJson,
            movementNeed = profile.movementNeed.toLong(),
            visualLatency = profile.visualLatency.name,
            visualFieldPreference = profile.visualFieldPreference.name,
            distanceViewing = profile.distanceViewing.toLong(),
            visualMotorIntegration = profile.visualMotorIntegration.toLong(),
            visualReflexes = profile.visualReflexes.name,
            noveltyTolerance = profile.noveltyTolerance.toLong(),
            faceRecognition = profile.faceRecognition.toLong(),
            sessionDurationMinutes = profile.sessionDurationMinutes.toLong(),
            inputMethod = profile.inputMethod.name,
            audioFeedbackEnabled = if (profile.audioFeedbackEnabled) 1L else 0L,
            hapticFeedbackEnabled = if (profile.hapticFeedbackEnabled) 1L else 0L,
            ttsRate = profile.ttsRate.toDouble(),
            isActive = if (shouldBeActive) 1L else 0L,
            createdAt = profile.createdAt.toEpochMilliseconds(),
            updatedAt = now
        )
        
        // Also set as active via the setActiveProfile query to ensure only one is active
        if (shouldBeActive) {
            queries.setActiveProfile(profile.id)
        }
    }
    
    override suspend fun updateProfile(profile: VisualProfile) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        val colorsJson = json.encodeToString(
            ListSerializer(String.serializer()),
            profile.preferredColors
        )
        
        queries.updateProfile(
            childName = profile.childName,
            childAge = profile.childAge?.toLong(),
            cviPhase = profile.cviPhase.name,
            visualAttention = profile.visualAttention.toLong(),
            visualRecognition = profile.visualRecognition.toLong(),
            visualCuriosity = profile.visualCuriosity.toLong(),
            objectComplexity = profile.objectComplexity.toLong(),
            arrayComplexity = profile.arrayComplexity.toLong(),
            sensoryComplexity = profile.sensoryComplexity.toLong(),
            lightPreference = profile.lightPreference.name,
            preferredColors = colorsJson,
            movementNeed = profile.movementNeed.toLong(),
            visualLatency = profile.visualLatency.name,
            visualFieldPreference = profile.visualFieldPreference.name,
            distanceViewing = profile.distanceViewing.toLong(),
            visualMotorIntegration = profile.visualMotorIntegration.toLong(),
            visualReflexes = profile.visualReflexes.name,
            noveltyTolerance = profile.noveltyTolerance.toLong(),
            faceRecognition = profile.faceRecognition.toLong(),
            sessionDurationMinutes = profile.sessionDurationMinutes.toLong(),
            inputMethod = profile.inputMethod.name,
            audioFeedbackEnabled = if (profile.audioFeedbackEnabled) 1L else 0L,
            hapticFeedbackEnabled = if (profile.hapticFeedbackEnabled) 1L else 0L,
            ttsRate = profile.ttsRate.toDouble(),
            updatedAt = now,
            id = profile.id
        )
    }
    
    override suspend fun setActiveProfile(profileId: String) = withContext(Dispatchers.IO) {
        queries.setActiveProfile(profileId)
    }
    
    override suspend fun deleteProfile(profileId: String) = withContext(Dispatchers.IO) {
        queries.deleteProfile(profileId)
    }
    
    override suspend fun createDefaultUser(userId: String, name: String) = 
        withContext(Dispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            queries.insertUser(
                id = userId,
                name = name,
                email = null,
                role = "parent",
                createdAt = now,
                updatedAt = now
            )
        }
    
    override suspend fun getUser(userId: String): User? = withContext(Dispatchers.IO) {
        queries.getUserById(userId).executeAsOneOrNull()?.let { dbUser ->
            User(
                id = dbUser.id,
                name = dbUser.name,
                email = dbUser.email,
                role = try { UserRole.valueOf(dbUser.role.uppercase()) } catch (e: Exception) { UserRole.PARENT },
                createdAt = Instant.fromEpochMilliseconds(dbUser.createdAt),
                updatedAt = Instant.fromEpochMilliseconds(dbUser.updatedAt)
            )
        }
    }
}

/**
 * Extension function to convert database entity to domain model
 */
private fun VisualProfileEntity.toDomain(json: Json): VisualProfile {
    val colors: List<String> = try {
        json.decodeFromString(
            ListSerializer(String.serializer()),
            preferredColors ?: "[\"yellow\"]"
        )
    } catch (e: Exception) {
        listOf("yellow")
    }
    
    return VisualProfile(
        id = id,
        userId = userId,
        childName = childName,
        childAge = childAge?.toInt(),
        cviPhase = try { CVIPhase.valueOf(cviPhase) } catch (e: Exception) { CVIPhase.PHASE_II },
        visualAttention = visualAttention?.toInt() ?: 3,
        visualRecognition = visualRecognition?.toInt() ?: 3,
        visualCuriosity = visualCuriosity?.toInt() ?: 3,
        objectComplexity = objectComplexity?.toInt() ?: 3,
        arrayComplexity = arrayComplexity?.toInt() ?: 3,
        sensoryComplexity = sensoryComplexity?.toInt() ?: 3,
        lightPreference = lightPreference?.let { try { LightPreference.valueOf(it) } catch (e: Exception) { null } } ?: LightPreference.NEUTRAL,
        preferredColors = colors,
        movementNeed = movementNeed?.toInt() ?: 3,
        visualLatency = visualLatency?.let { try { LatencyLevel.valueOf(it) } catch (e: Exception) { null } } ?: LatencyLevel.MILD,
        visualFieldPreference = visualFieldPreference?.let { try { VisualField.valueOf(it) } catch (e: Exception) { null } } ?: VisualField.CENTER,
        distanceViewing = distanceViewing?.toInt() ?: 3,
        visualMotorIntegration = visualMotorIntegration?.toInt() ?: 3,
        visualReflexes = visualReflexes?.let { try { ReflexStatus.valueOf(it) } catch (e: Exception) { null } } ?: ReflexStatus.TYPICAL,
        noveltyTolerance = noveltyTolerance?.toInt() ?: 3,
        faceRecognition = faceRecognition?.toInt() ?: 3,
        sessionDurationMinutes = sessionDurationMinutes?.toInt() ?: 15,
        inputMethod = inputMethod?.let { try { InputMethod.valueOf(it) } catch (e: Exception) { null } } ?: InputMethod.TOUCH,
        audioFeedbackEnabled = audioFeedbackEnabled == 1L,
        hapticFeedbackEnabled = hapticFeedbackEnabled == 1L,
        ttsRate = ttsRate?.toFloat() ?: 1.0f,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}
