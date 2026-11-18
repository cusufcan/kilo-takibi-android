package com.yusufcanmercan.weight_track_app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BmiTest {
    @Test
    fun `degeri kg bolu boy metre kare olarak hesaplar`() {
        assertEquals(24.22, calculateBmi(70.0, 170.0)!!.value, 0.01)
    }

    @Test
    fun `esik degerler dogru kategoriye girer`() {
        // 100 cm boyda BMI = kg / 1; sınırlar doğrudan görünür.
        assertEquals(BmiCategory.UNDERWEIGHT, calculateBmi(18.4, 100.0)!!.category)
        assertEquals(BmiCategory.NORMAL, calculateBmi(18.5, 100.0)!!.category)
        assertEquals(BmiCategory.NORMAL, calculateBmi(24.9, 100.0)!!.category)
        assertEquals(BmiCategory.OVERWEIGHT, calculateBmi(25.0, 100.0)!!.category)
        assertEquals(BmiCategory.OVERWEIGHT, calculateBmi(29.9, 100.0)!!.category)
        assertEquals(BmiCategory.OBESE, calculateBmi(30.0, 100.0)!!.category)
    }

    @Test
    fun `gecersiz girdide null doner`() {
        assertNull(calculateBmi(70.0, 0.0))
        assertNull(calculateBmi(0.0, 170.0))
    }

    @Test
    fun `buildHomeState boy varsa bmi uretir yoksa uretmez`() {
        val weights = listOf(com.yusufcanmercan.weight_track_app.data.local.Weight(1, 70.0, 1))
        val with = buildHomeState(weights, heightCm = 170.0)
        assertEquals(true, with.hasHeight)
        assertEquals(24.22, with.bmi!!.value, 0.01)
        val without = buildHomeState(weights)
        assertNull(without.bmi)
        assertEquals(false, without.hasHeight)
    }
}
