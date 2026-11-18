package com.yusufcanmercan.weight_track_app.ui.home

enum class BmiCategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

/** [value] = kg / (boy m)²; [category] standart eşiklerle (18,5 / 25 / 30). */
data class Bmi(val value: Double, val category: BmiCategory)

/** Boy ve kilodan BMI. Android'e bağlı değil, yalnız hesap. Geçersiz girdide (≤ 0) `null`. */
fun calculateBmi(weightKg: Double, heightCm: Double): Bmi? {
    if (weightKg <= 0.0 || heightCm <= 0.0) return null
    val meters = heightCm / 100.0
    val value = weightKg / (meters * meters)
    val category = when {
        value < 18.5 -> BmiCategory.UNDERWEIGHT
        value < 25.0 -> BmiCategory.NORMAL
        value < 30.0 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }
    return Bmi(value, category)
}
