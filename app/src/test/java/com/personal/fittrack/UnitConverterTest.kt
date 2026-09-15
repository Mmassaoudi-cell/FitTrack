package com.personal.fittrack

import com.personal.fittrack.domain.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `kg to lb conversion is accurate`() {
        assertEquals(176.37, UnitConverter.round2(UnitConverter.kgToLb(80.0)), 0.01)
    }

    @Test
    fun `lb to kg conversion is accurate`() {
        assertEquals(80.0, UnitConverter.round2(UnitConverter.lbToKg(176.37)), 0.01)
    }

    @Test
    fun `round trip conversion returns to the original value`() {
        val original = 82.5
        val roundTripped = UnitConverter.round2(UnitConverter.lbToKg(UnitConverter.kgToLb(original)))
        assertEquals(original, roundTripped, 0.01)
    }
}
