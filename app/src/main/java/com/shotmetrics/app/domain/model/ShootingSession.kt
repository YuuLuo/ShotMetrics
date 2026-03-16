package com.shotmetrics.app.domain.model

import androidx.compose.ui.geometry.Offset

data class ShootingSession(
    val id: Long = 0,
    val imageUri: String = "",
    val scaleFactor: Double = 0.0,
    val distanceToTarget: Double = 100.0,
    val distanceUnit: DistanceUnit = DistanceUnit.YARDS,
    val caliber: String = "",
    val lengthUnit: LengthUnit = LengthUnit.INCH,
    val angleUnit: AngleUnit = AngleUnit.MOA,
    val referenceSize: Double = 1.0,
    val referencePoints: Pair<Offset, Offset>? = null,
    val pointOfAim: Offset? = null,
    val impacts: List<ImpactPoint> = emptyList(),
    val turretClickValue: Double = 0.25,
    val turretAngleUnit: AngleUnit = AngleUnit.MOA,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class ImpactPoint(
    val id: Int,
    val position: Offset,
    val enabled: Boolean = true
)
