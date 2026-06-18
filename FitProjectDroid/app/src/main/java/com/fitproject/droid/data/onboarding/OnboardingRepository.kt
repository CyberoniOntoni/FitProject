package com.fitproject.droid.data.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore("onboarding")

class OnboardingRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SKIPPED = booleanPreferencesKey("onboarding_skipped")
        val FIRST_NAME = stringPreferencesKey("first_name")
        val GOAL = stringPreferencesKey("goal")
        val GENDER = stringPreferencesKey("gender")
        val DOB = longPreferencesKey("dob")
        val HEIGHT = doublePreferencesKey("height_cm")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val BF_METHOD = stringPreferencesKey("bf_method")
        val BF_PERCENT = doublePreferencesKey("bf_percent")
        val NECK = doublePreferencesKey("neck_cm")
        val WAIST = doublePreferencesKey("waist_cm")
        val HIP = doublePreferencesKey("hip_cm")
        val MUSCLE_GOAL = doublePreferencesKey("muscle_goal_kg")
        val EXPERIENCE = stringPreferencesKey("experience")
        val AVAILABLE_DAYS = stringPreferencesKey("available_days")
        val DAYS_PER_WEEK = intPreferencesKey("days_per_week")
        val SESSION_MIN = intPreferencesKey("session_min")
        val PRIORITY = stringPreferencesKey("priority")
        val BODY_FOCUS = stringPreferencesKey("body_focus")
        val SPLIT = stringPreferencesKey("split")
        val GYM = stringPreferencesKey("gym")
        val EQUIPMENT = stringPreferencesKey("equipment")
        val GENERATED_PLAN = stringPreferencesKey("generated_plan_json")
    }

    val isComplete: Flow<Boolean> = context.onboardingDataStore.data.map {
        it[Keys.COMPLETE] == true
    }

    suspend fun loadProfile(): OnboardingProfile {
        val prefs = context.onboardingDataStore.data.first()
        return OnboardingProfile(
            firstName = prefs[Keys.FIRST_NAME] ?: "",
            goal = prefs[Keys.GOAL]?.let { runCatching { FitnessGoal.valueOf(it) }.getOrNull() },
            gender = prefs[Keys.GENDER]?.let { runCatching { Gender.valueOf(it) }.getOrNull() },
            dateOfBirth = prefs[Keys.DOB]?.let { java.util.Date(it) },
            heightCm = prefs[Keys.HEIGHT],
            weightKg = prefs[Keys.WEIGHT],
            bodyFatMethod = prefs[Keys.BF_METHOD]?.let { runCatching { BodyFatMethod.valueOf(it) }.getOrNull() },
            bodyFatPercent = prefs[Keys.BF_PERCENT],
            neckCm = prefs[Keys.NECK],
            waistCm = prefs[Keys.WAIST],
            hipCm = prefs[Keys.HIP],
            muscleGoalKg = prefs[Keys.MUSCLE_GOAL] ?: 0.0,
            experience = prefs[Keys.EXPERIENCE]?.let { runCatching { ExperienceLevel.valueOf(it) }.getOrNull() },
            availableDays = prefs[Keys.AVAILABLE_DAYS]?.split(",")?.mapNotNull {
                runCatching { Weekday.valueOf(it) }.getOrNull()
            }?.toSet() ?: emptySet(),
            daysPerWeek = prefs[Keys.DAYS_PER_WEEK] ?: 3,
            sessionMinutes = prefs[Keys.SESSION_MIN] ?: 45,
            musclePriority = prefs[Keys.PRIORITY]?.let { runCatching { MusclePriority.valueOf(it) }.getOrNull() }
                ?: MusclePriority.EVERYTHING,
            bodyFocus = prefs[Keys.BODY_FOCUS]?.let { runCatching { BodyFocus.valueOf(it) }.getOrNull() }
                ?: BodyFocus.FULL_BODY,
            workoutSplit = prefs[Keys.SPLIT]?.let { runCatching { WorkoutSplit.valueOf(it) }.getOrNull() }
                ?: WorkoutSplit.FULL_BODY,
            gymType = prefs[Keys.GYM]?.let { runCatching { GymType.valueOf(it) }.getOrNull() }
                ?: GymType.COMMERCIAL,
            equipment = prefs[Keys.EQUIPMENT]?.split(",")?.mapNotNull {
                runCatching { Equipment.valueOf(it) }.getOrNull()
            }?.toSet() ?: emptySet(),
            completedAt = prefs[Keys.COMPLETE]?.let { if (it) System.currentTimeMillis() else null },
            skipped = prefs[Keys.SKIPPED] == true
        )
    }

    suspend fun saveProfile(profile: OnboardingProfile) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.FIRST_NAME] = profile.firstName
            profile.goal?.name?.let { prefs[Keys.GOAL] = it }
            profile.gender?.name?.let { prefs[Keys.GENDER] = it }
            profile.dateOfBirth?.time?.let { prefs[Keys.DOB] = it }
            profile.heightCm?.let { prefs[Keys.HEIGHT] = it }
            profile.weightKg?.let { prefs[Keys.WEIGHT] = it }
            profile.bodyFatMethod?.name?.let { prefs[Keys.BF_METHOD] = it }
            profile.bodyFatPercent?.let { prefs[Keys.BF_PERCENT] = it }
            profile.neckCm?.let { prefs[Keys.NECK] = it }
            profile.waistCm?.let { prefs[Keys.WAIST] = it }
            profile.hipCm?.let { prefs[Keys.HIP] = it }
            prefs[Keys.MUSCLE_GOAL] = profile.muscleGoalKg
            profile.experience?.name?.let { prefs[Keys.EXPERIENCE] = it }
            prefs[Keys.AVAILABLE_DAYS] = profile.availableDays.joinToString(",") { it.name }
            prefs[Keys.DAYS_PER_WEEK] = profile.daysPerWeek
            prefs[Keys.SESSION_MIN] = profile.sessionMinutes
            prefs[Keys.PRIORITY] = profile.musclePriority.name
            prefs[Keys.BODY_FOCUS] = profile.bodyFocus.name
            prefs[Keys.SPLIT] = profile.workoutSplit.name
            prefs[Keys.GYM] = profile.gymType.name
            prefs[Keys.EQUIPMENT] = profile.equipment.joinToString(",") { it.name }
        }
    }

    suspend fun markComplete(skipped: Boolean = false) {
        context.onboardingDataStore.edit {
            it[Keys.COMPLETE] = true
            it[Keys.SKIPPED] = skipped
        }
    }

    suspend fun saveGeneratedPlan(plan: GeneratedExercisePlan) {
        val payload = json.encodeToString(GeneratedPlanPayload.from(plan))
        context.onboardingDataStore.edit { it[Keys.GENERATED_PLAN] = payload }
    }

    suspend fun loadGeneratedPlan(): GeneratedExercisePlan? {
        val raw = context.onboardingDataStore.data.first()[Keys.GENERATED_PLAN] ?: return null
        return runCatching {
            GeneratedPlanPayload.fromJson(json, raw).toPlan()
        }.getOrNull()
    }

    suspend fun reset() {
        context.onboardingDataStore.edit { it.clear() }
    }
}

@Serializable
private data class GeneratedPlanPayload(
    val id: String,
    val title: String,
    val summary: String,
    val workouts: List<WorkoutPayload>
) {
    companion object {
        fun from(plan: GeneratedExercisePlan) = GeneratedPlanPayload(
            id = plan.id,
            title = plan.title,
            summary = plan.summary,
            workouts = plan.workouts.map { day ->
                WorkoutPayload(
                    dayLabel = day.dayLabel,
                    focus = day.focus,
                    warmup = day.warmup,
                    cooldown = day.cooldown,
                    estimatedMinutes = day.estimatedMinutes,
                    exercises = day.exercises.map { ex ->
                        ExercisePayload(ex.name, ex.exerciseId, ex.sets, ex.reps, ex.restSeconds, ex.rpe, ex.tempo, ex.notes)
                    }
                )
            }
        )

        fun fromJson(json: Json, raw: String) = json.decodeFromString<GeneratedPlanPayload>(raw)
    }

    fun toPlan() = GeneratedExercisePlan(
        id = id,
        title = title,
        summary = summary,
        workouts = workouts.map { day ->
            GeneratedWorkoutDay(
                dayLabel = day.dayLabel,
                focus = day.focus,
                warmup = day.warmup,
                cooldown = day.cooldown,
                estimatedMinutes = day.estimatedMinutes,
                exercises = day.exercises.map { ex ->
                    GeneratedExercise(ex.name, ex.exerciseId, ex.sets, ex.reps, ex.restSeconds, ex.rpe, ex.tempo, ex.notes)
                }
            )
        }
    )
}

@Serializable
private data class WorkoutPayload(
    val dayLabel: String,
    val focus: String,
    val warmup: List<String>,
    val cooldown: List<String>,
    val estimatedMinutes: Int,
    val exercises: List<ExercisePayload>
)

@Serializable
private data class ExercisePayload(
    val name: String,
    val exerciseId: String?,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val rpe: String?,
    val tempo: String,
    val notes: String?
)