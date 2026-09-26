package com.kpop.core.display

import kotlin.math.max

object ZoomMath {
    fun clampScale(value: Float, minimum: Float = 1f, maximum: Float = 5f): Float {
        require(minimum > 0f) { "minimum must be positive" }
        require(maximum >= minimum) { "maximum must be greater than or equal to minimum" }
        return value.coerceIn(minimum, maximum)
    }

    fun maximumTranslation(viewportSizePx: Float, scale: Float): Float {
        if (viewportSizePx <= 0f || scale <= 1f) return 0f
        return max(0f, viewportSizePx * (scale - 1f) / 2f)
    }

    fun clampTranslation(value: Float, viewportSizePx: Float, scale: Float): Float {
        val limit = maximumTranslation(viewportSizePx, scale)
        return value.coerceIn(-limit, limit)
    }
}
