package com.kpop.feature.upscale

import org.junit.Assert.assertEquals
import org.junit.Test

class FourKTargetTest {
    @Test
    fun landscapeImageFitsUhdPanel() {
        assertEquals(PixelSize(3_840, 2_160), FourKTarget.sizeFor(1_920, 1_080))
    }

    @Test
    fun portraitPhotoCardFitsRotatedUhdPanel() {
        assertEquals(PixelSize(3_072, 3_840), FourKTarget.sizeFor(800, 1_000))
    }

    @Test
    fun squareImagePreservesItsAspectRatio() {
        assertEquals(PixelSize(3_840, 3_840), FourKTarget.sizeFor(600, 600))
    }
}
