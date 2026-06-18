package com.fitproject.droid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitproject.droid.data.FPForm
import com.fitproject.droid.data.FPWorkout
import com.fitproject.droid.data.FirestoreService
import com.fitproject.droid.data.onboarding.BodyFatCalculator
import com.fitproject.droid.data.onboarding.BodyFatMethod
import com.fitproject.droid.data.onboarding.ExerciseCatalog
import com.fitproject.droid.data.onboarding.GeneratedExercisePlan
import com.fitproject.droid.data.onboarding.OnboardingFormSubmitter
import com.fitproject.droid.data.onboarding.OnboardingPlanConverter
import com.fitproject.droid.data.onboarding.OnboardingPlanGenerator
import com.fitproject.droid.data.onboarding.OnboardingProfile
import com.fitproject.droid.data.onboarding.OnboardingRepository
import com.fitproject.droid.data.onboarding.OnboardingStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OnboardingRepository(application)

    private val _step = MutableStateFlow(OnboardingStep.WELCOME)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _profile = MutableStateFlow(OnboardingProfile())
    val profile: StateFlow<OnboardingProfile> = _profile.asStateFlow()

    private val _generatedPlan = MutableStateFlow<GeneratedExercisePlan?>(null)
    val generatedPlan: StateFlow<GeneratedExercisePlan?> = _generatedPlan.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    val progress: Float
        get() {
            val steps = stepSequence(_profile.value)
            val index = steps.indexOf(_step.value).coerceAtLeast(0)
            return (index + 1).toFloat() / steps.size.coerceAtLeast(1)
        }

    init {
        viewModelScope.launch {
            _profile.value = repository.loadProfile()
            _generatedPlan.value = repository.loadGeneratedPlan()
        }
    }

    fun updateProfile(block: OnboardingProfile.() -> Unit) {
        _profile.update { it.apply(block) }
    }

    fun goBack() {
        val steps = stepSequence(_profile.value)
        val index = steps.indexOf(_step.value)
        if (index > 0) _step.value = steps[index - 1]
    }

    fun goNext() {
        if (!canProceed(_step.value, _profile.value)) {
            _error.value = validationMessage(_step.value)
            return
        }
        _error.value = null
        persistDraft()

        when (_step.value) {
            OnboardingStep.BODY_FAT_MEASURE -> calculateBodyFatFromMeasurements()
            OnboardingStep.BODY_FAT_PHOTO -> { /* slider already sets percent */ }
            else -> Unit
        }

        val steps = stepSequence(_profile.value)
        val index = steps.indexOf(_step.value)
        if (index < steps.lastIndex) {
            _step.value = steps[index + 1]
        }
    }

    fun skip() {
        viewModelScope.launch {
            repository.markComplete(skipped = true)
            _completed.value = true
        }
    }

    fun generatePlan(harvestedWorkouts: List<FPWorkout>) {
        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            try {
                val profile = _profile.value
                val harvested = ExerciseCatalog.harvestFromWorkouts(harvestedWorkouts)
                val catalog = ExerciseCatalog.availableExercises(profile, harvested)
                val plan = OnboardingPlanGenerator.generate(profile, catalog)
                _generatedPlan.value = plan
                repository.saveProfile(profile)
                repository.saveGeneratedPlan(plan)
                _step.value = OnboardingStep.GENERATE_PLAN
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Could not generate plan"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun finishOnboarding(
        userId: String?,
        forms: List<FPForm>,
        onSubmitForm: suspend (FPForm, List<com.fitproject.droid.data.FPFormAnswer>) -> Result<Unit>
    ) {
        viewModelScope.launch {
            val profile = _profile.value
            repository.saveProfile(profile)
            repository.markComplete(skipped = false)

            userId?.let { id ->
                runCatching { FirestoreService.shared.saveOnboardingProfile(id, profile) }
                OnboardingFormSubmitter.findOnboardingForm(forms)?.let { form ->
                    val answers = OnboardingFormSubmitter.buildAnswers(form, profile)
                    if (answers.any { it.value.isNotBlank() }) {
                        onSubmitForm(form, answers)
                    }
                }
            }

            _completed.value = true
        }
    }

    fun programWeek() = _generatedPlan.value?.let { OnboardingPlanConverter.toProgramWeek(it) }

    fun program() = _generatedPlan.value?.let { OnboardingPlanConverter.toProgram(it) }

    private fun persistDraft() {
        viewModelScope.launch {
            repository.saveProfile(_profile.value)
        }
    }

    private fun calculateBodyFatFromMeasurements() {
        val p = _profile.value
        val height = p.heightCm ?: return
        val gender = p.gender ?: return
        val neck = p.neckCm ?: return
        val waist = p.waistCm ?: return
        val percent = BodyFatCalculator.navyBodyFatPercent(
            gender = gender,
            heightCm = height,
            neckCm = neck,
            waistCm = waist,
            hipCm = p.hipCm
        )
        _profile.update { it.copy(bodyFatPercent = percent, bodyFatMethod = BodyFatMethod.MEASUREMENT) }
    }

    private fun stepSequence(profile: OnboardingProfile): List<OnboardingStep> {
        val head = listOf(
            OnboardingStep.WELCOME,
            OnboardingStep.GOAL,
            OnboardingStep.GENDER,
            OnboardingStep.DATE_OF_BIRTH,
            OnboardingStep.HEIGHT_WEIGHT,
            OnboardingStep.BODY_FAT_METHOD
        )
        val bf = when (profile.bodyFatMethod) {
            BodyFatMethod.VISUAL_PHOTO -> OnboardingStep.BODY_FAT_PHOTO
            BodyFatMethod.MEASUREMENT -> OnboardingStep.BODY_FAT_MEASURE
            null -> null
        }
        val tail = listOf(
            OnboardingStep.BODY_FAT_RESULT,
            OnboardingStep.MUSCLE_GOAL,
            OnboardingStep.EXPERIENCE,
            OnboardingStep.AVAILABLE_DAYS,
            OnboardingStep.DAYS_PER_WEEK,
            OnboardingStep.DURATION,
            OnboardingStep.MUSCLE_PRIORITY,
            OnboardingStep.BODY_FOCUS,
            OnboardingStep.WORKOUT_SPLIT,
            OnboardingStep.GYM_TYPE,
            OnboardingStep.EQUIPMENT,
            OnboardingStep.GENERATE_PLAN
        )
        return buildList {
            addAll(head)
            bf?.let { add(it) }
            addAll(tail)
        }
    }

    private fun canProceed(step: OnboardingStep, profile: OnboardingProfile): Boolean = when (step) {
        OnboardingStep.WELCOME -> profile.firstName.isNotBlank()
        OnboardingStep.GOAL -> profile.goal != null
        OnboardingStep.GENDER -> profile.gender != null
        OnboardingStep.DATE_OF_BIRTH -> profile.dateOfBirth != null
        OnboardingStep.HEIGHT_WEIGHT -> profile.heightCm != null && profile.weightKg != null
        OnboardingStep.BODY_FAT_METHOD -> profile.bodyFatMethod != null
        OnboardingStep.BODY_FAT_PHOTO -> profile.bodyFatPercent != null
        OnboardingStep.BODY_FAT_MEASURE -> {
            profile.neckCm != null && profile.waistCm != null &&
                (profile.gender != com.fitproject.droid.data.onboarding.Gender.FEMALE || profile.hipCm != null)
        }
        OnboardingStep.BODY_FAT_RESULT -> profile.bodyFatPercent != null
        OnboardingStep.EXPERIENCE -> profile.experience != null
        OnboardingStep.AVAILABLE_DAYS -> profile.availableDays.isNotEmpty()
        OnboardingStep.EQUIPMENT -> profile.equipment.isNotEmpty() || profile.gymType == com.fitproject.droid.data.onboarding.GymType.BODYWEIGHT
        else -> true
    }

    private fun validationMessage(step: OnboardingStep): String = when (step) {
        OnboardingStep.WELCOME -> "Please enter your name"
        OnboardingStep.GOAL -> "Select your goal"
        OnboardingStep.GENDER -> "Select your gender"
        OnboardingStep.DATE_OF_BIRTH -> "Select your date of birth"
        OnboardingStep.HEIGHT_WEIGHT -> "Enter height and weight"
        OnboardingStep.BODY_FAT_METHOD -> "Choose how to estimate body fat"
        OnboardingStep.BODY_FAT_PHOTO -> "Set your visual estimate"
        OnboardingStep.BODY_FAT_MEASURE -> "Enter all measurements"
        OnboardingStep.EXPERIENCE -> "Select your experience level"
        OnboardingStep.AVAILABLE_DAYS -> "Pick at least one day"
        OnboardingStep.EQUIPMENT -> "Select available equipment"
        else -> "Please complete this step"
    }

    companion object {
        suspend fun needsOnboarding(context: android.content.Context): Boolean =
            !OnboardingRepository(context).isComplete.first()
    }
}