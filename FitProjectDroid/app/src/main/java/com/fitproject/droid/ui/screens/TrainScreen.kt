package com.fitproject.droid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    userFirstName: String,
    onStartWorkout: (FPWorkout, FPProgram?) -> Unit,
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
            title = "Train",
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
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}