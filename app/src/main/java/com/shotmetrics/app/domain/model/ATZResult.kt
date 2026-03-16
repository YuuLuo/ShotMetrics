package com.shotmetrics.app.domain.model

import kotlin.math.roundToInt

data class ATZResult(
    val windageClicks: Double = 0.0,
    val elevationClicks: Double = 0.0,
    val windageDirection: HorizontalDirection = HorizontalDirection.RIGHT,
    val elevationDirection: VerticalDirection = VerticalDirection.UP,
    val windageAngle: Double = 0.0,
    val elevationAngle: Double = 0.0,
    val angleUnit: AngleUnit = AngleUnit.MOA
) {
    val windageClicksRounded: Int get() = windageClicks.roundToInt()
    val elevationClicksRounded: Int get() = elevationClicks.roundToInt()
}

enum class HorizontalDirection(val label: String, val arrow: String) {
    LEFT("Left", "L"),
    RIGHT("Right", "R");
}

enum class VerticalDirection(val label: String, val arrow: String) {
    UP("Up", "U"),
    DOWN("Down", "D");
}
