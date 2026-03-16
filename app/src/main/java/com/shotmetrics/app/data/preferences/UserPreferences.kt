package com.shotmetrics.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.LengthUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    private object Keys {
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val LENGTH_UNIT = stringPreferencesKey("length_unit")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val REFERENCE_SIZE = doublePreferencesKey("reference_size")
        val TURRET_CLICK_VALUE = doublePreferencesKey("turret_click_value")
        val TURRET_ANGLE_UNIT = stringPreferencesKey("turret_angle_unit")
        val DEFAULT_DISTANCE = doublePreferencesKey("default_distance")
        val DEFAULT_CALIBER = stringPreferencesKey("default_caliber")
        val AUTO_SHOW_OVERLAYS = booleanPreferencesKey("auto_show_overlays")
    }

    val angleUnit: Flow<AngleUnit> = context.dataStore.data.map { prefs ->
        prefs[Keys.ANGLE_UNIT]?.let { AngleUnit.valueOf(it) } ?: AngleUnit.MOA
    }

    val lengthUnit: Flow<LengthUnit> = context.dataStore.data.map { prefs ->
        prefs[Keys.LENGTH_UNIT]?.let { LengthUnit.valueOf(it) } ?: LengthUnit.INCH
    }

    val distanceUnit: Flow<DistanceUnit> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISTANCE_UNIT]?.let { DistanceUnit.valueOf(it) } ?: DistanceUnit.YARDS
    }

    val referenceSize: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[Keys.REFERENCE_SIZE] ?: 1.0
    }

    val turretClickValue: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[Keys.TURRET_CLICK_VALUE] ?: 0.25
    }

    val turretAngleUnit: Flow<AngleUnit> = context.dataStore.data.map { prefs ->
        prefs[Keys.TURRET_ANGLE_UNIT]?.let { AngleUnit.valueOf(it) } ?: AngleUnit.MOA
    }

    val defaultDistance: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_DISTANCE] ?: 100.0
    }

    val defaultCaliber: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_CALIBER] ?: ""
    }

    val autoShowOverlays: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_SHOW_OVERLAYS] ?: true
    }

    suspend fun setAngleUnit(unit: AngleUnit) {
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit.name }
    }

    suspend fun setLengthUnit(unit: LengthUnit) {
        context.dataStore.edit { it[Keys.LENGTH_UNIT] = unit.name }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun setReferenceSize(size: Double) {
        context.dataStore.edit { it[Keys.REFERENCE_SIZE] = size }
    }

    suspend fun setTurretClickValue(value: Double) {
        context.dataStore.edit { it[Keys.TURRET_CLICK_VALUE] = value }
    }

    suspend fun setTurretAngleUnit(unit: AngleUnit) {
        context.dataStore.edit { it[Keys.TURRET_ANGLE_UNIT] = unit.name }
    }

    suspend fun setDefaultDistance(distance: Double) {
        context.dataStore.edit { it[Keys.DEFAULT_DISTANCE] = distance }
    }

    suspend fun setDefaultCaliber(caliber: String) {
        context.dataStore.edit { it[Keys.DEFAULT_CALIBER] = caliber }
    }

    suspend fun setAutoShowOverlays(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SHOW_OVERLAYS] = enabled }
    }
}
