package com.fitproject.droid.data.fitness

data class DailyActivityMetrics(
    val steps: Long = 0,
    val distanceMeters: Double = 0.0,
    val stepGoal: Long = 10_000,
    val distanceGoalMeters: Double = 8046.72,
    val isConnected: Boolean = false,
    val needsPermission: Boolean = false,
    val needsInstall: Boolean = false,
    val isLoading: Boolean = false,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null
) {
    val stepsProgress: Double
        get() = if (stepGoal <= 0) 0.0 else (steps.toDouble() / stepGoal).coerceIn(0.0, 1.0)

    val distanceProgress: Double
        get() = if (distanceGoalMeters <= 0) 0.0 else (distanceMeters / distanceGoalMeters).coerceIn(0.0, 1.0)

    companion object {
        fun distanceGoalMeters(unit: String): Double = when (unit) {
            "KILOMETER" -> 8000.0
            else -> 8046.72
        }
    }
}