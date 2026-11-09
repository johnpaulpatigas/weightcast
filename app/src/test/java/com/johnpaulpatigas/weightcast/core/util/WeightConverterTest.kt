package com.johnpaulpatigas.weightcast.core.util

import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightConverterTest {

    @Test
    fun `kgToLbs converts correctly`() {
        val kg = 70.0
        val expectedLbs = 70.0 * 2.20462262185
        assertEquals(expectedLbs, WeightConverter.kgToLbs(kg), 0.0001)
    }

    @Test
    fun `lbsToKg converts correctly`() {
        val lbs = 154.3235835295
        val expectedKg = 70.0
        assertEquals(expectedKg, WeightConverter.lbsToKg(lbs), 0.0001)
    }

    @Test
    fun `convert from KG to LBS and back returns original value`() {
        val originalKg = 85.5
        val lbs = WeightConverter.convert(originalKg, WeightUnit.KG, WeightUnit.LBS)
        val convertedBack = WeightConverter.convert(lbs, WeightUnit.LBS, WeightUnit.KG)
        assertEquals(originalKg, convertedBack, 0.0001)
    }

    @Test
    fun `roundTo rounds to requested decimal places`() {
        val value = 75.6789
        assertEquals(75.7, WeightConverter.roundTo(value, 1), 0.0001)
        assertEquals(75.68, WeightConverter.roundTo(value, 2), 0.0001)
        assertEquals(76.0, WeightConverter.roundTo(value, 0), 0.0001)
    }

    @Test
    fun `formatDisplay formats canonical KG correctly for display unit`() {
        val canonicalKg = 70.0
        val formattedKg = WeightConverter.formatDisplay(canonicalKg, WeightUnit.KG, 1)
        val formattedLbs = WeightConverter.formatDisplay(canonicalKg, WeightUnit.LBS, 1)

        assertEquals("70.0 kg", formattedKg)
        assertEquals("154.3 lbs", formattedLbs)
    }
}
