package com.fitproject.droid.data.onboarding

import com.fitproject.droid.data.FPForm
import com.fitproject.droid.data.FPFormAnswer

object OnboardingFormSubmitter {
    private val keywords = listOf("onboarding", "intake", "client profile", "new client", "assessment")

    fun findOnboardingForm(forms: List<FPForm>): FPForm? =
        forms.firstOrNull { form ->
            val title = form.title.lowercase()
            keywords.any { title.contains(it) }
        }

    fun buildAnswers(form: FPForm, profile: OnboardingProfile): List<FPFormAnswer> {
        val data = profileToMap(profile)
        return form.fields.map { field ->
            val answer = matchField(field.question, data)
            FPFormAnswer(
                fieldId = field.id,
                question = field.question,
                type = field.type,
                value = answer
            )
        }
    }

    private fun profileToMap(profile: OnboardingProfile): Map<String, String> = buildMap {
        put("name", profile.firstName)
        put("first name", profile.firstName)
        put("goal", profile.goal?.label ?: "")
        put("gender", profile.gender?.label ?: "")
        profile.dateOfBirth?.let { put("date of birth", it.toString()) }
        profile.heightCm?.let { put("height", "%.0f cm".format(it)) }
        profile.weightKg?.let { put("weight", "%.1f kg".format(it)) }
        profile.bodyFatPercent?.let { put("body fat", "%.1f%%".format(it)) }
        put("muscle goal", "${profile.muscleGoalKg} kg")
        put("experience", profile.experience?.label ?: "")
        put("available days", profile.availableDays.joinToString(", ") { it.label })
        put("days per week", profile.daysPerWeek.toString())
        put("session duration", "${profile.sessionMinutes} min")
        put("priority", profile.musclePriority.label)
        put("body focus", profile.bodyFocus.label)
        put("split", profile.workoutSplit.label)
        put("gym", profile.gymType.label)
        put("equipment", profile.equipment.joinToString(", ") { it.label })
    }

    private fun matchField(question: String, data: Map<String, String>): String {
        val q = question.lowercase()
        data.entries.forEach { (key, value) ->
            if (q.contains(key) && value.isNotBlank()) return value
        }
        return ""
    }
}