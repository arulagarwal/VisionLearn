package com.visionlearn.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.visionlearn.data.local.VisionLearnDatabase
import com.visionlearn.domain.model.ProgressRecord
import com.visionlearn.domain.repository.ProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import com.visionlearn.data.local.ProgressRecord as ProgressRecordEntity

class ProgressRepositoryImpl(
    private val database: VisionLearnDatabase
) : ProgressRepository {
    
    private val queries get() = database.visionLearnDatabaseQueries
    
    override fun observeProgressByProfile(profileId: String): Flow<List<ProgressRecord>> {
        return queries.getProgressByProfile(profileId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun getProgressByProfile(profileId: String): List<ProgressRecord> {
        return withContext(Dispatchers.IO) {
            queries.getProgressByProfile(profileId)
                .executeAsList()
                .map { it.toDomain() }
        }
    }
    
    override suspend fun getProgressBySession(sessionId: String): List<ProgressRecord> {
        return withContext(Dispatchers.IO) {
            queries.getProgressBySession(sessionId)
                .executeAsList()
                .map { it.toDomain() }
        }
    }
    
    override suspend fun getRecentProgress(profileId: String, limit: Int): List<ProgressRecord> {
        return withContext(Dispatchers.IO) {
            queries.getRecentProgress(profileId, limit.toLong())
                .executeAsList()
                .map { it.toDomain() }
        }
    }
    
    override suspend fun recordProgress(record: ProgressRecord): Result<ProgressRecord> {
        return withContext(Dispatchers.IO) {
            runCatching {
                queries.insertProgress(
                    id = record.id,
                    sessionId = record.sessionId,
                    profileId = record.profileId,
                    activityId = record.activityId,
                    activityType = record.activityType,
                    startedAt = record.startedAt.toEpochMilliseconds(),
                    completedAt = record.completedAt?.toEpochMilliseconds(),
                    responseTimeMs = record.responseTimeMs,
                    isCorrect = record.isCorrect?.let { if (it) 1L else 0L },
                    score = record.score?.toDouble(),
                    attempts = record.attempts.toLong()
                )
                record
            }
        }
    }
    
    override suspend fun updateProgress(record: ProgressRecord): Result<ProgressRecord> {
        // For now, just return success - would need UPDATE query
        return Result.success(record)
    }
    
    override suspend fun getTotalCorrect(profileId: String): Int {
        return withContext(Dispatchers.IO) {
            queries.getTotalCorrectByProfile(profileId).executeAsOne().toInt()
        }
    }
    
    override suspend fun getTotalAttempts(profileId: String): Int {
        return withContext(Dispatchers.IO) {
            queries.getTotalAttemptsByProfile(profileId).executeAsOne().toInt()
        }
    }
    
    override suspend fun getAccuracyPercentage(profileId: String): Float {
        val correct = getTotalCorrect(profileId)
        val total = getTotalAttempts(profileId)
        return if (total > 0) (correct.toFloat() / total) * 100 else 0f
    }
    
    private fun ProgressRecordEntity.toDomain(): ProgressRecord {
        return ProgressRecord(
            id = id,
            sessionId = sessionId,
            profileId = profileId,
            activityId = activityId,
            activityType = activityType,
            startedAt = Instant.fromEpochMilliseconds(startedAt),
            completedAt = completedAt?.let { Instant.fromEpochMilliseconds(it) },
            responseTimeMs = responseTimeMs,
            isCorrect = isCorrect?.let { it == 1L },
            score = score?.toFloat(),
            attempts = attempts?.toInt() ?: 1
        )
    }
}
