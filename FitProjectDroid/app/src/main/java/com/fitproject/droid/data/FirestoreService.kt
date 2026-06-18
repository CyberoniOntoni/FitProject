package com.fitproject.droid.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.UUID

@Suppress("UNCHECKED_CAST")
private fun firestoreMap(value: Any?): Map<String, Any>? = value as? Map<String, Any>

class FirestoreService private constructor() {
    private val db = Firebase.firestore
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - User

    suspend fun fetchUserProfile(userId: String): FPUser {
        val doc = db.collection("users").document(userId).get().await()
        val data = doc.data ?: throw FitProsError.NotFound("User profile")
        return FPUser(
            id = userId,
            email = data["email"] as? String ?: "",
            firstName = data["firstName"] as? String ?: "",
            lastName = data["lastName"] as? String ?: "",
            profilePictureUrl = data["profilePictureUrl"] as? String,
            timezone = data["timezone"] as? String,
            coachHasProTools = data["coachHasProTools"] as? Boolean ?: false,
            unitPreferences = FPUnitPreferences.fromFirestore(
                firestoreMap(data["unitPreferences"])
            )
        )
    }

    suspend fun updateUnitPreference(userId: String, key: String, value: String) {
        db.collection("users").document(userId)
            .update("unitPreferences.$key", value)
            .await()
    }

    suspend fun saveOnboardingProfile(
        userId: String,
        profile: com.fitproject.droid.data.onboarding.OnboardingProfile
    ) {
        val data = mutableMapOf<String, Any>(
            "onboardingProfile" to mapOf(
                "firstName" to profile.firstName,
                "goal" to (profile.goal?.name ?: ""),
                "gender" to (profile.gender?.name ?: ""),
                "dateOfBirth" to (profile.dateOfBirth?.time ?: 0L),
                "heightCm" to (profile.heightCm ?: 0.0),
                "weightKg" to (profile.weightKg ?: 0.0),
                "bodyFatPercent" to (profile.bodyFatPercent ?: 0.0),
                "bodyFatMethod" to (profile.bodyFatMethod?.name ?: ""),
                "muscleGoalKg" to profile.muscleGoalKg,
                "experience" to (profile.experience?.name ?: ""),
                "availableDays" to profile.availableDays.map { it.name },
                "daysPerWeek" to profile.daysPerWeek,
                "sessionMinutes" to profile.sessionMinutes,
                "musclePriority" to profile.musclePriority.name,
                "bodyFocus" to profile.bodyFocus.name,
                "workoutSplit" to profile.workoutSplit.name,
                "gymType" to profile.gymType.name,
                "equipment" to profile.equipment.map { it.name },
                "completedAt" to System.currentTimeMillis(),
                "skipped" to profile.skipped
            )
        )
        if (profile.firstName.isNotBlank()) {
            data["firstName"] = profile.firstName
        }
        db.collection("users").document(userId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    // MARK: - Programs

    suspend fun fetchAssignedPrograms(userId: String): List<FPAssignedProgram> {
        val fromUserIds = runCatching {
            db.collection("programs")
                .whereArrayContains("userIds", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val program = parseProgram(doc) ?: return@mapNotNull null
                    FPAssignedProgram(
                        id = "assigned-${doc.id}",
                        programId = doc.id,
                        userId = userId,
                        coachId = program.creatorIds.firstOrNull(),
                        program = program
                    )
                }
        }.getOrElse { emptyList() }

        if (fromUserIds.isNotEmpty()) return fromUserIds

        return runCatching {
            db.collection("assignedPrograms")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val programId = data["programId"] as? String ?: ""
                    var item = FPAssignedProgram(
                        id = doc.id,
                        programId = programId,
                        userId = userId,
                        coachId = data["coachId"] as? String,
                        startDate = (data["startDate"] as? Timestamp)?.toDate(),
                        currentWeek = (data["currentWeek"] as? Long)?.toInt() ?: 1,
                        completedWorkouts = (data["completedWorkouts"] as? Long)?.toInt() ?: 0
                    )
                    if (programId.isNotEmpty()) {
                        item = item.copy(program = runCatching { fetchProgram(programId) }.getOrNull())
                    }
                    item
                }
        }.getOrElse { emptyList() }
    }

    suspend fun fetchCreatorPrograms(userId: String): List<FPProgram> =
        runCatching {
            db.collection("programs")
                .whereArrayContains("creatorIds", userId)
                .get()
                .await()
                .documents
                .mapNotNull { parseProgram(it) }
        }.getOrElse { emptyList() }

    suspend fun fetchProgram(programId: String): FPProgram {
        val doc = db.collection("programs").document(programId).get().await()
        return parseProgram(doc) ?: throw FitProsError.NotFound("Program")
    }

    suspend fun fetchProgramWeeks(programId: String): List<FPProgramWeek> =
        runCatching {
            val snapshot = db.collection("programs").document(programId)
                .collection("programWeeks")
                .get()
                .await()

            snapshot.documents
                .map { doc ->
                    val data = doc.data ?: emptyMap()
                    FPProgramWeek(
                        id = doc.id,
                        name = data["name"] as? String ?: "Week",
                        index = (data["index"] as? Long)?.toInt() ?: 0,
                        programId = programId,
                        workouts = fetchWorkouts(programId, doc.id)
                    )
                }
                .sortedBy { it.index }
        }.getOrElse { emptyList() }

    suspend fun fetchWorkouts(programId: String, weekId: String): List<FPWorkout> =
        runCatching {
            db.collection("programs").document(programId)
                .collection("programWeeks").document(weekId)
                .collection("workouts")
                .get()
                .await()
                .documents
                .mapNotNull { parseWorkout(it, programId, weekId) }
                .sortedBy { it.index }
        }.getOrElse { emptyList() }

    suspend fun fetchWorkout(programId: String, weekId: String, workoutId: String): FPWorkout {
        val doc = db.collection("programs").document(programId)
            .collection("programWeeks").document(weekId)
            .collection("workouts").document(workoutId)
            .get()
            .await()
        return parseWorkout(doc, programId, weekId) ?: throw FitProsError.NotFound("Workout")
    }

    // MARK: - Workout Logs

    suspend fun fetchWorkoutLogs(userId: String, limit: Int = 50): List<FPWorkoutLog> =
        runCatching {
            db.collection("workoutLogs")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { parseWorkoutLog(it) }
                .sortedByDescending { it.startedAt }
                .take(limit)
        }.getOrElse { emptyList() }

    suspend fun saveWorkoutLog(log: FPWorkoutLog) {
        val data = mutableMapOf<String, Any>(
            "userId" to log.userId,
            "workoutId" to log.workoutId,
            "workoutName" to log.workoutName,
            "startedAt" to Timestamp(log.startedAt),
            "totalVolume" to log.totalVolume,
            "prCount" to log.prCount,
            "exercises" to log.exercises.map { exerciseToMap(it) }
        )
        log.programId?.let { data["programId"] = it }
        log.completedAt?.let { data["completedAt"] = Timestamp(it) }
        log.durationSeconds?.let { data["durationSeconds"] = it }
        log.notes?.let { data["notes"] = it }
        db.collection("workoutLogs").document(log.id).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    // MARK: - Habits

    suspend fun fetchHabits(userId: String): List<FPHabit> {
        val habits = fetchUserHabits(userId)
        val logs = fetchUserHabitLogsForDate(userId, Date())
        return mergeHabitsWithLogs(habits, logs)
    }

    suspend fun fetchUserHabits(userId: String): List<FPHabit> =
        runCatching {
            db.collection("userHabits")
                .whereEqualTo("client.id", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    if (data["active"] as? Boolean == false) return@mapNotNull null
                    parseUserHabit(doc)
                }
                .sortedWith(compareBy<FPHabit> { it.index }.thenBy { it.name })
        }.getOrElse { emptyList() }

    suspend fun fetchUserHabitLogsForDate(userId: String, date: Date): List<FPHabitLog> {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.time
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.time

        return runCatching {
            db.collection("userHabitLogs")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { parseUserHabitLog(it) }
                .filter { it.date in start..end }
        }.getOrElse { emptyList() }
    }

    fun mergeHabitsWithLogs(habits: List<FPHabit>, logs: List<FPHabitLog>): List<FPHabit> {
        val logByHabit = logs.associateBy { it.userHabitId }
        return habits.map { habit ->
            logByHabit[habit.id]?.let { log ->
                habit.copy(
                    currentValue = log.value,
                    targetMet = log.targetMet,
                    logDateCreated = log.dateCreated
                )
            } ?: habit
        }
    }

    suspend fun saveUserHabitLog(habit: FPHabit, userId: String, value: Double, forDate: Date = Date()) {
        val cal = Calendar.getInstance().apply {
            time = forDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.time
        val docId = "${habit.id}_${HabitSyncHelper.startOfDayUnix(forDate)}"
        val targetMet = HabitSyncHelper.isTargetMet(habit.targetType, habit.targetMin, habit.targetMax, value)
        val now = Timestamp.now()
        val data = mutableMapOf<String, Any>(
            "userId" to userId,
            "date" to Timestamp(startOfDay),
            "lastUpdated" to now,
            "value" to value,
            "targetMet" to targetMet,
            "userHabit" to embeddedUserHabitMap(habit)
        )
        data["dateCreated"] = habit.logDateCreated?.let { Timestamp(it) } ?: now
        db.collection("userHabitLogs").document(docId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    // MARK: - Progress Pictures

    suspend fun fetchProgressPictures(userId: String): List<FPProgressPicture> =
        runCatching {
            db.collection("progressPictures")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { parseProgressPicture(it) }
                .sortedByDescending { it.dateCreated }
                .take(100)
        }.getOrElse { emptyList() }

    fun groupProgressSessions(pictures: List<FPProgressPicture>): List<FPProgressSession> {
        val poseOrder = mapOf("front" to 0, "side" to 1, "back" to 2)
        return pictures.groupBy { it.sessionId }
            .map { (sessionId, items) ->
                val sorted = items.sortedBy { poseOrder[it.poseType] ?: 99 }
                FPProgressSession(
                    sessionId = sessionId,
                    dateCreated = sorted.first().dateCreated,
                    pictures = sorted,
                    notes = sorted.mapNotNull { it.notes }.firstOrNull { it.isNotEmpty() }
                )
            }
            .sortedByDescending { it.dateCreated }
    }

    suspend fun saveProgressSession(
        userId: String,
        sessionId: String,
        dateCreated: Date,
        notes: String?,
        poses: List<Triple<String, String, String?>>
    ) {
        val now = Date()
        val batch = db.batch()
        for ((poseType, imageUrl, existingId) in poses) {
            val docId = existingId ?: UUID.randomUUID().toString()
            val ref = db.collection("progressPictures").document(docId)
            batch.set(
                ref,
                mapOf(
                    "userId" to userId,
                    "sessionId" to sessionId,
                    "poseType" to poseType,
                    "imageUrl" to imageUrl,
                    "dateCreated" to Timestamp(dateCreated),
                    "lastUpdated" to Timestamp(now),
                    "notes" to (notes ?: "")
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
        val summary = if (!notes.isNullOrEmpty()) notes else "Progress picture added"
        val historyRef = db.collection("userHistory").document(sessionId)
        batch.set(
            historyRef,
            mapOf(
                "userId" to userId,
                "dateCreated" to Timestamp(dateCreated),
                "type" to "PROGRESS_PICTURE",
                "summary" to summary,
                "imageUrl" to poses.first().second
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.commit().await()
    }

    // MARK: - Measurements

    suspend fun fetchAllMeasurements(userId: String): List<FPMeasurement> {
        val logs = fetchMeasurementLogs(userId)
        val legacy = fetchLegacyMeasurements(userId)
        return mergeMeasurements(logs, legacy)
    }

    suspend fun fetchMeasurementLogs(userId: String): List<FPMeasurement> =
        db.collection("measurementLogs")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { parseMeasurementLog(it) }
            .sortedByDescending { it.date }

    suspend fun fetchLegacyMeasurements(userId: String): List<FPMeasurement> =
        db.collection("measurements")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val date = (data["date"] as? Timestamp)?.toDate() ?: return@mapNotNull null
                val name = data["name"] as? String ?: ""
                val catalog = MeasurementCatalog.findByName(name)
                FPMeasurement(
                    id = doc.id,
                    typeId = catalog?.id,
                    name = catalog?.name ?: name,
                    unit = catalog?.displayUnit ?: normalizeUnit(data["unit"] as? String),
                    value = (data["value"] as? Number)?.toDouble() ?: 0.0,
                    date = date,
                    notes = data["notes"] as? String,
                    source = "measurements"
                )
            }

    suspend fun saveMeasurement(measurement: FPMeasurement, userId: String) {
        val type = MeasurementCatalog.findById(measurement.typeId)
            ?: MeasurementCatalog.findByName(measurement.name)
            ?: throw FitProsError.SyncFailed("Unknown measurement type: ${measurement.name}")

        val now = Date()
        val sessionId = measurement.sessionId ?: UUID.randomUUID().toString()
        db.collection("measurementLogs").document(measurement.id).set(
            mapOf(
                "userId" to userId,
                "dateCreated" to Timestamp(measurement.date),
                "lastUpdated" to Timestamp(now),
                "notes" to (measurement.notes ?: ""),
                "value" to "%.2f".format(measurement.value),
                "numericValue" to measurement.value,
                "sessionId" to sessionId,
                "measurement" to measurementTypeMap(type)
            )
        ).await()
    }

    // MARK: - Content

    suspend fun fetchContent(userId: String): List<FPContent> =
        runCatching {
            db.collection("content")
                .whereArrayContains("userIds", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    FPContent(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        body = data["body"] as? String,
                        imageUrl = data["imageUrl"] as? String,
                        type = data["type"] as? String ?: "article",
                        dateCreated = (data["dateCreated"] as? Timestamp)?.toDate()
                    )
                }
        }.getOrElse { emptyList() }

    // MARK: - Personal Records

    suspend fun fetchPersonalRecords(userId: String): List<FPPersonalRecord> =
        runCatching {
            db.collection("personalRecords")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val date = (data["date"] as? Timestamp)?.toDate() ?: return@mapNotNull null
                    FPPersonalRecord(
                        id = doc.id,
                        exerciseId = data["exerciseId"] as? String ?: "",
                        exerciseName = data["exerciseName"] as? String ?: "",
                        metric = data["metric"] as? String ?: "",
                        value = data["value"] as? String ?: "",
                        date = date,
                        previousValue = data["previousValue"] as? String
                    )
                }
                .sortedByDescending { it.date }
        }.getOrElse { emptyList() }

    suspend fun savePersonalRecord(record: FPPersonalRecord, userId: String) {
        val data = mutableMapOf<String, Any>(
            "userId" to userId,
            "exerciseId" to record.exerciseId,
            "exerciseName" to record.exerciseName,
            "metric" to record.metric,
            "value" to record.value,
            "date" to Timestamp(record.date)
        )
        record.previousValue?.let { data["previousValue"] = it }
        db.collection("personalRecords").document(record.id).set(data).await()
    }

    // MARK: - Forms

    suspend fun fetchForms(userId: String): List<FPForm> =
        runCatching {
            db.collection("forms")
                .whereArrayContains("clientIds", userId)
                .get()
                .await()
                .documents
                .mapNotNull { parseForm(it) }
        }.getOrElse { emptyList() }

    suspend fun submitForm(formId: String, clientId: String, answers: List<FPFormAnswer>) {
        val ref = db.collection("forms").document(formId)
        val doc = ref.get().await()
        val data = doc.data ?: throw FitProsError.NotFound("Form")
        val submissions = parseFormSubmissions(data).toMutableList()
        submissions.add(FPFormSubmission(clientId = clientId, submittedAt = Date(), answers = answers))
        val newResponses = ((data["newResponses"] as? Long)?.toInt() ?: 0) + 1
        ref.update(
            mapOf(
                "submissions" to submissions.map { submissionToMap(it) },
                "newResponses" to newResponses
            )
        ).await()
    }

    // MARK: - Real-time Listeners

    fun listenToWorkoutLogs(userId: String, onChange: (List<FPWorkoutLog>) -> Unit): ListenerRegistration =
        db.collection("workoutLogs")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val logs = snapshot?.documents
                    ?.mapNotNull { parseWorkoutLog(it) }
                    ?.sortedByDescending { it.startedAt }
                    ?.take(50)
                    ?: emptyList()
                onChange(logs)
            }

    fun listenToHabits(userId: String, onChange: (List<FPHabit>) -> Unit): ListenerRegistration =
        db.collection("userHabits")
            .whereEqualTo("client.id", userId)
            .addSnapshotListener { _, _ ->
                listenerScope.launch {
                    val habits = runCatching { fetchHabits(userId) }.getOrDefault(emptyList())
                    withContext(Dispatchers.Main) {
                        onChange(habits)
                    }
                }
            }

    // MARK: - Parsing

    private fun parseProgram(doc: DocumentSnapshot): FPProgram? {
        val data = doc.data ?: return null
        return FPProgram(
            id = doc.id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            imageUrl = data["imageUrl"] as? String,
            totalWeekCount = (data["totalWeekCount"] as? Long)?.toInt() ?: 0,
            totalWorkoutCount = (data["totalWorkoutCount"] as? Long)?.toInt() ?: 0,
            published = data["published"] as? Boolean ?: false,
            creatorIds = (data["creatorIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseWorkout(doc: DocumentSnapshot, programId: String, weekId: String): FPWorkout? {
        val data = doc.data ?: return null
        val groups = (data["workoutExerciseGroups"] as? List<Map<String, Any>> ?: emptyList()).map { g ->
            FPExerciseGroup(
                id = g["id"] as? String ?: UUID.randomUUID().toString(),
                name = g["name"] as? String ?: "",
                index = (g["index"] as? Long)?.toInt() ?: 0,
                type = g["type"] as? String
            )
        }
        val exercises = (data["workoutExercises"] as? List<Map<String, Any>> ?: emptyList()).map { e ->
            FPWorkoutExercise(
                id = e["id"] as? String ?: UUID.randomUUID().toString(),
                name = e["name"] as? String ?: "",
                exerciseId = e["exerciseId"] as? String ?: "",
                youtubeId = e["youtubeId"] as? String,
                thumbnailUrl = e["thumbnailUrl"] as? String,
                index = (e["index"] as? Long)?.toInt() ?: 0,
                sets = (e["sets"] as? Long)?.toInt() ?: 0,
                coachNotes = e["coachNotes"] as? String,
                header = e["header"] as? String,
                headerVisible = e["headerVisible"] as? Boolean ?: false,
                groupId = e["workoutExerciseGroupId"] as? String,
                isSuperset = e["isSuperset"] as? Boolean ?: false
            )
        }
        val metrics = (data["workoutExerciseMetrics"] as? List<Map<String, Any>> ?: emptyList()).map { m ->
            val unit = m["unitOfMeasurement"] as? Map<String, Any>
            FPWorkoutMetric(
                id = m["id"] as? String ?: UUID.randomUUID().toString(),
                name = m["name"] as? String ?: "",
                index = (m["index"] as? Long)?.toInt() ?: 0,
                color = m["color"] as? String ?: "#ffffff",
                exerciseMetricId = m["exerciseMetricId"] as? String ?: "",
                workoutExerciseId = m["workoutExerciseId"] as? String ?: "",
                unitAbbreviation = unit?.get("abbreviation") as? String
            )
        }
        val values = (data["workoutExerciseMetricValues"] as? List<Map<String, Any>> ?: emptyList()).map { v ->
            FPMetricValue(
                id = v["id"] as? String ?: UUID.randomUUID().toString(),
                workoutExerciseId = v["workoutExerciseId"] as? String ?: "",
                workoutExerciseMetricId = v["workoutExerciseMetricId"] as? String ?: "",
                exerciseMetricId = v["exerciseMetricId"] as? String ?: "",
                exerciseId = v["exerciseId"] as? String ?: "",
                set = (v["set"] as? Long)?.toInt() ?: 1,
                index = (v["index"] as? Long)?.toInt() ?: 0,
                value = v["value"] as? String ?: "",
                loggedValue = v["loggedValue"] as? String,
                isCompleted = v["isCompleted"] as? Boolean ?: false
            )
        }
        return FPWorkout(
            id = doc.id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String,
            index = (data["index"] as? Long)?.toInt() ?: 0,
            programId = programId,
            programWeekId = weekId,
            notes = data["notes"] as? String,
            exerciseGroups = groups,
            exercises = exercises,
            metrics = metrics,
            metricValues = values
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseWorkoutLog(doc: DocumentSnapshot): FPWorkoutLog? {
        val data = doc.data ?: return null
        val startedAt = (data["startedAt"] as? Timestamp)?.toDate()
            ?: (data["startDate"] as? Timestamp)?.toDate()
            ?: return null
        val exercises = (data["exercises"] as? List<Map<String, Any>> ?: emptyList()).map { e ->
            val sets = (e["sets"] as? List<Map<String, Any>> ?: emptyList()).map { s ->
                FPLoggedSet(
                    id = s["id"] as? String ?: UUID.randomUUID().toString(),
                    setNumber = (s["setNumber"] as? Long)?.toInt() ?: 1,
                    reps = s["reps"] as? String,
                    weight = s["weight"] as? String,
                    rpe = s["rpe"] as? String,
                    rest = s["rest"] as? String,
                    tempo = s["tempo"] as? String,
                    time = s["time"] as? String,
                    isCompleted = s["isCompleted"] as? Boolean ?: false,
                    isPR = s["isPR"] as? Boolean ?: false
                )
            }
            FPLoggedExercise(
                id = e["id"] as? String ?: UUID.randomUUID().toString(),
                exerciseId = e["exerciseId"] as? String ?: "",
                name = e["name"] as? String ?: "",
                sets = sets
            )
        }
        return FPWorkoutLog(
            id = doc.id,
            userId = data["userId"] as? String ?: "",
            workoutId = data["workoutId"] as? String ?: "",
            programId = data["programId"] as? String,
            workoutName = data["workoutName"] as? String ?: "",
            startedAt = startedAt,
            completedAt = (data["completedAt"] as? Timestamp)?.toDate(),
            durationSeconds = (data["durationSeconds"] as? Long)?.toInt(),
            exercises = exercises,
            notes = data["notes"] as? String,
            totalVolume = (data["totalVolume"] as? Number)?.toDouble() ?: 0.0,
            prCount = (data["prCount"] as? Long)?.toInt() ?: 0
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseForm(doc: DocumentSnapshot): FPForm? {
        val data = doc.data ?: return null
        return FPForm(
            id = doc.id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String,
            creatorId = data["creatorId"] as? String,
            fields = parseFormFields(data),
            submissions = parseFormSubmissions(data),
            dueDate = (data["dueDate"] as? Timestamp)?.toDate()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFormFields(data: Map<String, Any>): List<FPFormField> =
        (data["fields"] as? List<Map<String, Any>> ?: emptyList()).map { field ->
            FPFormField(
                id = field["id"] as? String ?: UUID.randomUUID().toString(),
                type = field["type"] as? String ?: "Text",
                question = field["question"] as? String ?: "",
                required = field["required"] as? Boolean ?: false,
                index = (field["index"] as? Long)?.toInt() ?: 0,
                maxRating = ((field["maxRating"] as? Long)?.toInt()?.takeIf { it > 0 }) ?: 5,
                scaleMin = ((field["scaleMin"] as? Long)?.toInt()?.takeIf { it > 0 }) ?: 1,
                scaleMax = ((field["scaleMax"] as? Long)?.toInt()?.takeIf { it > 0 }) ?: 10,
                scaleMinLabel = field["scaleMinLabel"] as? String,
                scaleMaxLabel = field["scaleMaxLabel"] as? String,
                options = (field["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }.sortedBy { it.index }

    @Suppress("UNCHECKED_CAST")
    private fun parseFormSubmissions(data: Map<String, Any>): List<FPFormSubmission> {
        val raw = (data["submissions"] ?: data["responses"]) as? List<Map<String, Any>> ?: emptyList()
        return raw.mapNotNull { submission ->
            val answers = (submission["answers"] as? List<Map<String, Any>> ?: emptyList()).map { answer ->
                FPFormAnswer(
                    fieldId = answer["fieldId"] as? String ?: "",
                    question = answer["question"] as? String ?: "",
                    type = answer["type"] as? String ?: "",
                    value = answer["value"] as? String ?: ""
                )
            }
            FPFormSubmission(
                clientId = submission["clientId"] as? String ?: "",
                submittedAt = (submission["submittedAt"] as? Timestamp)?.toDate() ?: Date(),
                answers = answers
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUserHabit(doc: DocumentSnapshot): FPHabit? {
        val data = doc.data ?: return null
        val (values, unit, frequency, explicitType) = parseHabitTarget(data["target"] as? Map<String, Any>)
        val first = values.firstOrNull() ?: return null
        val targetType = HabitSyncHelper.targetType(values, explicitType)
        val targetMax = if (values.size > 1) values[1] else first
        val iconData = data["icon"] as? Map<String, Any>
        return FPHabit(
            id = doc.id,
            habitId = data["habitId"] as? String ?: doc.id.split("_").firstOrNull() ?: doc.id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String,
            unit = unit,
            frequency = frequency,
            targetType = targetType,
            targetMin = first,
            targetMax = targetMax,
            icon = iconData?.get("mobileIcon") as? String ?: iconData?.get("name") as? String,
            color = data["color"] as? String,
            index = (data["index"] as? Long)?.toInt() ?: 0,
            coachId = (data["coach"] as? Map<String, Any>)?.get("id") as? String
                ?: (data["client"] as? Map<String, Any>)?.get("id") as? String ?: ""
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUserHabitLog(doc: DocumentSnapshot): FPHabitLog? {
        val data = doc.data ?: return null
        val userHabit = data["userHabit"] as? Map<String, Any>
        return FPHabitLog(
            id = doc.id,
            userHabitId = userHabit?.get("id") as? String ?: "",
            userId = data["userId"] as? String ?: "",
            date = (data["date"] as? Timestamp)?.toDate() ?: Date(),
            value = numericValue(data["value"]),
            targetMet = data["targetMet"] as? Boolean ?: false,
            dateCreated = (data["dateCreated"] as? Timestamp)?.toDate()
        )
    }

    private fun parseProgressPicture(doc: DocumentSnapshot): FPProgressPicture? {
        val data = doc.data ?: return null
        val dateCreated = (data["dateCreated"] as? Timestamp)?.toDate() ?: return null
        val imageUrl = data["imageUrl"] as? String ?: return null
        return FPProgressPicture(
            id = doc.id,
            userId = data["userId"] as? String ?: "",
            sessionId = data["sessionId"] as? String ?: doc.id,
            poseType = data["poseType"] as? String ?: "front",
            imageUrl = imageUrl,
            dateCreated = dateCreated,
            notes = data["notes"] as? String
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMeasurementLog(doc: DocumentSnapshot): FPMeasurement? {
        val data = doc.data ?: return null
        val date = (data["dateCreated"] as? Timestamp)?.toDate()
            ?: (data["lastUpdated"] as? Timestamp)?.toDate()
            ?: return null
        var value = (data["numericValue"] as? Number)?.toDouble() ?: 0.0
        if (value == 0.0) {
            value = (data["value"] as? String)?.toDoubleOrNull() ?: 0.0
        }
        var name = ""
        var typeId: String? = null
        var unit = ""
        val measurement = data["measurement"] as? Map<String, Any>
        if (measurement != null) {
            typeId = measurement["id"] as? String
            name = measurement["name"] as? String ?: ""
            val unitData = measurement["unitOfMeasurement"] as? Map<String, Any>
            unit = normalizeUnit(unitData?.get("abbreviation") as? String)
        }
        val catalog = MeasurementCatalog.findById(typeId) ?: MeasurementCatalog.findByName(name)
        if (catalog != null) {
            typeId = typeId ?: catalog.id
            if (name.isEmpty()) name = catalog.name
            unit = catalog.displayUnit
        }
        return FPMeasurement(
            id = doc.id,
            typeId = typeId,
            name = name,
            unit = unit,
            value = value,
            date = date,
            notes = data["notes"] as? String,
            sessionId = data["sessionId"] as? String,
            source = "measurementLogs"
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseHabitTarget(target: Map<String, Any>?): HabitTargetParse {
        if (target == null) return HabitTargetParse(emptyList(), "", "Per Day", null)
        val rawValues = target["value"] as? List<*> ?: emptyList<Any>()
        val values = rawValues.map { numericValue(it) }
        return HabitTargetParse(
            values = values,
            unit = target["unit"] as? String ?: "",
            frequency = target["frequency"] as? String ?: "Per Day",
            explicitType = target["targetType"] as? String
        )
    }

    private data class HabitTargetParse(
        val values: List<Double>,
        val unit: String,
        val frequency: String,
        val explicitType: String?
    )

    private fun embeddedUserHabitMap(habit: FPHabit): Map<String, Any> {
        val values = if (habit.targetType == "RANGE") listOf(habit.targetMin, habit.targetMax) else listOf(habit.targetMin)
        return mapOf(
            "id" to habit.id,
            "coach" to mapOf("id" to habit.coachId),
            "target" to mapOf(
                "unit" to habit.unit,
                "frequency" to habit.frequency,
                "value" to values
            )
        )
    }

    private fun numericValue(value: Any?): Double = when (value) {
        is Double -> value
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is Float -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private fun exerciseToMap(exercise: FPLoggedExercise): Map<String, Any> = mapOf(
        "id" to exercise.id,
        "exerciseId" to exercise.exerciseId,
        "name" to exercise.name,
        "sets" to exercise.sets.map { set ->
            val dict = mutableMapOf<String, Any>(
                "id" to set.id,
                "setNumber" to set.setNumber,
                "isCompleted" to set.isCompleted,
                "isPR" to set.isPR
            )
            set.reps?.let { dict["reps"] = it }
            set.weight?.let { dict["weight"] = it }
            set.rpe?.let { dict["rpe"] = it }
            set.rest?.let { dict["rest"] = it }
            set.tempo?.let { dict["tempo"] = it }
            set.time?.let { dict["time"] = it }
            dict
        }
    )

    private fun submissionToMap(submission: FPFormSubmission): Map<String, Any> = mapOf(
        "clientId" to submission.clientId,
        "submittedAt" to Timestamp(submission.submittedAt),
        "answers" to submission.answers.map { answer ->
            mapOf(
                "fieldId" to answer.fieldId,
                "question" to answer.question,
                "type" to answer.type,
                "value" to answer.value
            )
        }
    )

    private fun measurementTypeMap(type: FPMeasurementTypeDef): Map<String, Any> = mapOf(
        "id" to type.id,
        "name" to type.name,
        "color" to type.color,
        "unitOfMeasurement" to mapOf(
            "id" to type.unitId,
            "name" to type.unitName,
            "type" to type.unitType,
            "abbreviation" to type.unitAbbreviation
        )
    )

    private fun mergeMeasurements(logs: List<FPMeasurement>, legacy: List<FPMeasurement>): List<FPMeasurement> {
        val keysWithLogs = logs.map { it.matchKey }.toSet()
        val merged = logs.toMutableList()
        legacy.filter { it.matchKey !in keysWithLogs }.forEach { merged.add(it) }
        return merged.sortedByDescending { it.date }
    }

    private fun normalizeUnit(unit: String?): String = when (unit) {
        "centimeter" -> "cm"
        "inches", "in", "inch" -> "cm"
        else -> unit ?: ""
    }

    companion object {
        val shared: FirestoreService by lazy { FirestoreService() }
    }
}