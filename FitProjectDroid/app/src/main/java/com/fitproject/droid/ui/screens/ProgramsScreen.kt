package com.fitproject.droid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPProgram
import com.fitproject.droid.data.FPProgramWeek
import com.fitproject.droid.data.FPWorkout
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.components.ProgressRing
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography

@Composable
fun ProgramsScreen(
    programs: List<FPProgram>,
    programWeeks: Map<String, List<FPProgramWeek>>,
    onStartWorkout: (FPWorkout, FPProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.fitproject.droid.ui.components.ScreenHeader(
            title = "Programs",
            subtitle = "Assigned from FitPros"
        )

        if (programs.isEmpty()) {
            ProgramsEmptyState()
        } else {
            programs.forEach { program ->
                ExpandableProgramCard(
                    program = program,
                    weeks = programWeeks[program.id] ?: emptyList(),
                    onStartWorkout = { workout -> onStartWorkout(workout, program) }
                )
            }
        }
    }
}

@Composable
private fun ProgramsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ViewList,
            contentDescription = null,
            tint = BWSColors.TextTertiary,
            modifier = Modifier.size(48.dp)
        )
        Text("No programs yet", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
        Text(
            "Programs assigned on FitPros.io will appear here automatically.",
            style = BWSTypography.Caption,
            color = BWSColors.TextSecondary
        )
    }
}

@Composable
private fun ExpandableProgramCard(
    program: FPProgram,
    weeks: List<FPProgramWeek>,
    onStartWorkout: (FPWorkout) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedWeeks = remember(weeks) { weeks.sortedBy { it.index } }

    BWSCard(padding = 0.dp) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                if (!program.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = program.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(BWSColors.SurfaceHighlight)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, BWSColors.Surface)
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        program.title,
                        style = BWSTypography.Headline,
                        color = BWSColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = BWSColors.TextTertiary
                    )
                }

                if (program.description.isNotEmpty()) {
                    Text(
                        program.description,
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary,
                        maxLines = if (expanded) Int.MAX_VALUE else 2
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CalendarMonth, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(14.dp))
                        Text("${program.totalWeekCount} weeks", style = BWSTypography.Caption, color = BWSColors.TextTertiary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.FitnessCenter, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(14.dp))
                        Text("${program.totalWorkoutCount} workouts", style = BWSTypography.Caption, color = BWSColors.TextTertiary)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress = program.progress, size = 36.dp, lineWidth = 3.dp, showLabel = false)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        "${program.progressPercent}% complete",
                        style = BWSTypography.Caption,
                        color = BWSColors.Accent
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sortedWeeks.isEmpty()) {
                        Text(
                            "No weeks loaded yet.",
                            style = BWSTypography.Caption,
                            color = BWSColors.TextSecondary
                        )
                    } else {
                        sortedWeeks.forEach { week ->
                            WeekSection(
                                week = week,
                                onStartWorkout = onStartWorkout
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekSection(
    week: FPProgramWeek,
    onStartWorkout: (FPWorkout) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            week.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BWSColors.Accent
        )
        week.workouts.sortedBy { it.index }.forEach { workout ->
            WorkoutRow(workout = workout, onStart = { onStartWorkout(workout) })
        }
    }
}

@Composable
private fun WorkoutRow(
    workout: FPWorkout,
    onStart: () -> Unit
) {
    BWSCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    workout.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary
                )
                Text(
                    "${workout.exerciseCount} exercises",
                    style = BWSTypography.Caption,
                    color = BWSColors.TextSecondary
                )
            }
            Text(
                text = "Start",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BWSColors.Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BWSColors.Accent.copy(alpha = 0.15f))
                    .clickable(onClick = onStart)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}