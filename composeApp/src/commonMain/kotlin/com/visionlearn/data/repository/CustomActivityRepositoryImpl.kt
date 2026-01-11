package com.visionlearn.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.visionlearn.data.local.VisionLearnDatabase
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.domain.repository.CustomActivity
import com.visionlearn.domain.repository.CustomActivityImage
import com.visionlearn.domain.repository.CustomActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of CustomActivityRepository using SQLDelight
 */
class CustomActivityRepositoryImpl(
    private val database: VisionLearnDatabase
) : CustomActivityRepository {
    
    private val queries = database.visionLearnDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }
    
    override fun getAllActivities(): Flow<List<CustomActivity>> {
        return queries.getAllCustomActivities()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun getActivitiesByType(moduleType: ModuleType): List<CustomActivity> {
        return withContext(Dispatchers.IO) {
            queries.getCustomActivitiesByType(moduleType.name)
                .executeAsList()
                .map { it.toDomain() }
        }
    }
    
    override suspend fun getActivityById(id: String): CustomActivity? {
        return withContext(Dispatchers.IO) {
            queries.getCustomActivityById(id)
                .executeAsOneOrNull()
                ?.toDomain()
        }
    }
    
    override suspend fun createActivity(activity: CustomActivity): Result<CustomActivity> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val now = Clock.System.now().toEpochMilliseconds()
                val imagesJson = json.encodeToString(activity.images.map { it.toJson() })
                
                queries.insertCustomActivity(
                    id = activity.id,
                    title = activity.title,
                    moduleType = activity.moduleType.name,
                    imagesJson = imagesJson,
                    createdAt = activity.createdAt,
                    updatedAt = now,
                    isFromTemplate = if (activity.isFromTemplate) 1L else 0L,
                    templateId = activity.templateId
                )
                
                activity
            }
        }
    }
    
    override suspend fun updateActivity(activity: CustomActivity): Result<CustomActivity> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val now = Clock.System.now().toEpochMilliseconds()
                val imagesJson = json.encodeToString(activity.images.map { it.toJson() })
                
                queries.updateCustomActivity(
                    title = activity.title,
                    imagesJson = imagesJson,
                    updatedAt = now,
                    id = activity.id
                )
                
                activity
            }
        }
    }
    
    override suspend fun deleteActivity(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                queries.deleteCustomActivity(id)
            }
        }
    }
    
    override suspend fun getActivityCount(): Int {
        return withContext(Dispatchers.IO) {
            queries.getCustomActivityCount().executeAsOne().toInt()
        }
    }
    
    // Extension functions for mapping
    
    private fun com.visionlearn.data.local.CustomActivity.toDomain(): CustomActivity {
        val images = try {
            val imageJsonList = json.decodeFromString<List<ImageJson>>(imagesJson)
            imageJsonList.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
        
        return CustomActivity(
            id = id,
            title = title,
            moduleType = ModuleType.valueOf(moduleType),
            images = images,
            createdAt = createdAt,
            isFromTemplate = isFromTemplate == 1L,
            templateId = templateId
        )
    }
    
    private fun CustomActivityImage.toJson(): ImageJson {
        return ImageJson(
            id = id,
            emoji = emoji,
            name = name,
            category = category,
            aiDescription = aiDescription,
            complexityScore = complexityScore
        )
    }
    
    private fun ImageJson.toDomain(): CustomActivityImage {
        return CustomActivityImage(
            id = id,
            emoji = emoji,
            name = name,
            category = category,
            aiDescription = aiDescription,
            complexityScore = complexityScore
        )
    }
}

@kotlinx.serialization.Serializable
private data class ImageJson(
    val id: String,
    val emoji: String,
    val name: String,
    val category: String? = null,
    val aiDescription: String? = null,
    val complexityScore: Int = 2
)
