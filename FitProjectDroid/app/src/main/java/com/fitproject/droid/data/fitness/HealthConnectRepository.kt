package com.fitproject.droid.data.fitness

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

class HealthConnectRepository(private val context: Context) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    fun isAvailable(): Boolean = sdkStatus() == HealthConnectClient.SDK_AVAILABLE

    private fun clientOrNull(): HealthConnectClient? =
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    suspend fun readTodayMetrics(distanceUnit: String): DailyActivityMetrics {
        val goalMeters = DailyActivityMetrics.distanceGoalMeters(distanceUnit)

        if (!isAvailable()) {
            val message = when (sdkStatus()) {
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    "Update Health Connect to sync activity data"
                else -> "Install Health Connect to sync steps and distance from Google Fit"
            }
            return DailyActivityMetrics(
                distanceGoalMeters = goalMeters,
                needsInstall = true,
                errorMessage = message
            )
        }

        if (!hasPermissions()) {
            return DailyActivityMetrics(
                distanceGoalMeters = goalMeters,
                needsPermission = true
            )
        }

        return runCatching {
            val client = clientOrNull() ?: error("Health Connect unavailable")
            val startTime = startOfTodayInstant()
            val endTime = Instant.now()
            val filter = TimeRangeFilter.between(startTime, endTime)

            val stepsResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = filter
                )
            )
            val distanceResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = filter
                )
            )

            val steps = stepsResponse[StepsRecord.COUNT_TOTAL] ?: 0L
            val distanceMeters = distanceResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0

            DailyActivityMetrics(
                steps = steps,
                distanceMeters = distanceMeters,
                distanceGoalMeters = goalMeters,
                isConnected = true,
                lastSyncedAt = System.currentTimeMillis()
            )
        }.getOrElse { error ->
            DailyActivityMetrics(
                distanceGoalMeters = goalMeters,
                isConnected = false,
                errorMessage = error.message ?: "Could not read activity data"
            )
        }
    }

    companion object {
        fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    }

    private fun startOfTodayInstant(): Instant {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
            .let { Instant.ofEpochMilli(it) }
            .atZone(ZoneId.systemDefault())
            .toInstant()
    }
}