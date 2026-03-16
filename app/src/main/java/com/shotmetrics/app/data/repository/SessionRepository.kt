package com.shotmetrics.app.data.repository

import com.shotmetrics.app.data.local.SessionDao
import com.shotmetrics.app.data.local.entity.ImpactEntity
import com.shotmetrics.app.data.local.entity.SessionEntity
import com.shotmetrics.app.domain.model.ShootingSession
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {

    fun getAllSessions(): Flow<List<SessionEntity>> = dao.getAllSessions()

    suspend fun getSession(id: Long): SessionEntity? = dao.getSession(id)

    suspend fun getImpacts(sessionId: Long): List<ImpactEntity> = dao.getImpacts(sessionId)

    suspend fun saveSession(session: ShootingSession): Long {
        val entity = SessionEntity(
            id = session.id,
            imageUri = session.imageUri,
            createdAt = session.createdAt,
            caliber = session.caliber,
            distance = session.distanceToTarget,
            distanceUnit = session.distanceUnit.name,
            scaleFactor = session.scaleFactor,
            referenceSize = session.referenceSize,
            refX1 = session.referencePoints?.first?.x,
            refY1 = session.referencePoints?.first?.y,
            refX2 = session.referencePoints?.second?.x,
            refY2 = session.referencePoints?.second?.y,
            poaX = session.pointOfAim?.x,
            poaY = session.pointOfAim?.y,
            groupSizeMoa = null,
            notes = session.notes
        )

        val impacts = session.impacts.map { impact ->
            ImpactEntity(
                sessionId = session.id,
                x = impact.position.x,
                y = impact.position.y,
                enabled = impact.enabled
            )
        }

        return dao.saveSessionWithImpacts(entity, impacts)
    }

    suspend fun deleteSession(id: Long) = dao.deleteSessionById(id)
}
