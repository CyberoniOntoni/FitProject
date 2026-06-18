package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitproject.droid.data.FPForm
import com.fitproject.droid.data.FPWorkout
import com.fitproject.droid.data.onboarding.BodyFatMethod
import com.fitproject.droid.data.onboarding.BodyFocus
import com.fitproject.droid.data.onboarding.Equipment
import com.fitproject.droid.data.onboarding.ExperienceLevel
import com.fitproject.droid.data.onboarding.FitnessGoal
import com.fitproject.droid.data.onboarding.Gender
import com.fitproject.droid.data.onboarding.GymType
import com.fitproject.droid.data.onboarding.MusclePriority
import com.fitproject.droid.data.onboarding.OnboardingProfile
import com.fitproject.droid.data.onboarding.OnboardingStep
import com.fitproject.droid.data.onboarding.Weekday
import com.fitproject.droid.data.onboarding.WorkoutSplit
import com.fitproject.droid.ui.theme.AppleBodyFatGauge
import com.fitproject.droid.ui.theme.AppleChip
import com.fitproject.droid.ui.theme.AppleColors
import com.fitproject.droid.ui.theme.AppleNumericField
import com.fitproject.droid.ui.theme.ApplePrimaryButton
import com.fitproject.droid.ui.theme.AppleProgressBar
import com.fitproject.droid.ui.theme.AppleSecondaryButton
import com.fitproject.droid.ui.theme.AppleSelectionCard
import com.fitproject.droid.ui.theme.AppleStepHeader
import com.fitproject.droid.ui.theme.AppleTextField
import com.fitproject.droid.ui.theme.AppleTypography
import com.fitproject.droid.viewmodel.OnboardingViewModel
import java.util.Date

@Composable
fun OnboardingWizardScreen(
    viewModel: OnboardingViewModel,
    userId: String?,
    forms: List<FPForm>,
    harvestedWorkouts: List<FPWorkout>,
    onComplete: () -> Unit,
    onSubmitForm: suspend (FPForm, List<com.fitproject.droid.data.FPFormAnswer>) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val plan by viewModel.generatedPlan.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val completed by viewModel.completed.collectAsStateWithLifecycle()

    if (completed) {
        onComplete()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppleColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step != OnboardingStep.WELCOME) {
                IconButton(onClick = viewModel::goBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppleColors.Accent)
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            AppleProgressBar(
                progress = viewModel.progress,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            TextButton(onClick = viewModel::skip) {
                Text("Skip", style = AppleTypography.Subhead, color = AppleColors.SecondaryLabel)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(profile, viewModel::updateProfile)
                OnboardingStep.GOAL -> GoalStep(profile, viewModel::updateProfile)
                OnboardingStep.GENDER -> GenderStep(profile, viewModel::updateProfile)
                OnboardingStep.DATE_OF_BIRTH -> DobStep(profile, viewModel::updateProfile)
                OnboardingStep.HEIGHT_WEIGHT -> HeightWeightStep(profile, viewModel::updateProfile)
                OnboardingStep.BODY_FAT_METHOD -> BodyFatMethodStep(profile, viewModel::updateProfile)
                OnboardingStep.BODY_FAT_PHOTO -> BodyFatPhotoStep(profile, viewModel::updateProfile)
                OnboardingStep.BODY_FAT_MEASURE -> BodyFatMeasureStep(profile, viewModel::updateProfile)
                OnboardingStep.BODY_FAT_RESULT -> BodyFatResultStep(profile)
                OnboardingStep.MUSCLE_GOAL -> MuscleGoalStep(profile, viewModel::updateProfile)
                OnboardingStep.EXPERIENCE -> ExperienceStep(profile, viewModel::updateProfile)
                OnboardingStep.AVAILABLE_DAYS -> AvailableDaysStep(profile, viewModel::updateProfile)
                OnboardingStep.DAYS_PER_WEEK -> DaysPerWeekStep(profile, viewModel::updateProfile)
                OnboardingStep.DURATION -> DurationStep(profile, viewModel::updateProfile)
                OnboardingStep.MUSCLE_PRIORITY -> MusclePriorityStep(profile, viewModel::updateProfile)
                OnboardingStep.BODY_FOCUS -> BodyFocusStep(profile, viewModel::updateProfile)
                OnboardingStep.WORKOUT_SPLIT -> WorkoutSplitStep(profile, viewModel::updateProfile)
                OnboardingStep.GYM_TYPE -> GymTypeStep(profile, viewModel::updateProfile)
                OnboardingStep.EQUIPMENT -> EquipmentStep(profile, viewModel::updateProfile)
                OnboardingStep.GENERATE_PLAN -> GeneratePlanStep(
                    profile = profile,
                    plan = plan,
                    isGenerating = isGenerating,
                    onGenerate = { viewModel.generatePlan(harvestedWorkouts) },
                    onFinish = { viewModel.finishOnboarding(userId, forms, onSubmitForm) }
                )
            }

            error?.let {
                Text(it, style = AppleTypography.Footnote, color = AppleColors.Destructive)
            }
        }

        Box(modifier = Modifier.padding(24.dp)) {
            if (step == OnboardingStep.GENERATE_PLAN) {
                if (plan != null) {
                    ApplePrimaryButton(title = "Start training", onClick = {
                        viewModel.finishOnboarding(userId, forms, onSubmitForm)
                    })
                } else {
                    ApplePrimaryButton(
                        title = "Generate my plan",
                        onClick = { viewModel.generatePlan(harvestedWorkouts) },
                        isLoading = isGenerating
                    )
                }
            } else {
                ApplePrimaryButton(title = "Continue", onClick = viewModel::goNext)
            }
        }
    }
}

@Composable
private fun WelcomeStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(
        title = "Welcome",
        subtitle = "Let's personalize your training — Built With Science style, refined for you."
    )
    AppleTextField(
        value = profile.firstName,
        onValueChange = { v -> update { firstName = v } },
        placeholder = "Your first name"
    )
}

@Composable
private fun GoalStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Your goal", subtitle = "What are you training for?")
    FitnessGoal.entries.forEach { option ->
        AppleSelectionCard(
            title = option.label,
            selected = profile.goal == option,
            onClick = { update { goal = option } }
        )
    }
}

@Composable
private fun GenderStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Gender", subtitle = "Used for body fat formulas and programming.")
    Gender.entries.forEach { option ->
        AppleSelectionCard(
            title = option.label,
            selected = profile.gender == option,
            onClick = { update { gender = option } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState()
    AppleStepHeader(title = "Date of birth", subtitle = "Helps tailor recovery and volume.")

    val label = profile.dateOfBirth?.toString() ?: "Tap to select"
    AppleSelectionCard(
        title = label,
        subtitle = "Required",
        selected = profile.dateOfBirth != null,
        onClick = { showPicker = true }
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        update { dateOfBirth = Date(ms) }
                    }
                    showPicker = false
                }) { Text("Done", color = AppleColors.Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun HeightWeightStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    var heightText by remember {
        mutableStateOf(profile.heightCm?.let { formatWholeNumber(it) } ?: "")
    }
    var weightText by remember {
        mutableStateOf(profile.weightKg?.let { formatDecimalInput(it) } ?: "")
    }

    AppleStepHeader(title = "Height & weight", subtitle = "Metric units for accurate calculations.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppleNumericField(
            value = heightText,
            onValueChange = { v ->
                heightText = v.filter { it.isDigit() || it == '.' || it == ',' }
                update { heightCm = heightText.replace(",", ".").toDoubleOrNull() }
            },
            label = "Height",
            suffix = "cm",
            modifier = Modifier.weight(1f)
        )
        AppleNumericField(
            value = weightText,
            onValueChange = { v ->
                weightText = v.filter { it.isDigit() || it == '.' || it == ',' }
                update { weightKg = weightText.replace(",", ".").toDoubleOrNull() }
            },
            label = "Weight",
            suffix = "kg",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BodyFatMethodStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Body fat", subtitle = "Estimate visually or calculate with measurements.")
    AppleSelectionCard(
        title = "Visual estimate",
        subtitle = "Use a progress photo reference + slider",
        selected = profile.bodyFatMethod == BodyFatMethod.VISUAL_PHOTO,
        onClick = { update { bodyFatMethod = BodyFatMethod.VISUAL_PHOTO } },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(4.dp))
    AppleSelectionCard(
        title = "Calculate",
        subtitle = "Neck, waist & hip (U.S. Navy formula)",
        selected = profile.bodyFatMethod == BodyFatMethod.MEASUREMENT,
        onClick = { update { bodyFatMethod = BodyFatMethod.MEASUREMENT } }
    )
}

@Composable
private fun BodyFatPhotoStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    LaunchedEffect(Unit) {
        if (profile.bodyFatPercent == null) update { bodyFatPercent = 18.0 }
    }
    val estimate = profile.bodyFatPercent ?: 18.0
    AppleStepHeader(
        title = "Visual estimate",
        subtitle = "Take or upload a photo, then adjust the slider to match your best estimate."
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(AppleColors.Fill, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CameraAlt, null, tint = AppleColors.SecondaryLabel)
            Text("Photo capture coming soon", style = AppleTypography.Caption, color = AppleColors.SecondaryLabel)
        }
    }
    AppleBodyFatGauge(estimate)
    Slider(
        value = estimate.toFloat(),
        onValueChange = { v -> update { bodyFatPercent = v.toDouble() } },
        valueRange = 5f..45f,
        colors = SliderDefaults.colors(thumbColor = AppleColors.Accent, activeTrackColor = AppleColors.Accent)
    )
    Text(
        "Slide to match your visual estimate",
        style = AppleTypography.Caption,
        color = AppleColors.SecondaryLabel
    )
}

@Composable
private fun BodyFatMeasureStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    var neckText by remember {
        mutableStateOf(profile.neckCm?.let { formatDecimalInput(it) } ?: "")
    }
    var waistText by remember {
        mutableStateOf(profile.waistCm?.let { formatDecimalInput(it) } ?: "")
    }
    var hipText by remember {
        mutableStateOf(profile.hipCm?.let { formatDecimalInput(it) } ?: "")
    }

    AppleStepHeader(
        title = "Measurements",
        subtitle = "Measure in cm at the narrowest neck, navel-level waist${if (profile.gender == Gender.FEMALE) ", and hips" else ""}."
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        AppleNumericField(
            value = neckText,
            onValueChange = { v ->
                neckText = v.filter { it.isDigit() || it == '.' || it == ',' }
                update { neckCm = neckText.replace(",", ".").toDoubleOrNull() }
            },
            label = "Neck",
            suffix = "cm",
            modifier = Modifier.weight(1f)
        )
        AppleNumericField(
            value = waistText,
            onValueChange = { v ->
                waistText = v.filter { it.isDigit() || it == '.' || it == ',' }
                update { waistCm = waistText.replace(",", ".").toDoubleOrNull() }
            },
            label = "Waist",
            suffix = "cm",
            modifier = Modifier.weight(1f)
        )
    }
    if (profile.gender == Gender.FEMALE) {
        AppleNumericField(
            value = hipText,
            onValueChange = { v ->
                hipText = v.filter { it.isDigit() || it == '.' || it == ',' }
                update { hipCm = hipText.replace(",", ".").toDoubleOrNull() }
            },
            label = "Hips",
            suffix = "cm"
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Calculate, null, tint = AppleColors.Accent)
        Text("Navy formula calculates % on the next screen", style = AppleTypography.Footnote, color = AppleColors.SecondaryLabel)
    }
}

@Composable
private fun BodyFatResultStep(profile: OnboardingProfile) {
    AppleStepHeader(title = "Your body fat", subtitle = "Based on your selection.")
    profile.bodyFatPercent?.let { AppleBodyFatGauge(it) }
}

@Composable
private fun MuscleGoalStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Muscle goal", subtitle = "How much muscle do you want to gain or lose? (kg)")
    val value = profile.muscleGoalKg.toFloat()
    Text(
        when {
            value > 0 -> "Gain ${"%.1f".format(value)} kg muscle"
            value < 0 -> "Lose ${"%.1f".format(-value)} kg"
            else -> "Maintain"
        },
        style = AppleTypography.Title,
        color = AppleColors.Label
    )
    Slider(
        value = value,
        onValueChange = { v -> update { muscleGoalKg = v.toDouble() } },
        valueRange = -5f..8f,
        steps = 25,
        colors = SliderDefaults.colors(thumbColor = AppleColors.Accent, activeTrackColor = AppleColors.Accent)
    )
}

@Composable
private fun ExperienceStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Experience", subtitle = "Honest selection scales volume per NCSF guidelines.")
    ExperienceLevel.entries.forEach { option ->
        AppleSelectionCard(
            title = option.label,
            subtitle = "${option.setsRange.first}–${option.setsRange.last} sets · ${option.repRange.first}–${option.repRange.last} reps",
            selected = profile.experience == option,
            onClick = { update { experience = option } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvailableDaysStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Available days", subtitle = "Which days can you train?")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Weekday.entries.forEach { option ->
            AppleChip(
                label = option.label,
                selected = option in profile.availableDays,
                onClick = {
                    update {
                        availableDays = if (option in availableDays) availableDays - option else availableDays + option
                    }
                }
            )
        }
    }
}

@Composable
private fun DaysPerWeekStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    val maxDays = profile.availableDays.size.coerceAtLeast(2)
    AppleStepHeader(title = "Days per week", subtitle = "How many of those days will you train?")
    Text("${profile.daysPerWeek} days", style = AppleTypography.Title, color = AppleColors.Accent)
    Slider(
        value = profile.daysPerWeek.toFloat(),
        onValueChange = { v -> update { daysPerWeek = v.toInt().coerceIn(2, maxDays) } },
        valueRange = 2f..maxDays.toFloat().coerceAtLeast(2f),
        steps = (maxDays - 3).coerceAtLeast(0),
        colors = SliderDefaults.colors(thumbColor = AppleColors.Accent, activeTrackColor = AppleColors.Accent)
    )
}

@Composable
private fun DurationStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Session length", subtitle = "Includes warm-up and cool-down.")
    Text("${profile.sessionMinutes} minutes", style = AppleTypography.Title, color = AppleColors.Accent)
    Slider(
        value = profile.sessionMinutes.toFloat(),
        onValueChange = { v -> update { sessionMinutes = v.toInt() } },
        valueRange = 30f..90f,
        steps = 5,
        colors = SliderDefaults.colors(thumbColor = AppleColors.Accent, activeTrackColor = AppleColors.Accent)
    )
}

@Composable
private fun MusclePriorityStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Priority", subtitle = "Emphasize a muscle group or keep balance.")
    MusclePriority.entries.forEach { option ->
        AppleSelectionCard(
            title = option.label,
            selected = profile.musclePriority == option,
            onClick = { update { musclePriority = option } }
        )
    }
}

@Composable
private fun BodyFocusStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Body focus", subtitle = "Full body or upper body only?")
    BodyFocus.entries.forEach { option ->
        AppleSelectionCard(
            title = option.label,
            selected = profile.bodyFocus == option,
            onClick = { update { bodyFocus = option } }
        )
    }
}

@Composable
private fun WorkoutSplitStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Workout split", subtitle = "Choose how sessions are organized.")
    WorkoutSplit.entries.forEach { option ->
        AppleSelectionCard(
            title = option.label,
            subtitle = option.description,
            selected = profile.workoutSplit == option,
            onClick = { update { workoutSplit = option } }
        )
    }
}

@Composable
private fun GymTypeStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Your gym", subtitle = "We'll filter exercises from FitPros data.")
    GymType.entries.forEach { option ->
        AppleSelectionCard(
            title = option.label,
            selected = profile.gymType == option,
            onClick = {
                update {
                    gymType = option
                    if (option == GymType.BODYWEIGHT) equipment = setOf(Equipment.BODYWEIGHT)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentStep(profile: OnboardingProfile, update: (OnboardingProfile.() -> Unit) -> Unit) {
    AppleStepHeader(title = "Equipment", subtitle = "Select everything you have access to.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Equipment.entries.forEach { option ->
            AppleChip(
                label = option.label,
                selected = option in profile.equipment,
                onClick = {
                    update {
                        equipment = if (option in equipment) equipment - option else equipment + option
                    }
                }
            )
        }
    }
}

@Composable
private fun GeneratePlanStep(
    profile: OnboardingProfile,
    plan: com.fitproject.droid.data.onboarding.GeneratedExercisePlan?,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onFinish: () -> Unit
) {
    AppleStepHeader(
        title = if (plan != null) "Your plan is ready" else "Generate plan",
        subtitle = if (plan != null) plan.summary else "NCSF-based programming with warm-up & cool-down each session."
    )

    if (plan != null) {
        plan.workouts.forEach { day ->
            AppleSelectionCard(
                title = day.dayLabel,
                subtitle = "${day.focus} · ${day.exercises.size} exercises · ~${day.estimatedMinutes} min",
                selected = false,
                onClick = onFinish
            )
        }
        Text(
            "Exercises sourced from FitPros library where available",
            style = AppleTypography.Caption,
            color = AppleColors.TertiaryLabel
        )
    } else if (!isGenerating) {
        AppleSecondaryButton(title = "Preview profile", onClick = { })
        Text(
            "Hi ${profile.firstName} — ${profile.goal?.label ?: "ready"} · ${profile.daysPerWeek}x/week",
            style = AppleTypography.Body,
            color = AppleColors.SecondaryLabel
        )
    }
}

private fun formatWholeNumber(value: Double): String =
    if (value % 1.0 == 0.0) "%.0f".format(value) else value.toString()

private fun formatDecimalInput(value: Double): String {
    val rounded = "%.1f".format(value)
    return if (rounded.endsWith(".0")) rounded.dropLast(2) else rounded
}