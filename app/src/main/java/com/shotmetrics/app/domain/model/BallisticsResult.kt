package com.shotmetrics.app.domain.model

import androidx.compose.ui.geometry.Offset

data class BallisticsResult(
    val extremeSpread: Double = 0.0,
    val meanPointOfImpact: Offset = Offset.Zero,
    val meanRadius: Double = 0.0,
    val cep: Double = 0.0,
    val radialSD: Double = 0.0,
    val verticalSD: Double = 0.0,
    val horizontalSD: Double = 0.0,
    val meanWindage: Double = 0.0,
    val meanElevation: Double = 0.0,
    val groupSizeMoa: Double = 0.0,
    val groupSizeMil: Double = 0.0,
    val impactCount: Int = 0
) {
    val isValid: Boolean get() = impactCount >= 2
}
