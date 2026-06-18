package com.fitproject.droid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPLoggedSet
import com.fitproject.droid.data.FPWorkoutExercise
import com.fitproject.droid.data.FPWorkoutMetric
import com.fitproject.droid.data.WorkoutSessionState
import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.components.PRBadge
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import com.fitproject.droid.ui.theme.metricColor
import com.fitproject.droid.viewmodel.AppViewModel
import com.fitproject.droid.viewmodel.WorkoutSessionViewModel

@Composable
fun WorkoutSessionScreen(
    session: WorkoutSessionState,
    appViewModel: AppViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember(session) {
        WorkoutSessionViewModel(session, appViewModel)
    }

    val currentExerciseIndex by viewModel.currentExerciseIndex.collectAsStateWithLifecycle()
    val loggedSets by viewModel.loggedSets.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val restSeconds by viewModel.restSecondsRemaining.collectAsStateWithLifecycle()
    val restActive by viewModel.restTimerActive.collectAsStateWithLifecycle()
    val showPR by viewModel.showPRToast.collectAsStateWithLifecycle()
    val latestPR by viewModel.latestPR.collectAsStateWithLifecycle()
    val showDialog by viewModel.showCompleteDialog.collectAsStateWithLifecycle()
    val isFinishing by viewModel.isFinishing.collectAsStateWithLifecycle()

    val currentExercise = viewModel.currentExercise
    val metrics = currentExercise?.let { exercise ->
        session.workout.metrics
            .filter { it.workoutExerciseId == exercise.id }
            .sortedBy { it.index }
    } ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BWSColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SessionHeader(
                workoutName = session.workout.name,
                elapsed = viewModel.formatElapsed(elapsed),
                onDismiss = onDismiss,
                onFinish = { viewModel.requestFinish() }
            )

            ExerciseProgressBar(
                progress = viewModel.progress,
                modifier = Modifier.fillMaxWidth()
            )

            currentExercise?.let { exercise ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ExerciseHeader(
                        exercise = exercise,
                        currentIndex = currentExerciseIndex,
                        totalCount = viewModel.exerciseCount
                    )

                    SetsSection(
                        exercise = exercise,
                        metrics = metrics,
                        sets = loggedSets[exercise.id] ?: emptyList(),
                        onUpdateSet = { index, set -> viewModel.updateSet(exercise.id, index, set) },
                        onToggleComplete = { index -> viewModel.toggleSetComplete(exercise, index) },
                        onAddSet = { viewModel.addSet(exercise) }
                    )

                    NotesSection(
                        notes = notes,
                        onNotesChange = viewModel::updateNotes
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            BottomBar(
                canGoBack = currentExerciseIndex > 0,
                canGoForward = currentExerciseIndex < viewModel.exerciseCount - 1,
                isLastExercise = currentExerciseIndex >= viewModel.exerciseCount - 1,
                onPrevious = viewModel::previousExercise,
                onNext = viewModel::nextExercise,
                onComplete = { viewModel.requestFinish() }
            )
        }

        AnimatedVisibility(
            visible = restActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RestTimerOverlay(
                secondsRemaining = restSeconds,
                formattedTime = viewModel.formatRest(restSeconds),
                onSkip = viewModel::skipRestTimer,
                onAdd30 = { viewModel.addRestTime(30) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 100.dp)
            )
        }

        AnimatedVisibility(
            visible = showPR,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier.padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PRBadge()
                latestPR?.let {
                    Text(
                        "${it.exerciseName} — ${it.value} kg",
                        style = BWSTypography.Caption,
                        color = BWSColors.TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCompleteDialog() },
                title = { Text("Complete Workout?") },
                text = { Text("Your workout will sync to FitPros.io") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.finishWorkout(onDismiss) },
                        enabled = !isFinishing
                    ) { Text("Complete") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCompleteDialog() }) {
                        Text("Cancel")
                    }
                },
                containerColor = BWSColors.SurfaceElevated,
                titleContentColor = BWSColors.TextPrimary,
                textContentColor = BWSColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SessionHeader(
    workoutName: String,
    elapsed: String,
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BWSColors.Surface)
        ) {
            Icon(Icons.Default.Close, null, tint = BWSColors.TextSecondary, modifier = Modifier.size(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(workoutName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BWSColors.TextPrimary)
            Text(elapsed, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = BWSColors.TextSecondary)
        }

        Text(
            "Finish",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.Accent,
            modifier = Modifier.clickable(onClick = onFinish)
        )
    }
}

@Composable
private fun ExerciseProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(3.dp)
            .background(BWSColors.SurfaceHighlight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BWSColors.Accent, BWSColors.AccentSecondary)
                    )
                )
        )
    }
}

@Composable
private fun ExerciseHeader(
    exercise: FPWorkoutExercise,
    currentIndex: Int,
    totalCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (exercise.headerVisible && !exercise.header.isNullOrEmpty()) {
            Text(
                exercise.header!!.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BWSColors.Accent,
                letterSpacing = 1.5.sp
            )
        }

        exercise.videoThumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Text(exercise.name, style = BWSTypography.Headline, color = BWSColors.TextPrimary)

        exercise.coachNotes?.takeIf { it.isNotEmpty() }?.let { notes ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BWSColors.Surface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("\"", fontSize = 12.sp, color = BWSColors.Accent)
                Text(notes, style = BWSTypography.Caption, color = BWSColors.TextSecondary)
            }
        }

        Text(
            "Exercise ${currentIndex + 1} of $totalCount",
            style = BWSTypography.Caption,
            color = BWSColors.TextTertiary
        )
    }
}

@Composable
private fun SetsSection(
    exercise: FPWorkoutExercise,
    metrics: List<FPWorkoutMetric>,
    sets: List<FPLoggedSet>,
    onUpdateSet: (Int, FPLoggedSet) -> Unit,
    onToggleComplete: (Int) -> Unit,
    onAddSet: () -> Unit
) {
    val displayMetrics = if (metrics.isEmpty()) defaultMetrics() else metrics

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SET",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BWSColors.TextTertiary,
                modifier = Modifier.width(28.dp)
            )
            displayMetrics.forEach { metric ->
                Text(
                    metric.name.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BWSColors.TextTertiary,
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.width(36.dp))
        }

        sets.forEachIndexed { index, set ->
            SetRow(
                setNumber = index + 1,
                set = set,
                metrics = displayMetrics,
                onSetChange = { onUpdateSet(index, it) },
                onToggleComplete = { onToggleComplete(index) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BWSColors.Accent.copy(alpha = 0.1f))
                .clickable(onClick = onAddSet)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, null, tint = BWSColors.Accent, modifier = Modifier.size(18.dp))
            Text(
                "  Add Set",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BWSColors.Accent
            )
        }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    set: FPLoggedSet,
    metrics: List<FPWorkoutMetric>,
    onSetChange: (FPLoggedSet) -> Unit,
    onToggleComplete: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = BWSColors.TextPrimary,
        unfocusedTextColor = BWSColors.TextPrimary,
        focusedContainerColor = BWSColors.Surface,
        unfocusedContainerColor = BWSColors.Surface,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        cursorColor = BWSColors.Accent
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (set.isCompleted) BWSColors.Accent.copy(alpha = 0.08f) else BWSColors.Surface)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$setNumber",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (set.isCompleted) BWSColors.Accent else BWSColors.TextSecondary,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        metrics.forEach { metric ->
            val value = getMetricValue(set, metric.name)
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    onSetChange(updateMetricValue(set, metric.name, newValue))
                },
                modifier = Modifier
                    .width(56.dp)
                    .padding(horizontal = 2.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = metricColor(metric.name)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (set.isCompleted) BWSColors.Accent else BWSColors.SurfaceHighlight
                )
                .clickable(onClick = onToggleComplete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Complete set",
                tint = if (set.isCompleted) Color.White else BWSColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NotesSection(notes: String, onNotesChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Notes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BWSColors.TextSecondary)
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add a note...", color = BWSColors.TextTertiary) },
            minLines = 3,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BWSColors.TextPrimary,
                unfocusedTextColor = BWSColors.TextPrimary,
                focusedContainerColor = BWSColors.Surface,
                unfocusedContainerColor = BWSColors.Surface,
                focusedBorderColor = BWSColors.SurfaceHighlight,
                unfocusedBorderColor = BWSColors.SurfaceHighlight,
                cursorColor = BWSColors.Accent
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun BottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLastExercise: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BWSColors.SurfaceElevated)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canGoBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BWSColors.Surface)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                null,
                tint = if (canGoBack) BWSColors.TextPrimary else BWSColors.TextTertiary
            )
        }

        BWSPrimaryButton(
            title = if (isLastExercise) "Complete Workout" else "Next Exercise",
            onClick = if (isLastExercise) onComplete else onNext,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onNext,
            enabled = canGoForward,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BWSColors.Surface)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = if (canGoForward) BWSColors.TextPrimary else BWSColors.TextTertiary
            )
        }
    }
}

@Composable
private fun RestTimerOverlay(
    secondsRemaining: Int,
    formattedTime: String,
    onSkip: () -> Unit,
    onAdd30: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BWSColors.SurfaceElevated)
            .border(1.dp, BWSColors.Accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Rest Timer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BWSColors.TextSecondary)
        Text(
            formattedTime,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = BWSColors.Accent
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "+30s",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BWSColors.Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BWSColors.Accent.copy(alpha = 0.15f))
                    .clickable(onClick = onAdd30)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                "Skip",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BWSColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BWSColors.SurfaceHighlight)
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

private fun defaultMetrics(): List<FPWorkoutMetric> = listOf(
    FPWorkoutMetric(id = "reps", name = "Reps", index = 0),
    FPWorkoutMetric(id = "weight", name = "Weight", index = 1)
)

private fun getMetricValue(set: FPLoggedSet, metricName: String): String = when (metricName) {
    "Reps" -> set.reps ?: ""
    "Weight" -> set.weight ?: ""
    "RPE" -> set.rpe ?: ""
    "Rest" -> set.rest ?: ""
    "Tempo" -> set.tempo ?: ""
    "Time" -> set.time ?: ""
    else -> ""
}

private fun updateMetricValue(set: FPLoggedSet, metricName: String, value: String): FPLoggedSet = when (metricName) {
    "Reps" -> set.copy(reps = value.ifEmpty { null })
    "Weight" -> set.copy(weight = value.ifEmpty { null })
    "RPE" -> set.copy(rpe = value.ifEmpty { null })
    "Rest" -> set.copy(rest = value.ifEmpty { null })
    "Tempo" -> set.copy(tempo = value.ifEmpty { null })
    "Time" -> set.copy(time = value.ifEmpty { null })
    else -> set
}