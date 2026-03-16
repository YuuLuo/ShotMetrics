package com.shotmetrics.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shotmetrics.app.data.repository.SettingsRepository
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.LengthUnit
import com.shotmetrics.app.domain.model.TurretClickPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val angleUnit = settingsRepo.angleUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AngleUnit.MOA)

    val lengthUnit = settingsRepo.lengthUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LengthUnit.INCH)

    val distanceUnit = settingsRepo.distanceUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DistanceUnit.YARDS)

    val referenceSize = settingsRepo.referenceSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val turretClickValue = settingsRepo.turretClickValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.25)

    val turretAngleUnit = settingsRepo.turretAngleUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AngleUnit.MOA)

    val defaultDistance = settingsRepo.defaultDistance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100.0)

    val defaultCaliber = settingsRepo.defaultCaliber
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val autoShowOverlays = settingsRepo.autoShowOverlays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val turretPresets = TurretClickPreset.PRESETS

    fun setAngleUnit(unit: AngleUnit) = viewModelScope.launch { settingsRepo.setAngleUnit(unit) }
    fun setLengthUnit(unit: LengthUnit) = viewModelScope.launch { settingsRepo.setLengthUnit(unit) }
    fun setDistanceUnit(unit: DistanceUnit) = viewModelScope.launch { settingsRepo.setDistanceUnit(unit) }
    fun setReferenceSize(size: Double) = viewModelScope.launch { settingsRepo.setReferenceSize(size) }
    fun setTurretClickValue(value: Double) = viewModelScope.launch { settingsRepo.setTurretClickValue(value) }
    fun setTurretAngleUnit(unit: AngleUnit) = viewModelScope.launch { settingsRepo.setTurretAngleUnit(unit) }
    fun setDefaultDistance(distance: Double) = viewModelScope.launch { settingsRepo.setDefaultDistance(distance) }
    fun setDefaultCaliber(caliber: String) = viewModelScope.launch { settingsRepo.setDefaultCaliber(caliber) }
    fun setAutoShowOverlays(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoShowOverlays(enabled) }

    fun applyTurretPreset(preset: TurretClickPreset) {
        viewModelScope.launch {
            settingsRepo.setTurretClickValue(preset.clickValue)
            settingsRepo.setTurretAngleUnit(preset.angleUnit)
        }
    }
}
