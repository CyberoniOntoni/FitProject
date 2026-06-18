package com.fitproject.droid.data

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Date

data class FullSyncResult(
    val programs: List<FPProgram>,
    val programWeeks: Map<String, List<FPProgramWeek>>,
    val assignedPrograms: List<FPAssignedProgram>,
    val workoutLogs: List<FPWorkoutLog>,
    val habits: List<FPHabit>,
    val allProgressPictures: List<FPProgressPicture>,
    val progressSessions: List<FPProgressSession>,
    val measurements: List<FPMeasurement>,
    val content: List<FPContent>,
    val personalRecords: List<FPPersonalRecord>,
    val forms: List<FPForm>,
    val unitPreferences: FPUnitPreferences,
    val userProfile: FPUser
)

interface SyncStateCallbacks {
    fun onWorkoutLogsUpdated(logs: List<FPWorkoutLog>)
    fun onHabitsUpdated(habits: List<FPHabit>)
    fun onFullSyncResult(result: FullSyncResult)
    fun onSyncingChanged(isSyncing: Boolean)
    fun onLastSyncDateChanged(date: Date?)
    fun onSyncError(error: String?)
    fun onFormsUpdated(forms: List<FPForm>)
    fun onUnitPreferencesUpdated(prefs: FPUnitPreferences, user: FPUser?)
    fun onProgressPicturesUpdated(
        pictures: List<FPProgressPicture>,
        sessions: List<FPProgressSession>
    )
}

class SyncEngine {
    private val firestore = FirestoreService.shared
    private val storage = StorageService.shared
    private val listeners = mutableListOf<ListenerRegistration>()

    var isSyncing: Boolean = false
        private set
    var lastSyncDate: Date? = null
        private set
    var syncError: String? = null
        private set

    fun startSync(userId: String, callbacks: SyncStateCallbacks) {
        stopSync()

        val logsListener = firestore.listenToWorkoutLogs(userId) { logs ->
            callbacks.onWorkoutLogsUpdated(logs)
        }
        listeners.add(logsListener)

        val habitsListener = firestore.listenToHabits(userId) { habits ->
            callbacks.onHabitsUpdated(habits)
        }
        listeners.add(habitsListener)
    }

    fun stopSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    suspend fun fullSync(userId: String, callbacks: SyncStateCallbacks) {
        isSyncing = true
        syncError = null
        callbacks.onSyncingChanged(true)
        callbacks.onSyncError(null)

        val syncErrors = mutableListOf<String>()
        try {
            coroutineScope {
                val assignedDeferred = async { firestore.fetchAssignedPrograms(userId) }
                val creatorDeferred = async { firestore.fetchCreatorPrograms(userId) }
                val logsDeferred = async { firestore.fetchWorkoutLogs(userId) }
                val habitsDeferred = async { firestore.fetchHabits(userId) }
                val picturesDeferred = async { firestore.fetchProgressPictures(userId) }
                val measurementsDeferred = async { firestore.fetchAllMeasurements(userId) }
                val contentDeferred = async { firestore.fetchContent(userId) }
                val recordsDeferred = async { firestore.fetchPersonalRecords(userId) }
                val formsDeferred = async { firestore.fetchForms(userId) }
                val profileDeferred = async {
                    runCatching { firestore.fetchUserProfile(userId) }
                        .onFailure { syncErrors.add(it.message ?: "Failed to load profile") }
                        .getOrNull()
                }

                val assigned = assignedDeferred.await()
                val creator = creatorDeferred.await()
                val logs = logsDeferred.await()
                val habitsResult = habitsDeferred.await()
                val pictures = picturesDeferred.await()
                val meas = measurementsDeferred.await()
                val cont = contentDeferred.await()
                val recs = recordsDeferred.await()
                val formsResult = formsDeferred.await()
                val userProfile = profileDeferred.await()

                val allPrograms = creator.toMutableList()
                for (item in assigned) {
                    val program = item.program ?: continue
                    if (allPrograms.none { it.id == program.id }) {
                        allPrograms.add(program.copy(completedWorkouts = item.completedWorkouts))
                    }
                }

                val weeksByProgram = mutableMapOf<String, List<FPProgramWeek>>()
                for (program in allPrograms) {
                    weeksByProgram[program.id] = firestore.fetchProgramWeeks(program.id)
                }

                if (allPrograms.isEmpty() && syncErrors.isNotEmpty()) {
                    throw IllegalStateException(syncErrors.first())
                }

                val result = FullSyncResult(
                    programs = allPrograms,
                    programWeeks = weeksByProgram,
                    assignedPrograms = assigned,
                    workoutLogs = logs,
                    habits = habitsResult,
                    allProgressPictures = pictures,
                    progressSessions = firestore.groupProgressSessions(pictures),
                    measurements = meas,
                    content = cont,
                    personalRecords = recs,
                    forms = formsResult,
                    unitPreferences = userProfile?.unitPreferences ?: FPUnitPreferences(),
                    userProfile = userProfile ?: FPUser(id = userId)
                )
                callbacks.onFullSyncResult(result)
                syncError = null
                callbacks.onSyncError(null)
            }
        } catch (e: Exception) {
            syncError = e.message
            callbacks.onSyncError(e.message)
        } finally {
            isSyncing = false
            lastSyncDate = Date()
            callbacks.onSyncingChanged(false)
            callbacks.onLastSyncDateChanged(lastSyncDate)
        }
    }

    suspend fun pushWorkoutLog(log: FPWorkoutLog) {
        firestore.saveWorkoutLog(log)
    }

    suspend fun pushHabitUpdate(habit: FPHabit, userId: String, value: Double) {
        firestore.saveUserHabitLog(habit, userId, value)
    }

    suspend fun pushMeasurement(measurement: FPMeasurement, userId: String) {
        firestore.saveMeasurement(measurement, userId)
    }

    suspend fun pushPersonalRecord(record: FPPersonalRecord, userId: String) {
        firestore.savePersonalRecord(record, userId)
    }

    suspend fun pushFormSubmission(
        formId: String,
        userId: String,
        answers: List<FPFormAnswer>,
        callbacks: SyncStateCallbacks,
        currentForms: List<FPForm>
    ) {
        firestore.submitForm(formId, userId, answers)
        val updated = currentForms.map { form ->
            if (form.id == formId) {
                form.copy(
                    submissions = form.submissions + FPFormSubmission(
                        clientId = userId,
                        submittedAt = Date(),
                        answers = answers
                    )
                )
            } else form
        }
        callbacks.onFormsUpdated(updated)
    }

    suspend fun pushUnitPreference(
        userId: String,
        key: String,
        value: String,
        callbacks: SyncStateCallbacks,
        currentPrefs: FPUnitPreferences,
        currentUser: FPUser?
    ) {
        firestore.updateUnitPreference(userId, key, value)
        val prefs = when (key) {
            "weight" -> currentPrefs.copy(weight = value)
            "mass" -> currentPrefs.copy(mass = value)
            "circumference" -> currentPrefs.copy(circumference = value)
            "distance" -> currentPrefs.copy(distance = value)
            "time" -> currentPrefs.copy(time = value)
            else -> currentPrefs
        }
        val user = currentUser?.copy(unitPreferences = prefs)
        callbacks.onUnitPreferencesUpdated(prefs, user)
    }

    suspend fun pushProgressPhotoSession(
        draft: FPProgressPhotoDraft,
        userId: String,
        callbacks: SyncStateCallbacks,
        currentPictures: List<FPProgressPicture>
    ) {
        val poses = mutableListOf<Triple<String, String, String?>>()
        for ((poseType, payload) in draft.poseImageData) {
            val imageUrl = storage.uploadProgressPhoto(
                userId = userId,
                sessionId = draft.sessionId,
                poseType = poseType,
                data = payload.data,
                contentType = payload.contentType,
                fileExtension = payload.fileExtension
            )
            poses.add(Triple(poseType, imageUrl, null))
        }

        firestore.saveProgressSession(
            userId = userId,
            sessionId = draft.sessionId,
            dateCreated = draft.dateCreated,
            notes = draft.notes.ifEmpty { null },
            poses = poses
        )

        val pictures = poses.map { (poseType, imageUrl, _) ->
            FPProgressPicture(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                sessionId = draft.sessionId,
                poseType = poseType,
                imageUrl = imageUrl,
                dateCreated = draft.dateCreated,
                notes = draft.notes.ifEmpty { null }
            )
        }
        val allPictures = pictures + currentPictures
        callbacks.onProgressPicturesUpdated(
            allPictures,
            firestore.groupProgressSessions(allPictures)
        )
    }
}