package com.fitproject.droid.data.onboarding

import java.util.UUID

/**
 * Generates a starter plan using NCSF-style principles:
 * - Warm-up before each session (5–8 min dynamic prep)
 * - Primary compounds + accessories
 * - Cool-down after each session (5 min mobility)
 * - Volume scaled by experience level
 */
object OnboardingPlanGenerator {
    private val warmupTemplate = listOf(
        "5 min light cardio (bike, row, or brisk walk)",
        "Arm circles + band pull-aparts × 15",
        "Bodyweight squats × 12",
        "Hip hinges + inchworms × 8"
    )

    private val cooldownTemplate = listOf(
        "Slow breathing: 4 sec in / 6 sec out × 6",
        "Hamstring + hip flexor stretch × 30 sec each",
        "Chest doorway stretch × 30 sec",
        "Child's pose or spinal decompression × 45 sec"
    )

    fun generate(
        profile: OnboardingProfile,
        catalog: List<CatalogExercise>
    ): GeneratedExercisePlan {
        val experience = profile.experience ?: ExperienceLevel.BEGINNER
        val split = resolveSplit(profile)
        val days = buildDayTemplates(profile, split)
        val workouts = days.mapIndexed { index, template ->
            val exercises = pickExercises(template, profile, catalog, experience)
            GeneratedWorkoutDay(
                dayLabel = template.label,
                focus = template.focus,
                warmup = warmupTemplate,
                exercises = exercises,
                cooldown = cooldownTemplate,
                estimatedMinutes = profile.sessionMinutes
            )
        }

        val goalLabel = profile.goal?.label ?: "Fitness"
        return GeneratedExercisePlan(
            id = UUID.randomUUID().toString(),
            title = "$goalLabel · ${profile.daysPerWeek} day plan",
            summary = buildSummary(profile, split),
            workouts = workouts
        )
    }

    private fun buildSummary(profile: OnboardingProfile, split: WorkoutSplit): String {
        val bf = profile.bodyFatPercent?.let { "%.0f%% body fat".format(it) } ?: "Body comp tracked"
        val muscle = when {
            profile.muscleGoalKg > 0 -> "Gain ${profile.muscleGoalKg} kg muscle"
            profile.muscleGoalKg < 0 -> "Lose ${-profile.muscleGoalKg} kg"
            else -> "Maintain composition"
        }
        return "${profile.experience?.label ?: "Custom"} · ${split.label} · $bf · $muscle"
    }

    private data class DayTemplate(val label: String, val focus: String, val groups: List<String>)

    private fun buildDayTemplates(profile: OnboardingProfile, split: WorkoutSplit): List<DayTemplate> {
        val count = profile.daysPerWeek.coerceIn(2, 6)
        return when (split) {
            WorkoutSplit.FULL_BODY -> (1..count).map { i ->
                DayTemplate("Day $i", "Full body", listOf("Legs", "Chest", "Back", "Shoulders"))
            }
            WorkoutSplit.UPPER_LOWER -> List(count) { i ->
                if (i % 2 == 0) DayTemplate("Day ${i + 1}", "Upper", listOf("Chest", "Back", "Shoulders", "Arms"))
                else DayTemplate("Day ${i + 1}", "Lower", listOf("Legs", "Glutes", "Core"))
            }
            WorkoutSplit.PUSH_PULL_LEGS -> {
                val rotation = listOf(
                    DayTemplate("Push", "Push", listOf("Chest", "Shoulders", "Arms")),
                    DayTemplate("Pull", "Pull", listOf("Back", "Arms")),
                    DayTemplate("Legs", "Legs", listOf("Legs", "Glutes"))
                )
                List(count) { rotation[it % rotation.size].copy(label = "Day ${it + 1} · ${rotation[it % rotation.size].label}") }
            }
            WorkoutSplit.BRO_SPLIT -> {
                val groups = when (profile.bodyFocus) {
                    BodyFocus.UPPER_ONLY -> listOf("Chest", "Back", "Shoulders", "Arms")
                    BodyFocus.FULL_BODY -> listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Glutes")
                }
                List(count) { i ->
                    val g = groups[i % groups.size]
                    DayTemplate("Day ${i + 1}", g, listOf(g))
                }
            }
        }
    }

    private fun resolveSplit(profile: OnboardingProfile): WorkoutSplit {
        if (profile.bodyFocus == BodyFocus.UPPER_ONLY && profile.workoutSplit == WorkoutSplit.FULL_BODY) {
            return WorkoutSplit.UPPER_LOWER
        }
        return profile.workoutSplit
    }

    private fun pickExercises(
        template: DayTemplate,
        profile: OnboardingProfile,
        catalog: List<CatalogExercise>,
        experience: ExperienceLevel
    ): List<GeneratedExercise> {
        val priority = profile.musclePriority
        val orderedGroups = template.groups.sortedByDescending { group ->
            when (priority) {
                MusclePriority.EVERYTHING -> 0
                MusclePriority.CHEST -> if (group == "Chest") 10 else 0
                MusclePriority.BACK -> if (group == "Back") 10 else 0
                MusclePriority.LEGS -> if (group == "Legs") 10 else 0
                MusclePriority.SHOULDERS -> if (group == "Shoulders") 10 else 0
                MusclePriority.ARMS -> if (group == "Arms") 10 else 0
                MusclePriority.GLUTES -> if (group == "Glutes") 10 else 0
            }
        }

        val maxExercises = when (profile.sessionMinutes) {
            in 0..35 -> 4
            in 36..50 -> 5
            in 51..65 -> 6
            else -> 7
        }

        val selected = mutableListOf<CatalogExercise>()
        for (group in orderedGroups) {
            catalog.filter { it.muscleGroup == group || group == "Everything" }
                .shuffled()
                .forEach { ex ->
                    if (selected.size < maxExercises && selected.none { it.name == ex.name }) {
                        selected += ex
                    }
                }
        }
        if (selected.size < 3) {
            catalog.shuffled().forEach { ex ->
                if (selected.size < maxExercises && selected.none { it.name == ex.name }) selected += ex
            }
        }

        val sets = experience.setsRange.last.coerceAtMost(5)
        val reps = "${experience.repRange.first}-${experience.repRange.last}"
        val tempo = when (profile.goal) {
            FitnessGoal.BUILD_MUSCLE -> "301"
            FitnessGoal.GET_LEAN, FitnessGoal.LOSE_WEIGHT -> "201"
            else -> "211"
        }

        return selected.mapIndexed { index, ex ->
            GeneratedExercise(
                name = ex.name,
                exerciseId = ex.exerciseId,
                sets = if (index == 0) sets else (sets - 1).coerceAtLeast(2),
                reps = reps,
                restSeconds = if (index == 0) 120 else 90,
                rpe = when (experience) {
                    ExperienceLevel.BEGINNER -> "7"
                    ExperienceLevel.INTERMEDIATE -> "8"
                    ExperienceLevel.ADVANCED -> "8-9"
                },
                tempo = tempo,
                notes = if (index == 0) "Primary compound — focus on form" else null
            )
        }
    }
}