package com.fitproject.droid.data.onboarding

import java.util.Date

enum class FitnessGoal(val label: String) {
    GET_LEAN("Get lean"),
    BUILD_MUSCLE("Build muscle"),
    MAINTAIN("Maintain"),
    LOSE_WEIGHT("Lose weight")
}

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}

enum class BodyFatMethod {
    VISUAL_PHOTO,
    MEASUREMENT
}

enum class ExperienceLevel(val label: String, val setsRange: IntRange, val repRange: IntRange) {
    BEGINNER("Beginner", 2..3, 10..12),
    INTERMEDIATE("Intermediate", 3..4, 8..12),
    ADVANCED("Advanced", 4..5, 6..12)
}

enum class Weekday(val label: String, val index: Int) {
    MON("Mon", 1), TUE("Tue", 2), WED("Wed", 3),
    THU("Thu", 4), FRI("Fri", 5), SAT("Sat", 6), SUN("Sun", 7)
}

enum class MusclePriority(val label: String) {
    EVERYTHING("Everything balanced"),
    CHEST("Chest"),
    BACK("Back"),
    LEGS("Legs"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    GLUTES("Glutes")
}

enum class BodyFocus(val label: String) {
    FULL_BODY("Full body"),
    UPPER_ONLY("Upper body only")
}

enum class WorkoutSplit(val label: String, val description: String) {
    FULL_BODY("Full body", "Train all major muscles each session"),
    UPPER_LOWER("Upper / Lower", "Alternate upper and lower days"),
    PUSH_PULL_LEGS("Push / Pull / Legs", "Classic PPL rotation"),
    BRO_SPLIT("Bro split", "One muscle group per day")
}

enum class GymType(val label: String) {
    COMMERCIAL("Commercial gym"),
    HOME("Home gym"),
    BODYWEIGHT("Bodyweight only"),
    LIMITED("Limited equipment")
}

enum class Equipment(val label: String) {
    BARBELL("Barbell"),
    DUMBBELLS("Dumbbells"),
    CABLES("Cable machine"),
    MACHINES("Machines"),
    BENCH("Bench"),
    PULL_UP_BAR("Pull-up bar"),
    RESISTANCE_BANDS("Resistance bands"),
    CARDIO("Cardio machines"),
    BODYWEIGHT("Bodyweight")
}

enum class OnboardingStep {
    WELCOME,
    GOAL,
    GENDER,
    DATE_OF_BIRTH,
    HEIGHT_WEIGHT,
    BODY_FAT_METHOD,
    BODY_FAT_PHOTO,
    BODY_FAT_MEASURE,
    BODY_FAT_RESULT,
    MUSCLE_GOAL,
    EXPERIENCE,
    AVAILABLE_DAYS,
    DAYS_PER_WEEK,
    DURATION,
    MUSCLE_PRIORITY,
    BODY_FOCUS,
    WORKOUT_SPLIT,
    GYM_TYPE,
    EQUIPMENT,
    GENERATE_PLAN
}

data class OnboardingProfile(
    var firstName: String = "",
    var goal: FitnessGoal? = null,
    var gender: Gender? = null,
    var dateOfBirth: Date? = null,
    var heightCm: Double? = null,
    var weightKg: Double? = null,
    var bodyFatMethod: BodyFatMethod? = null,
    var bodyFatPercent: Double? = null,
    var neckCm: Double? = null,
    var waistCm: Double? = null,
    var hipCm: Double? = null,
    var muscleGoalKg: Double = 0.0,
    var experience: ExperienceLevel? = null,
    var availableDays: Set<Weekday> = emptySet(),
    var daysPerWeek: Int = 3,
    var sessionMinutes: Int = 45,
    var musclePriority: MusclePriority = MusclePriority.EVERYTHING,
    var bodyFocus: BodyFocus = BodyFocus.FULL_BODY,
    var workoutSplit: WorkoutSplit = WorkoutSplit.FULL_BODY,
    var gymType: GymType = GymType.COMMERCIAL,
    var equipment: Set<Equipment> = emptySet(),
    var completedAt: Long? = null,
    var skipped: Boolean = false
)

data class GeneratedExercisePlan(
    val id: String,
    val title: String,
    val summary: String,
    val workouts: List<GeneratedWorkoutDay>,
    val createdAt: Long = System.currentTimeMillis()
)

data class GeneratedWorkoutDay(
    val dayLabel: String,
    val focus: String,
    val warmup: List<String>,
    val exercises: List<GeneratedExercise>,
    val cooldown: List<String>,
    val estimatedMinutes: Int
)

data class GeneratedExercise(
    val name: String,
    val exerciseId: String?,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val rpe: String?,
    val tempo: String,
    val notes: String?
)