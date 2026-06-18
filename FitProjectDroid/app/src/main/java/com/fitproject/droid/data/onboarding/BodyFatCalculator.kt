package com.fitproject.droid.data.onboarding

import com.fitproject.droid.data.onboarding.Gender.FEMALE
import com.fitproject.droid.data.onboarding.Gender.MALE
import kotlin.math.log10

/**
 * U.S. Navy body fat estimation (public domain formula).
 * Measurements in centimeters; converted internally to inches.
 */
object BodyFatCalculator {
    fun navyBodyFatPercent(
        gender: Gender,
        heightCm: Double,
        neckCm: Double,
        waistCm: Double,
        hipCm: Double? = null
    ): Double? {
        if (heightCm <= 0 || neckCm <= 0 || waistCm <= 0) return null

        val heightIn = heightCm / 2.54
        val neckIn = neckCm / 2.54
        val waistIn = waistCm / 2.54

        val percent = when (gender) {
            MALE -> {
                val abdomen = waistIn - neckIn
                if (abdomen <= 0) return null
                86.010 * log10(abdomen) - 70.041 * log10(heightIn) + 36.76
            }
            FEMALE -> {
                val hipsIn = (hipCm ?: return null) / 2.54
                val composite = waistIn + hipsIn - neckIn
                if (composite <= 0) return null
                163.205 * log10(composite) - 97.684 * log10(heightIn) - 78.387
            }
            else -> {
                val male = navyBodyFatPercent(MALE, heightCm, neckCm, waistCm)
                val female = hipCm?.let { navyBodyFatPercent(FEMALE, heightCm, neckCm, waistCm, it) }
                when {
                    male != null && female != null -> (male + female) / 2
                    male != null -> male
                    else -> female
                }
            }
        }

        return percent?.coerceIn(3.0, 60.0)
    }
}