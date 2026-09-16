package com.personal.fittrack.domain

object InputValidation {
    fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    fun positive(value: Double) = value.isFinite() && value > 0.0
    fun nonNegative(value: Double) = value.isFinite() && value >= 0.0
    fun food(name: String, grams: Double, vararg nutrients: Double) {
        require(name.isNotBlank()) { "Enter a food name." }
        require(positive(grams)) { "Enter a portion greater than zero." }
        require(nutrients.all(::nonNegative)) { "Nutrition values must be zero or greater." }
    }
}

fun WeightUnit.display(kg: Double): Double = if (this == WeightUnit.LB) UnitConverter.kgToLb(kg) else kg
fun WeightUnit.toKg(value: Double): Double = if (this == WeightUnit.LB) UnitConverter.lbToKg(value) else value
fun WeightUnit.format(kg: Double): String = "%.1f %s".format(display(kg), name.lowercase())
