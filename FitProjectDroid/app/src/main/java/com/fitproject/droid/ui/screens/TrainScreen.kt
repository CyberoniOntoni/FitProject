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
import androidx.compose.foundation.layout.width
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
import com.fitproject.droid.data.FPProgram
import com.fitproject.droid.data.FPWorkout
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.components.ProgressRing
import com.fitproject.droid.ui.components.ScreenHeader
import com.fitproject.droid.ui.components.WeekProgressDots
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.util.Calendar

@Composable
fun TrainScreen(
    isSyncing: Boolean,
    weeklyCompleted: Int,
    weeklyGoal: Int,
    nextWorkout: FPWorkout?,
    nextProgram: FPProgram?,
    habits: List<FPHabit>,
    userFirstName: String,
    onStartWorkout: (FPWorkout, FPProgram?) -> Unit,
    onUpdateHabit: (FPHabit, Double) -> Unit,
    onSeeAllHabits: () -> Unit = {},
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

        BWSCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("THIS WEEK", style = BWSTypography.RingLabel, color = BWSColors.TextSecondary)
                    Text(
                        "$weeklyCompleted",
                        style = BWSTypography.Metric,
                        color = BWSColors.TextPrimary
                    )
                    Text(
                        "of $weeklyGoal workouts",
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    WeekProgressDots(completed = weeklyCompleted, total = weeklyGoal)
                }
                ProgressRing(
                    progress = weeklyCompleted.toDouble() / maxOf(weeklyGoal, 1),
                    size = 72.dp,
                    lineWidth = 6.dp
                )
            }
        }

        if (isSyncing) {
            Text("Syncing...", style = BWSTypography.Caption, color = BWSColors.Accent)
        }

        BWSCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Next Workout", style = BWSTypography.Caption, color = BWSColors.TextSecondary)
                if (nextWorkout != null) {
                    Text(nextWorkout.name, style = BWSTypography.Headline, color = BWSColors.TextPrimary)
                    nextProgram?.let {
                        Text(it.title, style = BWSTypography.Caption, color = BWSColors.TextTertiary)
                    }
                    Text(
                        "${nextWorkout.exerciseCount} exercises",
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary
                    )
                    BWSPrimaryButton(
                        title = "Start Workout",
                        onClick = { onStartWorkout(nextWorkout, nextProgram) }
                    )
                } else {
                    Text(
                        "All workouts complete!",
                        style = BWSTypography.Body,
                        color = BWSColors.TextSecondary
                    )
                }
            }
        }

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