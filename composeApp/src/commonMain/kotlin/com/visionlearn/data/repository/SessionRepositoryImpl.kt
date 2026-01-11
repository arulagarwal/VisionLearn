package com.visionlearn.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.visionlearn.data.local.VisionLearnDatabase
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.domain.model.Session
import com.visionlearn.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import com.visionlearn.data.local.Session as SessionEntity

class SessionRepositoryImpl(
    private val database: VisionLearnDatabase
) : SessionRepository {
    
    private val queries = database.visionLearnDatabaseQueries
    
    override fun getSessionsByProfile(profileId: String): Flow<List<Session>> {
        return queries.getSessionsByProfile(profileId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun getRecentSessions(profileId: String, limit: Int): List<Session> {
        return withContext(Dispatchers.IO) {
            queries.getRecentSessions(profileId, limit.toLong())
                .executeAsList()
                .map { it.toDomain() }
        }
    }
    
    override suspend fun getSessionById(sessionId: String): Session? {
        return withContext(Dispatchers.IO) {
            queries.getSessionById(sessionId).executeAsOneOrNull()?.toDomain()
        }
    }
    
    override suspend fun createSession(session: Session): Result<Session> {
        return withContext(Dispatchers.IO) {
            runCatching {
                queries.insertSession(
                    id = session.id,
                    profileId = session.profileId,
                    moduleType = session.moduleType?.name,
                    startedAt = session.startedAt.toEpochMilliseconds(),
                    endedAt = session.endedAt?.toEpochMilliseconds(),
                    totalActivities = session.totalActivities.toLong(),
                    completedActivities = session.completedActivities.toLong(),
                    correctAnswers = session.correctAnswers.toLong(),
                    averageResponseTimeMs = session.averageResponseTimeMs,
                    notes = session.notes
                )
                session
            }
        }
    }
    
    override suspend fun updateSession(session: Session): Result<Session> {
        return withContext(Dispatchers.IO) {
            runCatching {
                queries.updateSession(
                    endedAt = session.endedAt?.toEpochMilliseconds(),
                    totalActivities = session.totalActivities.toLong(),
                    completedActivities = session.completedActivities.toLong(),
                    correctAnswers = session.correctAnswers.toLong(),
                    averageResponseTimeMs = session.averageResponseTimeMs,
                    notes = session.notes,
                    id = session.id
                )
                session
            }
        }
    }
    
    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                queries.deleteSession(sessionId)
            }
        }
    }
    
    private fun SessionEntity.toDomain(): Session {
        return Session(
            id = id,
            profileId = profileId,
            moduleType = moduleType?.let { 
                try { ModuleType.valueOf(it) } catch (e: Exception) { null }
            },
            startedAt = Instant.fromEpochMilliseconds(startedAt),
            endedAt = endedAt?.let { Instant.fromEpochMilliseconds(it) },
            totalActivities = totalActivities?.toInt() ?: 0,
            completedActivities = completedActivities?.toInt() ?: 0,
            correctAnswers = correctAnswers?.toInt() ?: 0,
            averageResponseTimeMs = averageResponseTimeMs,
            notes = notes
        )
    }
}
