package com.personal.fittrack.domain

object UnitConverter {
    private const val KG_PER_LB = 0.45359237

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB
    fun lbToKg(lb: Double): Double = lb * KG_PER_LB

    /** Rounds to the nearest 0.01 to avoid floating point noise after repeated conversions. */
    fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}

enum class WeightUnit { KG, LB }
