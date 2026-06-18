package com.fitproject.droid.data.fitness

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GoogleFitRepository(private val context: Context) {

    val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    fun getAccount() = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

    fun hasPermissions(): Boolean =
        GoogleSignIn.hasPermissions(getAccount(), fitnessOptions)

    suspend fun readTodayMetrics(distanceUnit: String): DailyActivityMetrics {
        val goalMeters = DailyActivityMetrics.distanceGoalMeters(distanceUnit)
        if (!hasPermissions()) {
            return DailyActivityMetrics(
                distanceGoalMeters = goalMeters,
                needsPermission = true
            )
        }

        return runCatching {
            val startOfDay = startOfTodayMillis()
            val now = System.currentTimeMillis()

            val steps = readAggregateTotal(
                dataType = DataType.AGGREGATE_STEP_COUNT_DELTA,
                field = Field.FIELD_STEPS,
                startMillis = startOfDay,
                endMillis = now
            )
            val distance = readAggregateTotal(
                dataType = DataType.AGGREGATE_DISTANCE_DELTA,
                field = Field.FIELD_DISTANCE,
                startMillis = startOfDay,
                endMillis = now
            )

            DailyActivityMetrics(
                steps = steps.toLong(),
                distanceMeters = distance,
                distanceGoalMeters = goalMeters,
                isConnected = true,
                lastSyncedAt = now
            )
        }.getOrElse { error ->
            DailyActivityMetrics(
                distanceGoalMeters = goalMeters,
                isConnected = false,
                errorMessage = error.message ?: "Could not read Google Fit data"
            )
        }
    }

    private suspend fun readAggregateTotal(
        dataType: DataType,
        field: Field,
        startMillis: Long,
        endMillis: Long
    ): Double {
        val request = DataReadRequest.Builder()
            .aggregate(dataType)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startMillis, endMillis, TimeUnit.MILLISECONDS)
            .build()

        val response = Fitness.getHistoryClient(context, getAccount())
            .readData(request)
            .await()

        var total = 0.0
        for (bucket in response.buckets) {
            for (dataSet in bucket.dataSets) {
                for (dataPoint in dataSet.dataPoints) {
                    total += dataPoint.getValue(field).asFloat().toDouble()
                }
            }
        }
        return total
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}