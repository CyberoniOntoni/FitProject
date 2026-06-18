package com.fitproject.droid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitproject.droid.data.AppTab
import com.fitproject.droid.data.FPContent
import com.fitproject.droid.data.FPForm
import com.fitproject.droid.data.FPFormAnswer
import com.fitproject.droid.data.FPLoggedSet
import com.fitproject.droid.data.FPHabit
import com.fitproject.droid.data.FPLoggedExercise
import com.fitproject.droid.data.FPMeasurement
import com.fitproject.droid.data.FPPersonalRecord
import com.fitproject.droid.data.FPProgram
import com.fitproject.droid.data.FPProgressPhotoDraft
import com.fitproject.droid.data.FPProgressPicture
import com.fitproject.droid.data.FPProgressSession
import com.fitproject.droid.data.FPProgramWeek
import com.fitproject.droid.data.FPUnitPreferences
import com.fitproject.droid.data.FPUser
import com.fitproject.droid.data.FPWorkout
import com.fitproject.droid.data.FPWorkoutLog
import com.fitproject.droid.data.FullSyncResult
import com.fitproject.droid.data.SyncEngine
import com.fitproject.droid.data.SyncStateCallbacks
import com.fitproject.droid.data.WorkoutSessionState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application), SyncStateCallbacks {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val syncEngine = SyncEngine()

    // Navigation & tabs
    private val _selectedTab = MutableStateFlow(AppTab.TRAIN)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    // App data
    private val _programs = MutableStateFlow<List<FPProgram>>(emptyList())
    val programs: StateFlow<List<FPProgram>> = _programs.asStateFlow()

    private val _programWeeks = MutableStateFlow<Map<String, List<FPProgramWeek>>>(emptyMap())
    val programWeeks: StateFlow<Map<String, List<FPProgramWeek>>> = _programWeeks.asStateFlow()

    private val _workoutLogs = MutableStateFlow<List<FPWorkoutLog>>(emptyList())
    val workoutLogs: StateFlow<List<FPWorkoutLog>> = _workoutLogs.asStateFlow()

    private val _habits = MutableStateFlow<List<FPHabit>>(emptyList())
    val habits: StateFlow<List<FPHabit>> = _habits.asStateFlow()

    private val _progressSessions = MutableStateFlow<List<FPProgressSession>>(emptyList())
    val progressSessions: StateFlow<List<FPProgressSession>> = _progressSessions.asStateFlow()

    private val _allProgressPictures = MutableStateFlow<List<FPProgressPicture>>(emptyList())
    val allProgressPictures: StateFlow<List<FPProgressPicture>> = _allProgressPictures.asStateFlow()

    private val _unitPreferences = MutableStateFlow(FPUnitPreferences())
    val unitPreferences: StateFlow<FPUnitPreferences> = _unitPreferences.asStateFlow()

    private val _measurements = MutableStateFlow<List<FPMeasurement>>(emptyList())
    val measurements: StateFlow<List<FPMeasurement>> = _measurements.asStateFlow()

    private val _content = MutableStateFlow<List<FPContent>>(emptyList())
    val content: StateFlow<List<FPContent>> = _content.asStateFlow()

    private val _personalRecords = MutableStateFlow<List<FPPersonalRecord>>(emptyList())
    val personalRecords: StateFlow<List<FPPersonalRecord>> = _personalRecords.asStateFlow()

    private val _forms = MutableStateFlow<List<FPForm>>(emptyList())
    val forms: StateFlow<List<FPForm>> = _forms.asStateFlow()

    // Workout selection
    private val _nextWorkout = MutableStateFlow<FPWorkout?>(null)
    val nextWorkout: StateFlow<FPWorkout?> = _nextWorkout.asStateFlow()

    private val _nextProgram = MutableStateFlow<FPProgram?>(null)
    val nextProgram: StateFlow<FPProgram?> = _nextProgram.asStateFlow()

    private val _weeklyWorkoutGoal = MutableStateFlow(4)
    val weeklyWorkoutGoal: StateFlow<Int> = _weeklyWorkoutGoal.asStateFlow()

    private val _weeklyWorkoutsCompleted = MutableStateFlow(0)
    val weeklyWorkoutsCompleted: StateFlow<Int> = _weeklyWorkoutsCompleted.asStateFlow()

    private val _activeWorkoutSession = MutableStateFlow<WorkoutSessionState?>(null)
    val activeWorkoutSession: StateFlow<WorkoutSessionState?> = _activeWorkoutSession.asStateFlow()

    // Auth
    private val _currentUser = MutableStateFlow<FPUser?>(null)
    val currentUser: StateFlow<FPUser?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncDate = MutableStateFlow<Date?>(null)
    val lastSyncDate: StateFlow<Date?> = _lastSyncDate.asStateFlow()

    // Overlays
    private val _showProfileSheet = MutableStateFlow(false)
    val showProfileSheet: StateFlow<Boolean> = _showProfileSheet.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            _isAuthenticated.value = firebaseUser != null
            if (firebaseUser != null) {
                if (_currentUser.value?.id != firebaseUser.uid) {
                    _currentUser.value = FPUser(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "",
                        lastName = firebaseUser.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
                    )
                }
            } else {
                _currentUser.value = null
            }
        }
    }

    fun setSelectedTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setShowProfileSheet(show: Boolean) {
        _showProfileSheet.value = show
    }

    fun loadData() {
        val userId = _currentUser.value?.id ?: return
        syncEngine.startSync(userId, this)
        viewModelScope.launch {
            syncEngine.fullSync(userId, this@AppViewModel)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw IllegalStateException("No user returned")
                val profile = com.fitproject.droid.data.FirestoreService.shared.fetchUserProfile(userId)
                _currentUser.value = profile
                _isAuthenticated.value = true
                loadData()
            } catch (e: Exception) {
                _authError.value = mapAuthError(e)
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun signOut() {
        syncEngine.stopSync()
        _programs.value = emptyList()
        _programWeeks.value = emptyMap()
        _workoutLogs.value = emptyList()
        _habits.value = emptyList()
        _progressSessions.value = emptyList()
        _allProgressPictures.value = emptyList()
        _unitPreferences.value = FPUnitPreferences()
        _measurements.value = emptyList()
        _content.value = emptyList()
        _personalRecords.value = emptyList()
        _forms.value = emptyList()
        _activeWorkoutSession.value = null
        auth.signOut()
        _currentUser.value = null
        _isAuthenticated.value = false
    }

    fun updateWeeklyProgress() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfWeek = cal.time
        _weeklyWorkoutsCompleted.value = _workoutLogs.value.count { log ->
            log.isCompleted && (log.completedAt?.let { it >= startOfWeek } == true)
        }
    }

    fun selectNextWorkout() {
        val completedIds = _workoutLogs.value.filter { it.isCompleted }.map { it.workoutId }.toSet()
        for (program in _programs.value) {
            val weeks = _programWeeks.value[program.id] ?: continue
            for (week in weeks.sortedBy { it.index }) {
                for (workout in week.workouts.sortedBy { it.index }) {
                    if (workout.id !in completedIds) {
                        _nextWorkout.value = workout
                        _nextProgram.value = program
                        return
                    }
                }
            }
        }
        _nextWorkout.value = null
        _nextProgram.value = _programs.value.firstOrNull()
    }

    fun startWorkout(workout: FPWorkout, program: FPProgram?) {
        val loggedSets = mutableMapOf<String, List<FPLoggedSet>>()
        for (exercise in workout.exercises) {
            val exerciseMetrics = workout.metrics.filter { it.workoutExerciseId == exercise.id }
            val setCount = if (exercise.sets > 0) exercise.sets else 3
            loggedSets[exercise.id] = (1..setCount).map { setNum ->
                var set = FPLoggedSet(id = UUID.randomUUID().toString(), setNumber = setNum)
                for (metric in exerciseMetrics) {
                    val preset = workout.metricValues.find {
                        it.workoutExerciseId == exercise.id &&
                            it.workoutExerciseMetricId == metric.id &&
                            it.set == setNum
                    } ?: continue
                    when (metric.name) {
                        "Reps" -> set = set.copy(reps = preset.value)
                        "Weight" -> set = set.copy(weight = preset.value)
                        "RPE" -> set = set.copy(rpe = preset.value)
                        "Rest" -> set = set.copy(rest = preset.value)
                        "Tempo" -> set = set.copy(tempo = preset.value)
                        "Time" -> set = set.copy(time = preset.value)
                    }
                }
                set
            }
        }

        _activeWorkoutSession.value = WorkoutSessionState(
            workout = workout,
            program = program,
            startedAt = Date(),
            loggedSets = loggedSets
        )
    }

    fun updateWorkoutSession(session: WorkoutSessionState) {
        _activeWorkoutSession.value = session
    }

    fun completeWorkout() {
        val session = _activeWorkoutSession.value ?: return
        val userId = _currentUser.value?.id ?: return

        viewModelScope.launch {
            val endDate = Date()
            val elapsedSeconds = ((endDate.time - session.startedAt.time) / 1000).toInt()

            var totalVolume = 0.0
            var prCount = 0
            val loggedExercises = mutableListOf<FPLoggedExercise>()

            for (exercise in session.workout.exercises) {
                val sets = session.loggedSets[exercise.id] ?: emptyList()
                for (set in sets.filter { it.isCompleted }) {
                    val weight = set.weight?.toDoubleOrNull()
                    val reps = set.reps?.toDoubleOrNull()
                    if (weight != null && reps != null) {
                        totalVolume += weight * reps
                    }
                    if (set.isPR) prCount++
                }
                loggedExercises.add(
                    FPLoggedExercise(
                        id = exercise.id,
                        exerciseId = exercise.exerciseId,
                        name = exercise.name,
                        sets = sets
                    )
                )
            }

            val log = FPWorkoutLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                workoutId = session.workout.id,
                programId = session.program?.id,
                workoutName = session.workout.name,
                startedAt = session.startedAt,
                completedAt = endDate,
                durationSeconds = elapsedSeconds,
                exercises = loggedExercises,
                notes = session.notes.ifEmpty { null },
                totalVolume = totalVolume,
                prCount = prCount
            )

            try {
                syncEngine.pushWorkoutLog(log)
                _workoutLogs.update { listOf(log) + it }
                updateWeeklyProgress()
                selectNextWorkout()

                for (pr in session.prCelebrations) {
                    runCatching { syncEngine.pushPersonalRecord(pr, userId) }
                    _personalRecords.update { listOf(pr) + it }
                }
            } catch (_: Exception) {
                return@launch
            }

            _activeWorkoutSession.value = null
        }
    }

    fun updateHabit(habit: FPHabit, value: Double) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            try {
                syncEngine.pushHabitUpdate(habit, userId, value)
                _habits.update { habits ->
                    habits.map {
                        if (it.id == habit.id) it.copy(currentValue = value) else it
                    }
                }
            } catch (_: Exception) {
                // Keep local state unchanged on failure
            }
        }
    }

    fun saveMeasurement(measurement: FPMeasurement) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            try {
                syncEngine.pushMeasurement(measurement, userId)
                _measurements.update { listOf(measurement) + it }
            } catch (_: Exception) {
                // Keep local state unchanged on failure
            }
        }
    }

    fun saveProgressPhoto(draft: FPProgressPhotoDraft) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            syncEngine.pushProgressPhotoSession(
                draft = draft,
                userId = userId,
                callbacks = this@AppViewModel,
                currentPictures = _allProgressPictures.value
            )
        }
    }

    fun updateUnitPreference(key: String, value: String) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            syncEngine.pushUnitPreference(
                userId = userId,
                key = key,
                value = value,
                callbacks = this@AppViewModel,
                currentPrefs = _unitPreferences.value,
                currentUser = _currentUser.value
            )
        }
    }

    fun submitForm(form: FPForm, answers: List<FPFormAnswer>) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            syncEngine.pushFormSubmission(
                formId = form.id,
                userId = userId,
                answers = answers,
                callbacks = this@AppViewModel,
                currentForms = _forms.value
            )
        }
    }

    fun checkForPR(exercise: com.fitproject.droid.data.FPWorkoutExercise, set: FPLoggedSet): FPPersonalRecord? {
        if (!set.isCompleted) return null
        val weight = set.weight?.toDoubleOrNull() ?: return null
        val reps = set.reps?.toDoubleOrNull() ?: return null
        if (weight * reps <= 0) return null

        val existing = _personalRecords.value.find {
            it.exerciseId == exercise.exerciseId && it.metric == "Weight"
        }
        val prevWeight = existing?.value?.toDoubleOrNull()
        if (prevWeight != null && weight <= prevWeight) return null

        return FPPersonalRecord(
            id = UUID.randomUUID().toString(),
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name,
            metric = "Weight",
            value = set.weight ?: weight.toString(),
            date = Date(),
            previousValue = existing?.value
        )
    }

    // SyncStateCallbacks

    override fun onWorkoutLogsUpdated(logs: List<FPWorkoutLog>) {
        _workoutLogs.value = logs
        updateWeeklyProgress()
    }

    override fun onHabitsUpdated(habits: List<FPHabit>) {
        _habits.value = habits
    }

    override fun onFullSyncResult(result: FullSyncResult) {
        _programs.value = result.programs
        _programWeeks.value = result.programWeeks
        _workoutLogs.value = result.workoutLogs
        _habits.value = result.habits
        _allProgressPictures.value = result.allProgressPictures
        _progressSessions.value = result.progressSessions
        _measurements.value = result.measurements
        _content.value = result.content
        _personalRecords.value = result.personalRecords
        _forms.value = result.forms
        _unitPreferences.value = result.unitPreferences
        _currentUser.value = result.userProfile
        updateWeeklyProgress()
        selectNextWorkout()
    }

    override fun onSyncingChanged(isSyncing: Boolean) {
        _isSyncing.value = isSyncing
    }

    override fun onLastSyncDateChanged(date: Date?) {
        _lastSyncDate.value = date
    }

    override fun onSyncError(error: String?) {
        // Ignored for now
    }

    override fun onFormsUpdated(forms: List<FPForm>) {
        _forms.value = forms
    }

    override fun onUnitPreferencesUpdated(prefs: FPUnitPreferences, user: FPUser?) {
        _unitPreferences.value = prefs
        user?.let { _currentUser.value = it }
    }

    override fun onProgressPicturesUpdated(
        pictures: List<FPProgressPicture>,
        sessions: List<FPProgressSession>
    ) {
        _allProgressPictures.value = pictures
        _progressSessions.value = sessions
    }

    private fun mapAuthError(error: Exception): String = when (error) {
        is FirebaseAuthInvalidUserException -> "No account found with this email."
        is FirebaseAuthWeakPasswordException -> "Password is too weak."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
        else -> error.localizedMessage ?: "Authentication failed."
    }
}