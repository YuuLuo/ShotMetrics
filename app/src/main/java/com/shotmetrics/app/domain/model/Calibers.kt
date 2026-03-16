package com.shotmetrics.app.domain.model

data class CaliberInfo(
    val name: String,
    val category: String,
    val bulletDiameterInch: Float
)

object Calibers {
    val ALL: List<CaliberInfo> = listOf(
        // Rimfire
        CaliberInfo(".22 LR", "Rimfire", 0.223f),
        CaliberInfo(".22 WMR", "Rimfire", 0.224f),
        CaliberInfo(".17 HMR", "Rimfire", 0.172f),

        // Pistol / Handgun
        CaliberInfo("9mm Luger", "Pistol", 0.355f),
        CaliberInfo(".380 ACP", "Pistol", 0.355f),
        CaliberInfo(".40 S&W", "Pistol", 0.400f),
        CaliberInfo(".45 ACP", "Pistol", 0.452f),
        CaliberInfo("10mm Auto", "Pistol", 0.400f),
        CaliberInfo(".357 Magnum", "Pistol", 0.357f),
        CaliberInfo(".44 Magnum", "Pistol", 0.429f),
        CaliberInfo(".357 SIG", "Pistol", 0.355f),

        // .22 Caliber Rifle
        CaliberInfo(".204 Ruger", "Rifle .22", 0.204f),
        CaliberInfo(".22 Hornet", "Rifle .22", 0.224f),
        CaliberInfo(".222 Remington", "Rifle .22", 0.224f),
        CaliberInfo(".223 Remington", "Rifle .22", 0.224f),
        CaliberInfo("5.56x45mm NATO", "Rifle .22", 0.224f),
        CaliberInfo(".224 Valkyrie", "Rifle .22", 0.224f),
        CaliberInfo(".22-250 Remington", "Rifle .22", 0.224f),
        CaliberInfo(".220 Swift", "Rifle .22", 0.224f),

        // 6mm Rifle
        CaliberInfo("6mm ARC", "Rifle 6mm", 0.243f),
        CaliberInfo("6mm Creedmoor", "Rifle 6mm", 0.243f),
        CaliberInfo("6mm BR Norma", "Rifle 6mm", 0.243f),
        CaliberInfo(".243 Winchester", "Rifle 6mm", 0.243f),
        CaliberInfo(".240 Weatherby Magnum", "Rifle 6mm", 0.243f),
        CaliberInfo("6x47 Lapua", "Rifle 6mm", 0.243f),
        CaliberInfo("6mm GT", "Rifle 6mm", 0.243f),
        CaliberInfo("6mm Dasher", "Rifle 6mm", 0.243f),

        // 6.5mm Rifle
        CaliberInfo("6.5mm Creedmoor", "Rifle 6.5mm", 0.264f),
        CaliberInfo("6.5mm PRC", "Rifle 6.5mm", 0.264f),
        CaliberInfo("6.5x47 Lapua", "Rifle 6.5mm", 0.264f),
        CaliberInfo("6.5x55 Swedish", "Rifle 6.5mm", 0.264f),
        CaliberInfo(".260 Remington", "Rifle 6.5mm", 0.264f),
        CaliberInfo("6.5-284 Norma", "Rifle 6.5mm", 0.264f),
        CaliberInfo("6.5 Grendel", "Rifle 6.5mm", 0.264f),

        // 7mm Rifle
        CaliberInfo("7mm Remington Magnum", "Rifle 7mm", 0.284f),
        CaliberInfo("7mm PRC", "Rifle 7mm", 0.284f),
        CaliberInfo("7mm-08 Remington", "Rifle 7mm", 0.284f),
        CaliberInfo(".280 Ackley Improved", "Rifle 7mm", 0.284f),
        CaliberInfo(".284 Winchester", "Rifle 7mm", 0.284f),
        CaliberInfo("28 Nosler", "Rifle 7mm", 0.284f),
        CaliberInfo("7mm SAUM", "Rifle 7mm", 0.284f),

        // .30 Caliber Rifle
        CaliberInfo(".308 Winchester", "Rifle .30", 0.308f),
        CaliberInfo("7.62x51mm NATO", "Rifle .30", 0.308f),
        CaliberInfo(".30-06 Springfield", "Rifle .30", 0.308f),
        CaliberInfo(".300 Winchester Magnum", "Rifle .30", 0.308f),
        CaliberInfo(".300 PRC", "Rifle .30", 0.308f),
        CaliberInfo(".300 Blackout", "Rifle .30", 0.308f),
        CaliberInfo(".300 WSM", "Rifle .30", 0.308f),
        CaliberInfo(".30-30 Winchester", "Rifle .30", 0.308f),
        CaliberInfo(".300 Norma Magnum", "Rifle .30", 0.308f),
        CaliberInfo(".300 Weatherby Magnum", "Rifle .30", 0.308f),
        CaliberInfo("7.62x39mm", "Rifle .30", 0.311f),
        CaliberInfo("7.62x54R", "Rifle .30", 0.312f),

        // Large / Magnum Rifle
        CaliberInfo(".338 Lapua Magnum", "Magnum", 0.338f),
        CaliberInfo(".338 Norma Magnum", "Magnum", 0.338f),
        CaliberInfo(".338 Federal", "Magnum", 0.338f),
        CaliberInfo(".375 H&H Magnum", "Magnum", 0.375f),
        CaliberInfo(".375 CheyTac", "Magnum", 0.375f),

        // ELR / Anti-Material
        CaliberInfo(".408 CheyTac", "ELR", 0.408f),
        CaliberInfo(".416 Barrett", "ELR", 0.416f),
        CaliberInfo(".50 BMG", "ELR", 0.510f),

        // Shotgun
        CaliberInfo("12 Gauge", "Shotgun", 0.729f),
        CaliberInfo("20 Gauge", "Shotgun", 0.615f),
        CaliberInfo("16 Gauge", "Shotgun", 0.662f),
        CaliberInfo(".410 Bore", "Shotgun", 0.410f),
    )

    val categories: List<String> = ALL.map { it.category }.distinct()

    fun search(query: String): List<CaliberInfo> {
        if (query.isBlank()) return ALL
        val q = query.lowercase().trim()
        return ALL.filter {
            it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    fun getDiameterInch(caliberName: String): Float {
        return ALL.find { it.name == caliberName }?.bulletDiameterInch ?: 0.308f
    }
}
