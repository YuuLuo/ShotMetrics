package com.shotmetrics.app.domain.model

enum class AngleUnit(val label: String, val abbreviation: String) {
    MOA("Minute of Angle", "MOA"),
    MIL("Milliradian", "MIL");
}

enum class LengthUnit(val label: String, val abbreviation: String) {
    INCH("Inch", "in"),
    CM("Centimeter", "cm");
}

enum class DistanceUnit(val label: String, val abbreviation: String) {
    YARDS("Yards", "yd"),
    METERS("Meters", "m");
}

data class TurretClickPreset(
    val name: String,
    val angleUnit: AngleUnit,
    val clickValue: Double
) {
    companion object {
        val PRESETS = listOf(
            TurretClickPreset("1/4 MOA", AngleUnit.MOA, 0.25),
            TurretClickPreset("1/8 MOA", AngleUnit.MOA, 0.125),
            TurretClickPreset("1/2 MOA", AngleUnit.MOA, 0.5),
            TurretClickPreset("1 MOA", AngleUnit.MOA, 1.0),
            TurretClickPreset("0.1 MIL", AngleUnit.MIL, 0.1),
            TurretClickPreset("0.05 MIL", AngleUnit.MIL, 0.05),
            TurretClickPreset("0.2 MIL", AngleUnit.MIL, 0.2),
        )
    }
}
