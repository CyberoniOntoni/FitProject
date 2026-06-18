package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPHabit
import com.fitproject.droid.data.fitness.DailyActivityMetrics
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.components.MetricProgressRing
import com.fitproject.droid.ui.components.ScreenHeader
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
@Composable
fun SummaryScreen(
    activityMetrics: DailyActivityMetrics,
    distanceUnit: String,
    habits: List<FPHabit>,
    userFirstName: String,
    onRefreshActivity: () -> Unit,
    onConnectActivity: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onUpdateHabit: (FPHabit, Double) -> Unit,
    onSeeAllHabits: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Summary",
            subtitle = "$greeting, ${userFirstName.ifEmpty { "Athlete" }}"
        )

        ActivityMetricsSection(
            metrics = activityMetrics,
            distanceUnit = distanceUnit,
            onRefresh = onRefreshActivity,
            onConnectActivity = onConnectActivity,
            onInstallHealthConnect = onInstallHealthConnect
        )

        if (habits.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Habits",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BWSColors.TextPrimary
                    )
                    Text(
                        text = "See all",
                        style = BWSTypography.Caption,
                        color = BWSColors.Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onSeeAllHabits)
                            .padding(4.dp)
                    )
                }
                habits.take(3).forEach { habit ->
                    HabitQuickCard(
                        habit = habit,
                        onUpdate = { onUpdateHabit(habit, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricsSection(
    metrics: DailyActivityMetrics,
    distanceUnit: String,
    onRefresh: () -> Unit,
    onConnectActivity: () -> Unit,
    onInstallHealthConnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TODAY",
                style = BWSTypography.RingLabel,
                color = BWSColors.TextSecondary
            )
            if (metrics.isConnected) {
                Text(
                    "Health Connect",
                    style = BWSTypography.Caption,
                    color = BWSColors.Success
                )
            }
        }

        if (metrics.needsInstall) {
            BWSCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Install Health Connect",
                        style = BWSTypography.Headline,
                        color = BWSColors.TextPrimary
                    )
                    Text(
                        metrics.errorMessage
                            ?: "Install Health Connect to sync steps and distance. Connect Google Fit inside Health Connect to include Fit data.",
                        style = BWSTypography.Body,
                        color = BWSColors.TextSecondary
                    )
                    BWSPrimaryButton(
                        title = "Get Health Connect",
                        onClick = onInstallHealthConnect
                    )
                }
            }
        } else if (metrics.needsPermission) {
            BWSCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Connect Activity Data",
                        style = BWSTypography.Headline,
                        color = BWSColors.TextPrimary
                    )
                    Text(
                        "Allow access to steps and distance through Health Connect. If Google Fit is linked in Health Connect, your Fit activity syncs automatically.",
                        style = BWSTypography.Body,
                        color = BWSColors.TextSecondary
                    )
                    BWSPrimaryButton(
                        title = "Connect Health Connect",
                        onClick = onConnectActivity
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActivityMetricCard(
                    label = "Steps",
                    value = formatSteps(metrics.steps),
                    goalText = "Goal ${formatSteps(metrics.stepGoal)}",
                    progress = metrics.stepsProgress,
                    ringColor = BWSColors.RingMove,
                    modifier = Modifier.weight(1f)
                )
                ActivityMetricCard(
                    label = "Distance",
                    value = formatDistance(metrics.distanceMeters, distanceUnit),
                    goalText = "Goal ${formatDistance(metrics.distanceGoalMeters, distanceUnit)}",
                    progress = metrics.distanceProgress,
                    ringColor = BWSColors.RingExercise,
                    modifier = Modifier.weight(1f)
                )
            }

            metrics.errorMessage?.let { message ->
                Text(message, style = BWSTypography.Caption, color = BWSColors.Warning)
            }

            if (metrics.isLoading) {
                Text("Syncing activity...", style = BWSTypography.Caption, color = BWSColors.Accent)
            }
        }
    }
}

@Composable
private fun ActivityMetricCard(
    label: String,
    value: String,
    goalText: String,
    progress: Double,
    ringColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    BWSCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MetricProgressRing(
                progress = progress,
                color = ringColor,
                size = 72.dp,
                lineWidth = 7.dp
            )
            Text(label.uppercase(), style = BWSTypography.RingLabel, color = BWSColors.TextSecondary)
            Text(value, style = BWSTypography.Metric, color = BWSColors.TextPrimary)
            Text(goalText, style = BWSTypography.Caption, color = BWSColors.TextTertiary)
        }
    }
}

@Composable
private fun HabitQuickCard(
    habit: FPHabit,
    onUpdate: (Double) -> Unit
) {
    BWSCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    habit.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (habit.isComplete) {
                    Text(
                        "On target",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BWSColors.Success
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BWSColors.SurfaceHighlight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(habit.progress.toFloat())
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BWSColors.Accent)
                )
            }

            Text(
                text = if (habit.isComplete) "✓ Complete" else habit.progressText,
                style = BWSTypography.Caption,
                color = if (habit.isComplete) BWSColors.Success else BWSColors.TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onUpdate(maxOf(0.0, habit.currentValue - 1)) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BWSColors.SurfaceHighlight)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = BWSColors.TextPrimary)
                }

                Text(
                    text = formatHabitValue(habit.currentValue),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BWSColors.TextPrimary
                )

                IconButton(
                    onClick = { onUpdate(habit.currentValue + 1) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BWSColors.SurfaceHighlight)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = BWSColors.TextPrimary)
                }
            }
        }
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private fun formatHabitValue(value: Double): String =
    if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)

private fun formatSteps(steps: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(steps)

private fun formatDistance(meters: Double, unit: String): String {
    return when (unit) {
        "KILOMETER" -> {
            val km = meters / 1000.0
            if (km < 10) "%.2f km".format(km) else "%.1f km".format(km)
        }
        else -> {
            val miles = meters / 1609.344
            if (miles < 10) "%.2f mi".format(miles) else "%.1f mi".format(miles)
        }
    }
}