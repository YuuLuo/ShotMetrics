package com.shotmetrics.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val createdAt: Long,
    val caliber: String,
    val distance: Double,
    val distanceUnit: String,
    val scaleFactor: Double,
    val referenceSize: Double,
    val refX1: Float? = null,
    val refY1: Float? = null,
    val refX2: Float? = null,
    val refY2: Float? = null,
    val poaX: Float? = null,
    val poaY: Float? = null,
    val groupSizeMoa: Double? = null,
    val notes: String? = null
)
