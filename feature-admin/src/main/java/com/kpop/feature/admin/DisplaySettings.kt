package com.kpop.feature.admin

data class DisplaySettings(
    val slideIntervalMillis: Long = 8_000L,
    val maximumZoom: Float = 5f,
) {
    init {
        require(slideIntervalMillis >= 1_000L)
        require(maximumZoom >= 1f)
    }
}
