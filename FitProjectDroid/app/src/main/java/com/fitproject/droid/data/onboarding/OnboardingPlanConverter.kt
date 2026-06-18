package com.fitproject.droid.data.onboarding

import com.fitproject.droid.data.FPExerciseGroup
import com.fitproject.droid.data.FPMetricValue
import com.fitproject.droid.data.FPProgram
import com.fitproject.droid.data.FPProgramWeek
import com.fitproject.droid.data.FPWorkout
import com.fitproject.droid.data.FPWorkoutExercise
import com.fitproject.droid.data.FPWorkoutMetric
import java.util.UUID

object OnboardingPlanConverter {
    const val PROGRAM_ID = "onboarding-generated"
    private const val WEEK_ID = "onboarding-week-1"

    fun toProgram(plan: GeneratedExercisePlan): FPProgram = FPProgram(
        id = PROGRAM_ID,
        title = plan.title,
        description = plan.summary,
        totalWeekCount = 1,
        totalWorkoutCount = plan.workouts.size,
        published = true
    )

    fun toProgramWeek(plan: GeneratedExercisePlan): FPProgramWeek = FPProgramWeek(
        id = WEEK_ID,
        name = "Your Plan",
        index = 0,
        programId = PROGRAM_ID,
        workouts = plan.workouts.mapIndexed { index, day -> toWorkout(day, index) }
    )

    fun toWorkouts(plan: GeneratedExercisePlan): List<FPWorkout> =
        plan.workouts.mapIndexed { index, day -> toWorkout(day, index) }

    private fun toWorkout(day: GeneratedWorkoutDay, index: Int): FPWorkout {
        val warmupGroupId = UUID.randomUUID().toString()
        val mainGroupId = UUID.randomUUID().toString()
        val cooldownGroupId = UUID.randomUUID().toString()

        val groups = listOf(
            FPExerciseGroup(id = warmupGroupId, name = "Warm-up", index = 0, type = "warmup"),
            FPExerciseGroup(id = mainGroupId, name = "Training", index = 1, type = "main"),
            FPExerciseGroup(id = cooldownGroupId, name = "Cool-down", index = 2, type = "cooldown")
        )

        var exerciseIndex = 0
        val exercises = mutableListOf<FPWorkoutExercise>()
        val metrics = mutableListOf<FPWorkoutMetric>()
        val metricValues = mutableListOf<FPMetricValue>()

        day.warmup.forEach { step ->
            val exId = UUID.randomUUID().toString()
            val isFirst = exercises.isEmpty()
            exercises += FPWorkoutExercise(
                id = exId,
                name = step.take(48),
                exerciseId = "",
                index = exerciseIndex++,
                sets = 1,
                coachNotes = step,
                header = if (isFirst) "Warm-up (NCSF)" else null,
                headerVisible = isFirst,
                groupId = warmupGroupId
            )
            addTimeMetric(exId, metrics, metricValues, "5", exerciseIndex)
        }

        day.exercises.forEachIndexed { i, generated ->
            val exId = UUID.randomUUID().toString()
            exercises += FPWorkoutExercise(
                id = exId,
                name = generated.name,
                exerciseId = generated.exerciseId ?: "",
                index = exerciseIndex++,
                sets = generated.sets,
                coachNotes = generated.notes,
                header = if (i == 0) day.focus else null,
                headerVisible = i == 0,
                groupId = mainGroupId
            )
            addTrainingMetrics(exId, generated, metrics, metricValues)
        }

        day.cooldown.forEachIndexed { i, step ->
            val exId = UUID.randomUUID().toString()
            exercises += FPWorkoutExercise(
                id = exId,
                name = step.take(48),
                exerciseId = "",
                index = exerciseIndex++,
                sets = 1,
                coachNotes = step,
                header = if (i == 0) "Cool-down" else null,
                headerVisible = i == 0,
                groupId = cooldownGroupId
            )
            addTimeMetric(exId, metrics, metricValues, "3", exerciseIndex)
        }

        val warmupNotes = day.warmup.joinToString("\n") { "• $it" }
        val cooldownNotes = day.cooldown.joinToString("\n") { "• $it" }

        return FPWorkout(
            id = "onboarding-${index}",
            name = day.dayLabel,
            description = "${day.focus} · ~${day.estimatedMinutes} min",
            index = index,
            programId = PROGRAM_ID,
            programWeekId = WEEK_ID,
            notes = "Warm-up\n$warmupNotes\n\nCool-down\n$cooldownNotes",
            exerciseGroups = groups,
            exercises = exercises,
            metrics = metrics,
            metricValues = metricValues
        )
    }

    private fun addTimeMetric(
        exerciseId: String,
        metrics: MutableList<FPWorkoutMetric>,
        values: MutableList<FPMetricValue>,
        minutes: String,
        seed: Int
    ) {
        val metricId = UUID.randomUUID().toString()
        metrics += FPWorkoutMetric(
            id = metricId,
            name = "Time",
            index = 0,
            workoutExerciseId = exerciseId,
            unitAbbreviation = "min"
        )
        values += FPMetricValue(
            id = UUID.randomUUID().toString(),
            workoutExerciseId = exerciseId,
            workoutExerciseMetricId = metricId,
            set = 1,
            index = seed,
            value = minutes
        )
    }

    private fun addTrainingMetrics(
        exerciseId: String,
        generated: GeneratedExercise,
        metrics: MutableList<FPWorkoutMetric>,
        values: MutableList<FPMetricValue>
    ) {
        val defs = listOf(
            "Reps" to generated.reps,
            "RPE" to (generated.rpe ?: ""),
            "Rest" to generated.restSeconds.toString(),
            "Tempo" to generated.tempo
        )
        defs.forEachIndexed { metricIndex, (name, value) ->
            if (value.isEmpty()) return@forEachIndexed
            val metricId = UUID.randomUUID().toString()
            metrics += FPWorkoutMetric(
                id = metricId,
                name = name,
                index = metricIndex,
                workoutExerciseId = exerciseId,
                unitAbbreviation = when (name) {
                    "Rest" -> "sec"
                    else -> null
                }
            )
            for (set in 1..generated.sets) {
                values += FPMetricValue(
                    id = UUID.randomUUID().toString(),
                    workoutExerciseId = exerciseId,
                    workoutExerciseMetricId = metricId,
                    set = set,
                    index = set,
                    value = value
                )
            }
        }
    }
}