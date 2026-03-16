package com.shotmetrics.app.domain.calculator

import androidx.compose.ui.geometry.Offset
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.BallisticsResult
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.LengthUnit
import kotlin.math.pow
import kotlin.math.sqrt

class BallisticsCalculator(private val unitConverter: UnitConverter) {

    /**
     * @param impacts list of impact positions in image pixel coordinates
     * @param pointOfAim POA position in image pixel coordinates (nullable)
     * @param scaleFactor pixels per reference unit length
     * @param distanceToTarget in the provided distance unit
     */
    fun calculate(
        impacts: List<Offset>,
        pointOfAim: Offset?,
        scaleFactor: Double,
        distanceToTarget: Double,
        distanceUnit: DistanceUnit,
        lengthUnit: LengthUnit,
        angleUnit: AngleUnit
    ): BallisticsResult {
        if (impacts.size < 2 || scaleFactor <= 0.0) {
            return BallisticsResult(impactCount = impacts.size)
        }

        val mpi = computeMPI(impacts)
        val extremeSpreadPx = computeExtremeSpread(impacts)
        val meanRadiusPx = computeMeanRadius(impacts, mpi)
        val cepPx = computeCEP(impacts, mpi)
        val radialSDPx = computeRadialSD(impacts, mpi)
        val verticalSDPx = computeVerticalSD(impacts)
        val horizontalSDPx = computeHorizontalSD(impacts)

        fun pxToLength(px: Double): Double = px / scaleFactor

        val extremeSpread = pxToLength(extremeSpreadPx)
        val meanRadius = pxToLength(meanRadiusPx)
        val cep = pxToLength(cepPx)
        val radialSD = pxToLength(radialSDPx)
        val verticalSD = pxToLength(verticalSDPx)
        val horizontalSD = pxToLength(horizontalSDPx)

        val windagePx = if (pointOfAim != null) (mpi.x - pointOfAim.x).toDouble() else 0.0
        val elevationPx = if (pointOfAim != null) (pointOfAim.y - mpi.y).toDouble() else 0.0

        val windageLength = pxToLength(windagePx)
        val elevationLength = pxToLength(elevationPx)

        val meanWindage = unitConverter.lengthToAngle(
            windageLength, lengthUnit, distanceToTarget, distanceUnit, angleUnit
        )
        val meanElevation = unitConverter.lengthToAngle(
            elevationLength, lengthUnit, distanceToTarget, distanceUnit, angleUnit
        )

        val groupSizeMoa = unitConverter.lengthToAngle(
            extremeSpread, lengthUnit, distanceToTarget, distanceUnit, AngleUnit.MOA
        )
        val groupSizeMil = unitConverter.lengthToAngle(
            extremeSpread, lengthUnit, distanceToTarget, distanceUnit, AngleUnit.MIL
        )

        return BallisticsResult(
            extremeSpread = extremeSpread,
            meanPointOfImpact = mpi,
            meanRadius = meanRadius,
            cep = cep,
            radialSD = radialSD,
            verticalSD = verticalSD,
            horizontalSD = horizontalSD,
            meanWindage = meanWindage,
            meanElevation = meanElevation,
            groupSizeMoa = groupSizeMoa,
            groupSizeMil = groupSizeMil,
            impactCount = impacts.size
        )
    }

    private fun computeMPI(impacts: List<Offset>): Offset {
        val avgX = impacts.map { it.x }.average()
        val avgY = impacts.map { it.y }.average()
        return Offset(avgX.toFloat(), avgY.toFloat())
    }

    private fun computeExtremeSpread(impacts: List<Offset>): Double {
        var maxDist = 0.0
        for (i in impacts.indices) {
            for (j in i + 1 until impacts.size) {
                val dist = distance(impacts[i], impacts[j])
                if (dist > maxDist) maxDist = dist
            }
        }
        return maxDist
    }

    private fun computeMeanRadius(impacts: List<Offset>, mpi: Offset): Double {
        return impacts.map { distance(it, mpi) }.average()
    }

    /**
     * CEP: radius of the smallest circle centered at MPI containing 50% of shots.
     * We sort the distances from MPI and take the median.
     */
    private fun computeCEP(impacts: List<Offset>, mpi: Offset): Double {
        val distances = impacts.map { distance(it, mpi) }.sorted()
        val halfIndex = (impacts.size * 0.5).toInt().coerceIn(0, impacts.size - 1)
        return distances[halfIndex]
    }

    private fun computeRadialSD(impacts: List<Offset>, mpi: Offset): Double {
        val distances = impacts.map { distance(it, mpi) }
        return standardDeviation(distances)
    }

    private fun computeVerticalSD(impacts: List<Offset>): Double {
        val yValues = impacts.map { it.y.toDouble() }
        return standardDeviation(yValues)
    }

    private fun computeHorizontalSD(impacts: List<Offset>): Double {
        val xValues = impacts.map { it.x.toDouble() }
        return standardDeviation(xValues)
    }

    private fun distance(a: Offset, b: Offset): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean).pow(2) } / (values.size - 1)
        return sqrt(variance)
    }
}
