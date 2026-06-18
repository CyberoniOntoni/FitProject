package com.fitproject.droid.data.onboarding

import com.fitproject.droid.data.FPWorkout
import com.fitproject.droid.data.FPWorkoutExercise

data class CatalogExercise(
    val name: String,
    val exerciseId: String?,
    val muscleGroup: String,
    val equipment: Set<Equipment>,
    val youtubeId: String? = null
)

object ExerciseCatalog {
    private val builtIn = listOf(
        CatalogExercise("Barbell Back Squat", null, "Legs", setOf(Equipment.BARBELL, Equipment.BENCH)),
        CatalogExercise("Romanian Deadlift", null, "Legs", setOf(Equipment.BARBELL, Equipment.DUMBBELLS)),
        CatalogExercise("Barbell Bench Press", null, "Chest", setOf(Equipment.BARBELL, Equipment.BENCH)),
        CatalogExercise("Incline Dumbbell Press", null, "Chest", setOf(Equipment.DUMBBELLS, Equipment.BENCH)),
        CatalogExercise("Overhead Press", null, "Shoulders", setOf(Equipment.BARBELL, Equipment.DUMBBELLS)),
        CatalogExercise("Lat Pulldown", null, "Back", setOf(Equipment.CABLES, Equipment.MACHINES)),
        CatalogExercise("Barbell Row", null, "Back", setOf(Equipment.BARBELL)),
        CatalogExercise("Pull-Up", null, "Back", setOf(Equipment.PULL_UP_BAR)),
        CatalogExercise("Cable Fly", null, "Chest", setOf(Equipment.CABLES)),
        CatalogExercise("Leg Press", null, "Legs", setOf(Equipment.MACHINES)),
        CatalogExercise("Leg Curl", null, "Legs", setOf(Equipment.MACHINES)),
        CatalogExercise("Walking Lunge", null, "Legs", setOf(Equipment.DUMBBELLS, Equipment.BODYWEIGHT)),
        CatalogExercise("Dumbbell Shoulder Press", null, "Shoulders", setOf(Equipment.DUMBBELLS)),
        CatalogExercise("Lateral Raise", null, "Shoulders", setOf(Equipment.DUMBBELLS, Equipment.CABLES)),
        CatalogExercise("Tricep Pushdown", null, "Arms", setOf(Equipment.CABLES)),
        CatalogExercise("Barbell Curl", null, "Arms", setOf(Equipment.BARBELL, Equipment.DUMBBELLS)),
        CatalogExercise("Hip Thrust", null, "Glutes", setOf(Equipment.BARBELL, Equipment.BENCH)),
        CatalogExercise("Plank", null, "Core", setOf(Equipment.BODYWEIGHT)),
        CatalogExercise("Push-Up", null, "Chest", setOf(Equipment.BODYWEIGHT)),
        CatalogExercise("Bodyweight Squat", null, "Legs", setOf(Equipment.BODYWEIGHT)),
        CatalogExercise("Band Pull-Apart", null, "Back", setOf(Equipment.RESISTANCE_BANDS))
    )

    fun harvestFromWorkouts(workouts: List<FPWorkout>): List<CatalogExercise> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<CatalogExercise>()
        for (workout in workouts) {
            for (exercise in workout.exercises) {
                if (!seen.add(exercise.name.lowercase())) continue
                result += CatalogExercise(
                    name = exercise.name,
                    exerciseId = exercise.exerciseId.takeIf { it.isNotBlank() },
                    muscleGroup = inferMuscleGroup(exercise.name),
                    equipment = inferEquipment(exercise.name),
                    youtubeId = exercise.youtubeId
                )
            }
        }
        return result
    }

    fun availableExercises(
        profile: OnboardingProfile,
        harvested: List<CatalogExercise> = emptyList()
    ): List<CatalogExercise> {
        val merged = (harvested + builtIn).distinctBy { it.name.lowercase() }
        return merged.filter { matchesEquipment(it, profile) }
    }

    private fun matchesEquipment(exercise: CatalogExercise, profile: OnboardingProfile): Boolean {
        if (profile.gymType == GymType.BODYWEIGHT) {
            return Equipment.BODYWEIGHT in exercise.equipment
        }
        if (profile.equipment.isEmpty()) return true
        return exercise.equipment.any { it in profile.equipment || it == Equipment.BODYWEIGHT }
    }

    private fun inferMuscleGroup(name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("squat") || n.contains("lunge") || n.contains("leg") -> "Legs"
            n.contains("bench") || n.contains("chest") || n.contains("fly") || n.contains("push") -> "Chest"
            n.contains("row") || n.contains("pull") || n.contains("lat") -> "Back"
            n.contains("shoulder") || n.contains("overhead") || n.contains("lateral") -> "Shoulders"
            n.contains("curl") || n.contains("tricep") || n.contains("bicep") -> "Arms"
            n.contains("hip") || n.contains("glute") -> "Glutes"
            else -> "Everything"
        }
    }

    private fun inferEquipment(name: String): Set<Equipment> {
        val n = name.lowercase()
        val set = mutableSetOf<Equipment>()
        if (n.contains("barbell")) set += Equipment.BARBELL
        if (n.contains("dumbbell")) set += Equipment.DUMBBELLS
        if (n.contains("cable")) set += Equipment.CABLES
        if (n.contains("machine") || n.contains("press")) set += Equipment.MACHINES
        if (n.contains("pull-up") || n.contains("pull up")) set += Equipment.PULL_UP_BAR
        if (n.contains("band")) set += Equipment.RESISTANCE_BANDS
        if (n.contains("bodyweight") || n.contains("push-up") || n.contains("plank")) {
            set += Equipment.BODYWEIGHT
        }
        if (set.isEmpty()) set += Equipment.DUMBBELLS
        return set
    }
}