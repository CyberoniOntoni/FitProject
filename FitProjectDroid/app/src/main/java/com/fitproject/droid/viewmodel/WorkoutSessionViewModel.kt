package com.fitproject.droid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitproject.droid.data.FPLoggedSet
import com.fitproject.droid.data.FPPersonalRecord
import com.fitproject.droid.data.FPWorkoutExercise
import com.fitproject.droid.data.WorkoutSessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class WorkoutSessionViewModel(
    initialSession: WorkoutSessionState,
    private val appViewModel: AppViewModel
) : ViewModel() {

    private val exercises = initialSession.workout.exercises.sortedBy { it.index }

    private val _session = MutableStateFlow(initialSession)
    val session: StateFlow<WorkoutSessionState> = _session.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(initialSession.currentExerciseIndex)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _loggedSets = MutableStateFlow(initialSession.loggedSets)
    val loggedSets: StateFlow<Map<String, List<FPLoggedSet>>> = _loggedSets.asStateFlow()

    private val _notes = MutableStateFlow(initialSession.notes)
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(initialSession.elapsedSeconds)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _restSecondsRemaining = MutableStateFlow(0)
    val restSecondsRemaining: StateFlow<Int> = _restSecondsRemaining.asStateFlow()

    private val _restTimerActive = MutableStateFlow(false)
    val restTimerActive: StateFlow<Boolean> = _restTimerActive.asStateFlow()

    private val _showPRToast = MutableStateFlow(false)
    val showPRToast: StateFlow<Boolean> = _showPRToast.asStateFlow()

    private val _latestPR = MutableStateFlow<FPPersonalRecord?>(null)
    val latestPR: StateFlow<FPPersonalRecord?> = _latestPR.asStateFlow()

    private val _showCompleteDialog = MutableStateFlow(false)
    val showCompleteDialog: StateFlow<Boolean> = _showCompleteDialog.asStateFlow()

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    private var elapsedTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private val prCelebrations = mutableListOf<FPPersonalRecord>()

    val currentExercise: FPWorkoutExercise?
        get() = exercises.getOrNull(_currentExerciseIndex.value)

    val exerciseCount: Int get() = exercises.size

    val progress: Float
        get() = if (exerciseCount == 0) 0f else (_currentExerciseIndex.value + 1).toFloat() / exerciseCount

    init {
        startElapsedTimer()
    }

    fun updateNotes(value: String) {
        _notes.value = value
        syncSessionToAppViewModel()
    }

    fun updateSet(exerciseId: String, index: Int, set: FPLoggedSet) {
        _loggedSets.update { current ->
            val sets = current[exerciseId]?.toMutableList() ?: mutableListOf()
            while (sets.size <= index) {
                sets.add(FPLoggedSet(id = UUID.randomUUID().toString(), setNumber = sets.size + 1))
            }
            sets[index] = set
            current + (exerciseId to sets)
        }
        syncSessionToAppViewModel()
    }

    fun addSet(exercise: FPWorkoutExercise) {
        _loggedSets.update { current ->
            val sets = current[exercise.id]?.toMutableList() ?: mutableListOf()
            val lastSet = sets.lastOrNull()
            sets.add(
                FPLoggedSet(
                    id = UUID.randomUUID().toString(),
                    setNumber = sets.size + 1,
                    reps = lastSet?.reps,
                    weight = lastSet?.weight,
                    rpe = lastSet?.rpe,
                    rest = lastSet?.rest,
                    tempo = lastSet?.tempo,
                    time = lastSet?.time
                )
            )
            current + (exercise.id to sets)
        }
        syncSessionToAppViewModel()
    }

    fun toggleSetComplete(exercise: FPWorkoutExercise, setIndex: Int) {
        val sets = _loggedSets.value[exercise.id] ?: return
        if (setIndex !in sets.indices) return
        val set = sets[setIndex]
        val newCompleted = !set.isCompleted
        var updatedSet = set.copy(isCompleted = newCompleted)

        if (newCompleted) {
            val pr = appViewModel.checkForPR(exercise, updatedSet)
            if (pr != null) {
                updatedSet = updatedSet.copy(isPR = true)
                _latestPR.value = pr
                _showPRToast.value = true
                if (prCelebrations.none { it.exerciseId == pr.exerciseId }) {
                    prCelebrations.add(pr)
                }
                viewModelScope.launch {
                    delay(2500)
                    _showPRToast.value = false
                }
            }
            set.rest?.toIntOrNull()?.takeIf { it > 0 }?.let { startRestTimer(it) }
        }

        updateSet(exercise.id, setIndex, updatedSet)
    }

    fun previousExercise() {
        if (_currentExerciseIndex.value > 0) {
            _currentExerciseIndex.value -= 1
            syncSessionToAppViewModel()
        }
    }

    fun nextExercise() {
        if (_currentExerciseIndex.value < exercises.size - 1) {
            _currentExerciseIndex.value += 1
            syncSessionToAppViewModel()
        } else {
            _showCompleteDialog.value = true
        }
    }

    fun dismissCompleteDialog() {
        _showCompleteDialog.value = false
    }

    fun requestFinish() {
        _showCompleteDialog.value = true
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _restTimerActive.value = false
        _restSecondsRemaining.value = 0
    }

    fun addRestTime(seconds: Int = 30) {
        _restSecondsRemaining.update { it + seconds }
    }

    fun finishWorkout(onComplete: () -> Unit) {
        _isFinishing.value = true
        elapsedTimerJob?.cancel()
        restTimerJob?.cancel()

        val updated = _session.value.copy(
            loggedSets = _loggedSets.value,
            notes = _notes.value,
            elapsedSeconds = _elapsedSeconds.value,
            currentExerciseIndex = _currentExerciseIndex.value,
            prCelebrations = prCelebrations.toList()
        )
        appViewModel.updateWorkoutSession(updated)
        appViewModel.completeWorkout()
        _isFinishing.value = false
        onComplete()
    }

    private fun startElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.update { it + 1 }
            }
        }
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restSecondsRemaining.value = seconds
        _restTimerActive.value = true
        restTimerJob = viewModelScope.launch {
            while (_restSecondsRemaining.value > 0) {
                delay(1000)
                _restSecondsRemaining.update { it - 1 }
            }
            _restTimerActive.value = false
        }
    }

    private fun syncSessionToAppViewModel() {
        _session.update {
            it.copy(
                loggedSets = _loggedSets.value,
                notes = _notes.value,
                elapsedSeconds = _elapsedSeconds.value,
                currentExerciseIndex = _currentExerciseIndex.value,
                prCelebrations = prCelebrations.toList()
            )
        }
        appViewModel.updateWorkoutSession(_session.value)
    }

    fun formatElapsed(seconds: Int): String =
        "%02d:%02d".format(seconds / 60, seconds % 60)

    fun formatRest(seconds: Int): String =
        "%d:%02d".format(seconds / 60, seconds % 60)

    override fun onCleared() {
        elapsedTimerJob?.cancel()
        restTimerJob?.cancel()
        super.onCleared()
    }
}