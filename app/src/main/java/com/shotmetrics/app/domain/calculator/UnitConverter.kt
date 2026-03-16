package com.shotmetrics.app.domain.calculator

import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.LengthUnit

class UnitConverter {

    companion object {
        const val MOA_TO_INCH_AT_100YD = 1.047
        const val MIL_TO_CM_AT_100M = 10.0
        const val MIL_TO_INCH_AT_100YD = 3.6
        const val INCH_TO_CM = 2.54
        const val YARD_TO_METER = 0.9144
        const val MOA_PER_MIL = 3.4377
    }

    fun lengthToInches(value: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.INCH -> value
        LengthUnit.CM -> value / INCH_TO_CM
    }

    fun inchesToLength(inches: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.INCH -> inches
        LengthUnit.CM -> inches * INCH_TO_CM
    }

    fun distanceToYards(value: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.YARDS -> value
        DistanceUnit.METERS -> value / YARD_TO_METER
    }

    fun distanceToMeters(value: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.YARDS -> value * YARD_TO_METER
        DistanceUnit.METERS -> value
    }

    fun angleToMoa(value: Double, unit: AngleUnit): Double = when (unit) {
        AngleUnit.MOA -> value
        AngleUnit.MIL -> value * MOA_PER_MIL
    }

    fun angleToMil(value: Double, unit: AngleUnit): Double = when (unit) {
        AngleUnit.MOA -> value / MOA_PER_MIL
        AngleUnit.MIL -> value
    }

    fun moaToMil(moa: Double): Double = moa / MOA_PER_MIL
    fun milToMoa(mil: Double): Double = mil * MOA_PER_MIL

    /**
     * Convert a physical offset (in the configured length unit) to an angular measurement.
     * @param offsetLength offset in the active length unit
     * @param distanceToTarget distance in the active distance unit
     */
    fun lengthToAngle(
        offsetLength: Double,
        lengthUnit: LengthUnit,
        distanceToTarget: Double,
        distanceUnit: DistanceUnit,
        targetAngleUnit: AngleUnit
    ): Double {
        val offsetInches = lengthToInches(offsetLength, lengthUnit)
        val distYards = distanceToYards(distanceToTarget, distanceUnit)
        if (distYards <= 0) return 0.0

        val moa = offsetInches / (MOA_TO_INCH_AT_100YD * distYards / 100.0)
        return when (targetAngleUnit) {
            AngleUnit.MOA -> moa
            AngleUnit.MIL -> moaToMil(moa)
        }
    }

    /**
     * Convert an angular measurement to a physical offset.
     */
    fun angleToLength(
        angle: Double,
        angleUnit: AngleUnit,
        distanceToTarget: Double,
        distanceUnit: DistanceUnit,
        targetLengthUnit: LengthUnit
    ): Double {
        val moa = angleToMoa(angle, angleUnit)
        val distYards = distanceToYards(distanceToTarget, distanceUnit)

        val offsetInches = moa * MOA_TO_INCH_AT_100YD * distYards / 100.0
        return inchesToLength(offsetInches, targetLengthUnit)
    }
}
