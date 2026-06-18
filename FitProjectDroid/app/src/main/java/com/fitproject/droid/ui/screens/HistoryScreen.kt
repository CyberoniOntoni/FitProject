package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPLoggedSet
import com.fitproject.droid.data.FPPersonalRecord
import com.fitproject.droid.data.FPWorkoutLog
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.components.PRBadge
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    workoutLogs: List<FPWorkoutLog>,
    personalRecords: List<FPPersonalRecord>,
    onLogTap: (FPWorkoutLog) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLog by remember { mutableStateOf<FPWorkoutLog?>(null) }

    if (selectedLog != null) {
        WorkoutLogDetailScreen(
            log = selectedLog!!,
            onBack = { selectedLog = null }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            com.fitproject.droid.ui.components.ScreenHeader(
                title = "History",
                subtitle = "Workouts and personal records",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (workoutLogs.isEmpty()) {
                HistoryEmptyState()
            } else {
                if (personalRecords.isNotEmpty()) {
                    PRPreviewSection(records = personalRecords)
                }
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    workoutLogs.forEach { log ->
                        HistoryLogCard(
                            log = log,
                            onClick = {
                                onLogTap(log)
                                selectedLog = log
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.History, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(48.dp))
        Text("No workout history", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
        Text(
            "Completed workouts sync automatically with FitPros.io",
            style = BWSTypography.Caption,
            color = BWSColors.TextSecondary
        )
    }
}

@Composable
private fun PRPreviewSection(records: List<FPPersonalRecord>) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            "Personal Records",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            records.take(10).forEach { record ->
                BWSCard(modifier = Modifier.width(140.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🏆", fontSize = 12.sp)
                            Text("PR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BWSColors.PrGold)
                        }
                        Text(
                            record.exerciseName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BWSColors.TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            "${record.value} ${record.metric}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BWSColors.Accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLogCard(
    log: FPWorkoutLog,
    onClick: () -> Unit
) {
    BWSCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    log.workoutName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    log.completedAt?.let { date ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CalendarMonth, null, tint = BWSColors.TextSecondary, modifier = Modifier.size(14.dp))
                            Text(formatShortDate(date), style = BWSTypography.Caption, color = BWSColors.TextSecondary)
                        }
                    }
                    log.durationSeconds?.let { duration ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AccessTime, null, tint = BWSColors.TextSecondary, modifier = Modifier.size(14.dp))
                            Text("${duration / 60} min", style = BWSTypography.Caption, color = BWSColors.TextSecondary)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.List, null, tint = BWSColors.TextSecondary, modifier = Modifier.size(14.dp))
                        Text("${log.exercises.size}", style = BWSTypography.Caption, color = BWSColors.TextSecondary)
                    }
                }
                if (log.prCount > 0) {
                    PRBadge()
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BWSColors.TextTertiary)
        }
    }
}

@Composable
fun WorkoutLogDetailScreen(
    log: FPWorkoutLog,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BWSColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "← Back",
            style = BWSTypography.Caption,
            color = BWSColors.Accent,
            modifier = Modifier.clickable(onClick = onBack)
        )

        Text(log.workoutName, style = BWSTypography.Headline, color = BWSColors.TextPrimary)

        log.completedAt?.let { date ->
            Text(formatFullDate(date), style = BWSTypography.Caption, color = BWSColors.TextSecondary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            log.durationSeconds?.let {
                DetailStat("Duration", "${it / 60} min")
            }
            DetailStat("Volume", "%.0f kg".format(log.totalVolume))
            if (log.prCount > 0) {
                DetailStat("PRs", "${log.prCount}")
            }
        }

        log.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
            BWSCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Notes", style = BWSTypography.Caption, color = BWSColors.TextSecondary)
                    Text(notes, color = BWSColors.TextPrimary)
                }
            }
        }

        log.exercises.forEach { exercise ->
            BWSCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        exercise.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BWSColors.TextPrimary
                    )
                    exercise.sets.filter { it.isCompleted }.forEach { set ->
                        SetRow(set = set)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BWSColors.Accent)
        Text(label, style = BWSTypography.Caption, color = BWSColors.TextSecondary)
    }
}

@Composable
private fun SetRow(set: FPLoggedSet) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Set ${set.setNumber}",
            style = BWSTypography.Caption,
            color = BWSColors.TextTertiary,
            modifier = Modifier.width(50.dp)
        )
        if (set.weight != null && set.reps != null) {
            Text("${set.weight} kg × ${set.reps}", style = BWSTypography.Metric, color = BWSColors.TextPrimary)
        }
        if (set.isPR) {
            Spacer(modifier = Modifier.width(8.dp))
            PRBadge()
        }
    }
}

private fun formatShortDate(date: java.util.Date): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(date)

private fun formatFullDate(date: java.util.Date): String =
    SimpleDateFormat("EEEE, MMMM d 'at' h:mm a", Locale.getDefault()).format(date)