package com.shotmetrics.app.domain.calculator

import com.shotmetrics.app.domain.model.ATZResult
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.HorizontalDirection
import com.shotmetrics.app.domain.model.VerticalDirection
import kotlin.math.abs

class ATZCalculator(private val unitConverter: UnitConverter) {

    /**
     * Calculate turret adjustments needed to zero the rifle.
     *
     * @param meanWindage horizontal offset of MPI from POA in the given angle unit
     *                    (positive = right, negative = left)
     * @param meanElevation vertical offset of MPI from POA in the given angle unit
     *                      (positive = up/high, negative = low)
     * @param angleUnit the unit of the windage/elevation values
     * @param turretClickValue the angular value of one turret click
     * @param turretAngleUnit the angular unit of the turret
     */
    fun calculate(
        meanWindage: Double,
        meanElevation: Double,
        angleUnit: AngleUnit,
        turretClickValue: Double,
        turretAngleUnit: AngleUnit
    ): ATZResult {
        if (turretClickValue <= 0) {
            return ATZResult()
        }

        val windageInTurretUnit = convertAngle(meanWindage, angleUnit, turretAngleUnit)
        val elevationInTurretUnit = convertAngle(meanElevation, angleUnit, turretAngleUnit)

        // To zero: we need to move POI toward POA, so we adjust opposite to offset
        val windageClicks = -windageInTurretUnit / turretClickValue
        val elevationClicks = -elevationInTurretUnit / turretClickValue

        val windageDirection = if (windageClicks >= 0) HorizontalDirection.RIGHT else HorizontalDirection.LEFT
        val elevationDirection = if (elevationClicks >= 0) VerticalDirection.UP else VerticalDirection.DOWN

        return ATZResult(
            windageClicks = abs(windageClicks),
            elevationClicks = abs(elevationClicks),
            windageDirection = windageDirection,
            elevationDirection = elevationDirection,
            windageAngle = abs(windageInTurretUnit),
            elevationAngle = abs(elevationInTurretUnit),
            angleUnit = turretAngleUnit
        )
    }

    private fun convertAngle(value: Double, from: AngleUnit, to: AngleUnit): Double {
        if (from == to) return value
        return when {
            from == AngleUnit.MOA && to == AngleUnit.MIL -> unitConverter.moaToMil(value)
            from == AngleUnit.MIL && to == AngleUnit.MOA -> unitConverter.milToMoa(value)
            else -> value
        }
    }
}
