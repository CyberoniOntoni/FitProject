package com.fitproject.droid.data

import java.util.Date
import java.util.UUID

// MARK: - User

data class FPUser(
    val id: String,
    val email: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var profilePictureUrl: String? = null,
    var timezone: String? = null,
    var coachHasProTools: Boolean = false,
    var unitPreferences: FPUnitPreferences = FPUnitPreferences()
) {
    val displayName: String
        get() {
            val name = "$firstName $lastName".trim()
            return name.ifEmpty { email }
        }

    val initials: String
        get() {
            val parts = displayName.split(" ")
            val first = parts.firstOrNull()?.take(1) ?: ""
            val last = if (parts.size > 1) parts.last().take(1) else ""
            return "$first$last".uppercase()
        }
}

// MARK: - Program

data class FPProgram(
    val id: String,
    var title: String = "",
    var description: String = "",
    var imageUrl: String? = null,
    var totalWeekCount: Int = 0,
    var totalWorkoutCount: Int = 0,
    var published: Boolean = false,
    var creatorIds: List<String> = emptyList(),
    var completedWorkouts: Int = 0
) {
    val progress: Double
        get() = if (totalWorkoutCount > 0) completedWorkouts.toDouble() / totalWorkoutCount else 0.0

    val progressPercent: Int get() = (progress * 100).toInt()
}

data class FPProgramWeek(
    val id: String,
    var name: String = "Week",
    var index: Int = 0,
    var programId: String = "",
    var workouts: List<FPWorkout> = emptyList()
)

// MARK: - Workout

data class FPWorkout(
    val id: String,
    var name: String = "",
    var description: String? = null,
    var index: Int = 0,
    var programId: String = "",
    var programWeekId: String = "",
    var notes: String? = null,
    var exerciseGroups: List<FPExerciseGroup> = emptyList(),
    var exercises: List<FPWorkoutExercise> = emptyList(),
    var metrics: List<FPWorkoutMetric> = emptyList(),
    var metricValues: List<FPMetricValue> = emptyList(),
    var isCompleted: Boolean = false,
    var completedAt: Date? = null
) {
    val exerciseCount: Int get() = exercises.size

    val groupedExercises: List<Pair<FPExerciseGroup?, List<FPWorkoutExercise>>>
        get() {
            val result = mutableListOf<Pair<FPExerciseGroup?, List<FPWorkoutExercise>>>()
            val sorted = exercises.sortedBy { it.index }
            var currentGroupId: String? = null
            var currentExercises = mutableListOf<FPWorkoutExercise>()

            for (exercise in sorted) {
                if (exercise.groupId != currentGroupId) {
                    if (currentExercises.isNotEmpty()) {
                        val group = exerciseGroups.firstOrNull { it.id == currentGroupId }
                        result.add(group to currentExercises.toList())
                    }
                    currentGroupId = exercise.groupId
                    currentExercises = mutableListOf(exercise)
                } else {
                    currentExercises.add(exercise)
                }
            }
            if (currentExercises.isNotEmpty()) {
                val group = exerciseGroups.firstOrNull { it.id == currentGroupId }
                result.add(group to currentExercises.toList())
            }
            return result
        }
}

data class FPExerciseGroup(
    val id: String,
    var name: String = "",
    var index: Int = 0,
    var type: String? = null
)

data class FPWorkoutExercise(
    val id: String,
    var name: String = "",
    var exerciseId: String = "",
    var youtubeId: String? = null,
    var thumbnailUrl: String? = null,
    var index: Int = 0,
    var sets: Int = 0,
    var coachNotes: String? = null,
    var header: String? = null,
    var headerVisible: Boolean = false,
    var groupId: String? = null,
    var isSuperset: Boolean = false
) {
    val videoThumbnailUrl: String?
        get() {
            if (!thumbnailUrl.isNullOrEmpty()) return thumbnailUrl
            if (!youtubeId.isNullOrEmpty()) return "https://img.youtube.com/vi/$youtubeId/mqdefault.jpg"
            return null
        }

    val youtubeWatchUrl: String?
        get() = youtubeId?.takeIf { it.isNotEmpty() }?.let { "https://www.youtube.com/watch?v=$it" }
}

data class FPWorkoutMetric(
    val id: String,
    var name: String = "",
    var index: Int = 0,
    var color: String = "#ffffff",
    var exerciseMetricId: String = "",
    var workoutExerciseId: String = "",
    var unitAbbreviation: String? = null
)

data class FPMetricValue(
    val id: String,
    var workoutExerciseId: String = "",
    var workoutExerciseMetricId: String = "",
    var exerciseMetricId: String = "",
    var exerciseId: String = "",
    var set: Int = 1,
    var index: Int = 0,
    var value: String = "",
    var loggedValue: String? = null,
    var isCompleted: Boolean = false
)

// MARK: - Workout Log

data class FPWorkoutLog(
    val id: String,
    var userId: String = "",
    var workoutId: String = "",
    var programId: String? = null,
    var workoutName: String = "",
    var startedAt: Date = Date(),
    var completedAt: Date? = null,
    var durationSeconds: Int? = null,
    var exercises: List<FPLoggedExercise> = emptyList(),
    var notes: String? = null,
    var totalVolume: Double = 0.0,
    var prCount: Int = 0
) {
    val isCompleted: Boolean get() = completedAt != null
}

data class FPLoggedExercise(
    val id: String,
    var exerciseId: String = "",
    var name: String = "",
    var sets: List<FPLoggedSet> = emptyList()
)

data class FPLoggedSet(
    val id: String,
    var setNumber: Int = 1,
    var reps: String? = null,
    var weight: String? = null,
    var rpe: String? = null,
    var rest: String? = null,
    var tempo: String? = null,
    var time: String? = null,
    var isCompleted: Boolean = false,
    var isPR: Boolean = false
)

// MARK: - Habit

data class FPHabit(
    val id: String,
    var habitId: String = "",
    var name: String = "",
    var description: String? = null,
    var unit: String = "",
    var frequency: String = "Per Day",
    var targetType: String = "FLOOR",
    var targetMin: Double = 0.0,
    var targetMax: Double = 0.0,
    var icon: String? = null,
    var color: String? = null,
    var index: Int = 0,
    var coachId: String = "",
    var currentValue: Double = 0.0,
    var targetMet: Boolean = false,
    var logDateCreated: Date? = null
) {
    val targetValue: Double get() = if (targetType == "RANGE") targetMax else targetMin
    val isComplete: Boolean get() = HabitSyncHelper.isTargetMet(targetType, targetMin, targetMax, currentValue)
    val progress: Double get() = HabitSyncHelper.progress(targetType, targetMin, targetMax, currentValue)
    val progressText: String get() = HabitSyncHelper.progressText(targetType, targetMin, targetMax, currentValue, unit)
}

data class FPHabitLog(
    val id: String,
    var userHabitId: String = "",
    var userId: String = "",
    var date: Date = Date(),
    var value: Double = 0.0,
    var targetMet: Boolean = false,
    var dateCreated: Date? = null
)

object HabitSyncHelper {
    fun targetType(values: List<Double>, explicit: String? = null): String {
        if (!explicit.isNullOrEmpty()) return explicit
        return if (values.size >= 2) "RANGE" else "FLOOR"
    }

    fun isTargetMet(type: String, min: Double, max: Double, value: Double): Boolean = when (type) {
        "CEILING" -> value <= min
        "RANGE" -> value >= min && value <= max
        else -> value >= min
    }

    fun progress(type: String, min: Double, max: Double, value: Double): Double {
        if (isTargetMet(type, min, max, value)) return 1.0
        return when (type) {
            "CEILING" -> if (min > 0) minOf(value / min, 1.0) else 0.0
            "RANGE" -> when {
                value < min -> if (min > 0) value / min else 0.0
                value > max -> if (value > 0) max / value else 0.0
                else -> 0.0
            }
            else -> if (min > 0) minOf(value / min, 1.0) else 0.0
        }
    }

    fun progressText(type: String, min: Double, max: Double, value: Double, unit: String): String {
        val suffix = if (unit.isEmpty()) "" else " $unit"
        val current = if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)
        val target = if (type == "RANGE") "${formatNumber(min)}-${formatNumber(max)}" else formatNumber(min)
        return "$current / $target$suffix"
    }

    fun startOfDayUnix(date: Date = Date()): Int {
        val cal = java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis / 1000).toInt()
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)
}

// MARK: - Progress Pictures

data class FPProgressPicture(
    val id: String,
    var userId: String = "",
    var sessionId: String = "",
    var poseType: String = "front",
    var imageUrl: String = "",
    var dateCreated: Date = Date(),
    var notes: String? = null
)

data class ProgressPoseUpload(
    val poseType: String,
    val imageUrl: String,
    val existingId: String? = null,
)

data class FPProgressSession(
    val sessionId: String,
    var dateCreated: Date = Date(),
    var pictures: List<FPProgressPicture> = emptyList(),
    var notes: String? = null
) {
    val id: String get() = sessionId
}

data class FPUnitPreferences(
    var weight: String = "KILOGRAM",
    var mass: String = "KILOGRAM",
    var circumference: String = "CENTIMETER",
    var distance: String = "MILE",
    var time: String = "SECOND"
) {
    companion object {
        fun fromFirestore(data: Map<String, Any>?): FPUnitPreferences {
            if (data == null) return FPUnitPreferences()
            fun read(key: String, default: String): String {
                val value = data[key] as? String ?: ""
                return value.ifEmpty { default }
            }
            return FPUnitPreferences(
                weight = read("weight", "KILOGRAM"),
                mass = read("mass", "KILOGRAM"),
                circumference = read("circumference", "CENTIMETER"),
                distance = read("distance", "MILE"),
                time = read("time", "SECOND")
            )
        }
    }

    val massAbbreviation: String get() = if (mass == "POUND") "lb" else "kg"
}

data class PoseImagePayload(
    val data: ByteArray,
    val contentType: String,
    val fileExtension: String
)

data class FPProgressPhotoDraft(
    var sessionId: String = UUID.randomUUID().toString(),
    var dateCreated: Date = Date(),
    var notes: String = "",
    var poseImageData: Map<String, PoseImagePayload> = emptyMap()
) {
    val completedPoseCount: Int get() = poseImageData.size
}

object UnitConversionHelper {
    fun massAbbreviation(unit: String): String = if (unit == "POUND") "lb" else "kg"

    fun circumferenceAbbreviation(unit: String): String = if (unit == "INCH") "in" else "cm"

    fun convertMassForDisplay(kg: Double, unit: String): Double =
        if (unit == "POUND") (kg * 2.20462 * 100).toInt() / 100.0 else (kg * 100).toInt() / 100.0

    fun convertCircumferenceForDisplay(cm: Double, unit: String): Double =
        if (unit == "INCH") ((cm / 2.54) * 100).toInt() / 100.0 else (cm * 100).toInt() / 100.0

    fun massToCanonical(value: Double, unit: String): Double =
        if (unit == "POUND") value / 2.20462 else value

    fun circumferenceToCanonical(value: Double, unit: String): Double =
        if (unit == "INCH") value * 2.54 else value

    fun displayUnit(type: FPMeasurementTypeDef, prefs: FPUnitPreferences): String = when {
        type.category == "Circumference" -> circumferenceAbbreviation(prefs.circumference)
        type.category == "Body Composition" && type.unitType == "MASS" -> massAbbreviation(prefs.mass)
        else -> type.displayUnit
    }

    fun formatDisplayNumber(value: Double): String =
        if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)

    fun formatMeasurementValue(measurement: FPMeasurement, prefs: FPUnitPreferences): String {
        val type = MeasurementCatalog.findById(measurement.typeId)
            ?: MeasurementCatalog.findByName(measurement.name)
        if (type?.category == "Circumference") {
            val display = convertCircumferenceForDisplay(measurement.value, prefs.circumference)
            return "${formatDisplayNumber(display)} ${circumferenceAbbreviation(prefs.circumference)}"
        }
        if (type?.category == "Body Composition" && type.unitType == "MASS") {
            val display = convertMassForDisplay(measurement.value, prefs.mass)
            return "${formatDisplayNumber(display)} ${massAbbreviation(prefs.mass)}"
        }
        return "${formatDisplayNumber(measurement.value)} ${measurement.unit}"
    }
}

// MARK: - Measurement

data class FPMeasurementTypeDef(
    val id: String,
    val name: String,
    val category: String,
    val color: String,
    val unitId: String,
    val unitName: String,
    val unitType: String,
    val unitAbbreviation: String
) {
    val displayUnit: String
        get() = when {
            category == "Circumference" -> "cm"
            category == "Body Composition" && unitType == "MASS" -> "kg"
            category == "Body Composition" && unitType == "PERCENT" -> "%"
            unitAbbreviation == "centimeter" -> "cm"
            unitAbbreviation in listOf("inches", "in", "inch") -> "cm"
            else -> unitAbbreviation
        }
}

object MeasurementCatalog {
    val types: List<FPMeasurementTypeDef> = listOf(
        FPMeasurementTypeDef("DfqsrFQBGi04aHWAPA7I", "Bodyweight", "Body Composition", "#c93477", "kg", "Kilograms", "MASS", "kg"),
        FPMeasurementTypeDef("body_fat_percentage", "Body Fat %", "Body Composition", "#FDCB6E", "%", "PERCENT", "PERCENT", "%"),
        FPMeasurementTypeDef("muscle_mass", "Muscle Mass", "Body Composition", "#E17055", "kg", "MASS", "MASS", "kg"),
        FPMeasurementTypeDef("body_water_percentage", "Body Water %", "Body Composition", "#06b6d4", "%", "PERCENT", "PERCENT", "%"),
        FPMeasurementTypeDef("bone_mass", "Bone Mass", "Body Composition", "#94a3b8", "bone_mass_unit", "kg", "MASS", "kg"),
        FPMeasurementTypeDef("vo2_max", "VO2 Max", "Body Composition", "#7c3aed", "vo2_max_unit", "ml/kg/min", "NUMERIC", "ml/kg/min"),
        FPMeasurementTypeDef("visceral_fat", "Visceral Fat", "Body Composition", "#f97316", "level", "NUMERIC", "NUMERIC", "level"),
        FPMeasurementTypeDef("waist", "Waist", "Circumference", "#4ECDC4", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("chest", "Chest", "Circumference", "#10b981", "cm", "Circumference", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("arms", "Arms", "Circumference", "#FFA07A", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("thighs", "Thighs", "Circumference", "#8b5cf6", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("hips", "Hips", "Circumference", "#45B7D1", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("neck", "Neck", "Circumference", "#06b6d4", "cm", "Circumference", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("shoulders", "Shoulders", "Circumference", "#FD79A8", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("calves", "Calves", "Circumference", "#00B894", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("forearms", "Forearms", "Circumference", "#74B9FF", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("wrist", "Wrist", "Circumference", "#a855f7", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        FPMeasurementTypeDef("ankle", "Ankle", "Circumference", "#fbbf24", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
    )

    val categories = listOf("Body Composition", "Circumference")

    fun findById(id: String?): FPMeasurementTypeDef? = id?.let { types.find { t -> t.id == it } }

    fun findByName(name: String): FPMeasurementTypeDef? {
        types.find { it.name.equals(name, ignoreCase = true) }?.let { return it }
        return when (name.lowercase()) {
            "weight", "body weight" -> findById("DfqsrFQBGi04aHWAPA7I")
            "body fat", "bodyfat" -> findById("body_fat_percentage")
            else -> types.find { it.name.contains(name, ignoreCase = true) }
        }
    }

    fun typesInCategory(category: String): List<FPMeasurementTypeDef> =
        types.filter { it.category == category }
}

data class FPMeasurement(
    val id: String,
    var typeId: String? = null,
    var name: String = "",
    var unit: String = "",
    var value: Double = 0.0,
    var date: Date = Date(),
    var notes: String? = null,
    var sessionId: String? = null,
    var source: String = "measurementLogs"
) {
    val matchKey: String get() = typeId ?: name.lowercase()
}

// MARK: - Content

data class FPContent(
    val id: String,
    var title: String = "",
    var body: String? = null,
    var imageUrl: String? = null,
    var type: String = "article",
    var dateCreated: Date? = null,
    var isRead: Boolean = false
)

// MARK: - Personal Record

data class FPPersonalRecord(
    val id: String,
    var exerciseId: String = "",
    var exerciseName: String = "",
    var metric: String = "",
    var value: String = "",
    var date: Date = Date(),
    var previousValue: String? = null
)

// MARK: - Form

data class FPFormField(
    val id: String,
    var type: String = "Text",
    var question: String = "",
    var required: Boolean = false,
    var index: Int = 0,
    var maxRating: Int = 5,
    var scaleMin: Int = 1,
    var scaleMax: Int = 10,
    var scaleMinLabel: String? = null,
    var scaleMaxLabel: String? = null,
    var options: List<String> = emptyList()
)

data class FPFormAnswer(
    var fieldId: String = "",
    var question: String = "",
    var type: String = "",
    var value: String = ""
)

data class FPFormSubmission(
    var clientId: String = "",
    var submittedAt: Date = Date(),
    var answers: List<FPFormAnswer> = emptyList()
)

data class FPForm(
    val id: String,
    var title: String = "",
    var description: String? = null,
    var creatorId: String? = null,
    var fields: List<FPFormField> = emptyList(),
    var submissions: List<FPFormSubmission> = emptyList(),
    var dueDate: Date? = null
) {
    fun isCompleted(userId: String): Boolean = submissions.any { it.clientId == userId }
}

// MARK: - Assigned Program

data class FPAssignedProgram(
    val id: String,
    var programId: String = "",
    var userId: String = "",
    var coachId: String? = null,
    var program: FPProgram? = null,
    var startDate: Date? = null,
    var currentWeek: Int = 1,
    var completedWorkouts: Int = 0
)

// MARK: - Tab

enum class AppTab(val label: String, val icon: String) {
    SUMMARY("Summary", "insights"),
    TRAIN("Train", "fitness_center"),
    PROGRAMS("Programs", "view_list"),
    LEARN("Learn", "menu_book"),
    HISTORY("History", "history"),
    PROFILE("Profile", "person"),
}

// MARK: - Workout Session

data class WorkoutSessionState(
    val workout: FPWorkout,
    val program: FPProgram? = null,
    val startedAt: Date = Date(),
    var currentExerciseIndex: Int = 0,
    var loggedSets: Map<String, List<FPLoggedSet>> = emptyMap(),
    var notes: String = "",
    var restTimerEnd: Date? = null,
    var restDuration: Int = 90,
    var prCelebrations: List<FPPersonalRecord> = emptyList(),
    var elapsedSeconds: Int = 0
)

// MARK: - Errors

sealed class FitProsError(message: String) : Exception(message) {
    class NotFound(item: String) : FitProsError("$item not found.")
    class SyncFailed(reason: String) : FitProsError("Sync failed: $reason")
}