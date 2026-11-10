package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightConverterTest {

    @Test
    fun `convert returns original value`() {
        val originalKg = 85.5
        val converted = WeightConverter.convert(originalKg, WeightUnit.KG, WeightUnit.KG)
        assertEquals(originalKg, converted, 0.0001)
    }

    @Test
    fun `roundTo rounds to requested decimal places`() {
        val value = 75.6789
        assertEquals(75.7, WeightConverter.roundTo(value, 1), 0.0001)
        assertEquals(75.68, WeightConverter.roundTo(value, 2), 0.0001)
        assertEquals(76.0, WeightConverter.roundTo(value, 0), 0.0001)
    }

    @Test
    fun `formatDisplay formats canonical KG correctly`() {
        val canonicalKg = 70.0
        val formattedKg = WeightConverter.formatDisplay(canonicalKg, WeightUnit.KG)

        assertEquals("70.00 kg", formattedKg)
    }
}
