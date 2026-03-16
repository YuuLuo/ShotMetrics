package com.shotmetrics.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shotmetrics.app.data.local.entity.ImpactEntity
import com.shotmetrics.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("SELECT * FROM impacts WHERE sessionId = :sessionId")
    suspend fun getImpacts(sessionId: Long): List<ImpactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImpacts(impacts: List<ImpactEntity>)

    @Query("DELETE FROM impacts WHERE sessionId = :sessionId")
    suspend fun deleteImpactsBySession(sessionId: Long)

    @Transaction
    suspend fun saveSessionWithImpacts(session: SessionEntity, impacts: List<ImpactEntity>): Long {
        val sessionId = if (session.id == 0L) {
            insertSession(session)
        } else {
            updateSession(session)
            session.id
        }
        deleteImpactsBySession(sessionId)
        if (impacts.isNotEmpty()) {
            insertImpacts(impacts.map { it.copy(sessionId = sessionId) })
        }
        return sessionId
    }

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int
}
