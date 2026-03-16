package com.shotmetrics.app.data.repository

import com.shotmetrics.app.data.preferences.UserPreferences
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.LengthUnit
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val prefs: UserPreferences) {

    val angleUnit: Flow<AngleUnit> = prefs.angleUnit
    val lengthUnit: Flow<LengthUnit> = prefs.lengthUnit
    val distanceUnit: Flow<DistanceUnit> = prefs.distanceUnit
    val referenceSize: Flow<Double> = prefs.referenceSize
    val turretClickValue: Flow<Double> = prefs.turretClickValue
    val turretAngleUnit: Flow<AngleUnit> = prefs.turretAngleUnit
    val defaultDistance: Flow<Double> = prefs.defaultDistance
    val defaultCaliber: Flow<String> = prefs.defaultCaliber
    val autoShowOverlays: Flow<Boolean> = prefs.autoShowOverlays

    suspend fun setAngleUnit(unit: AngleUnit) = prefs.setAngleUnit(unit)
    suspend fun setLengthUnit(unit: LengthUnit) = prefs.setLengthUnit(unit)
    suspend fun setDistanceUnit(unit: DistanceUnit) = prefs.setDistanceUnit(unit)
    suspend fun setReferenceSize(size: Double) = prefs.setReferenceSize(size)
    suspend fun setTurretClickValue(value: Double) = prefs.setTurretClickValue(value)
    suspend fun setTurretAngleUnit(unit: AngleUnit) = prefs.setTurretAngleUnit(unit)
    suspend fun setDefaultDistance(distance: Double) = prefs.setDefaultDistance(distance)
    suspend fun setDefaultCaliber(caliber: String) = prefs.setDefaultCaliber(caliber)
    suspend fun setAutoShowOverlays(enabled: Boolean) = prefs.setAutoShowOverlays(enabled)
}
