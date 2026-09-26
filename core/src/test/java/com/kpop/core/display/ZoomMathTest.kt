package com.kpop.core.display

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomMathTest {
    @Test
    fun scaleIsRestrictedToSupportedRange() {
        assertEquals(1f, ZoomMath.clampScale(0.2f), 0f)
        assertEquals(3f, ZoomMath.clampScale(3f), 0f)
        assertEquals(5f, ZoomMath.clampScale(8f), 0f)
    }

    @Test
    fun translationIsDisabledAtOriginalScale() {
        assertEquals(0f, ZoomMath.clampTranslation(300f, 1080f, 1f), 0f)
    }

    @Test
    fun translationIsRestrictedToScaledViewport() {
        assertEquals(540f, ZoomMath.maximumTranslation(1080f, 2f), 0f)
        assertEquals(540f, ZoomMath.clampTranslation(900f, 1080f, 2f), 0f)
        assertEquals(-540f, ZoomMath.clampTranslation(-900f, 1080f, 2f), 0f)
    }
}
