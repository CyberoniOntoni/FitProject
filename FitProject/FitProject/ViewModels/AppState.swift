import Foundation
import Combine

@MainActor
final class AppState: ObservableObject {
    @Published var selectedTab: AppTab = .train
    @Published var programs: [FPProgram] = []
    @Published var programWeeks: [String: [FPProgramWeek]] = [:]
    @Published var assignedPrograms: [FPAssignedProgram] = []
    @Published var workoutLogs: [FPWorkoutLog] = []
    @Published var habits: [FPHabit] = []
    @Published var measurements: [FPMeasurement] = []
    @Published var content: [FPContent] = []
    @Published var personalRecords: [FPPersonalRecord] = []
    @Published var forms: [FPForm] = []

    @Published var activeWorkout: FPWorkout?
    @Published var activeProgram: FPProgram?
    @Published var nextWorkout: FPWorkout?
    @Published var nextProgram: FPProgram?

    @Published var weeklyWorkoutGoal: Int = 4
    @Published var weeklyWorkoutsCompleted: Int = 0
    @Published var activeWorkoutSession: WorkoutSessionState?

    let authService = AuthService()
    let syncEngine = SyncEngine()

    struct WorkoutSessionState {
        var workout: FPWorkout
        var program: FPProgram?
        var startedAt: Date
        var currentExerciseIndex: Int = 0
        var loggedSets: [String: [FPLoggedSet]] = [:]
        var notes: String = ""
        var restTimerEnd: Date?
        var restDuration: Int = 90
        var prCelebrations: [FPPersonalRecord] = []
        var elapsedSeconds: Int = 0
    }

    func loadData() async {
        guard let userId = authService.currentUser?.id else { return }
        syncEngine.startSync(userId: userId, appState: self)
        await syncEngine.fullSync(userId: userId, appState: self)
    }

    func signOut() {
        syncEngine.stopSync()
        programs = []
        programWeeks = [:]
        workoutLogs = []
        habits = []
        measurements = []
        content = []
        personalRecords = []
        forms = []
        activeWorkoutSession = nil
        try? authService.signOut()
    }

    func updateWeeklyProgress() {
        let calendar = Calendar.current
        let startOfWeek = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: Date()))!
        weeklyWorkoutsCompleted = workoutLogs.filter { log in
            log.isCompleted && log.completedAt.map { $0 >= startOfWeek } == true
        }.count
    }

    func selectNextWorkout() {
        let completedIds = Set(workoutLogs.filter(\.isCompleted).map(\.workoutId))

        for program in programs {
            guard let weeks = programWeeks[program.id] else { continue }
            for week in weeks.sorted(by: { $0.index < $1.index }) {
                for workout in week.workouts.sorted(by: { $0.index < $1.index }) {
                    if !completedIds.contains(workout.id) {
                        nextWorkout = workout
                        nextProgram = program
                        return
                    }
                }
            }
        }
        nextWorkout = nil
        nextProgram = programs.first
    }

    func startWorkout(_ workout: FPWorkout, program: FPProgram?) {
        var loggedSets: [String: [FPLoggedSet]] = [:]
        for exercise in workout.exercises {
            let exerciseMetrics = workout.metrics.filter { $0.workoutExerciseId == exercise.id }
            let setCount = exercise.sets > 0 ? exercise.sets : 3
            loggedSets[exercise.id] = (1...setCount).map { setNum in
                var set = FPLoggedSet(id: UUID().uuidString, setNumber: setNum)
                for metric in exerciseMetrics {
                    let preset = workout.metricValues.first {
                        $0.workoutExerciseId == exercise.id &&
                        $0.workoutExerciseMetricId == metric.id &&
                        $0.set == setNum
                    }
                    guard let value = preset?.value else { continue }
                    switch metric.name {
                    case "Reps": set.reps = value
                    case "Weight": set.weight = value
                    case "RPE": set.rpe = value
                    case "Rest": set.rest = value
                    case "Tempo": set.tempo = value
                    case "Time": set.time = value
                    default: break
                    }
                }
                return set
            }
        }

        activeWorkoutSession = WorkoutSessionState(
            workout: workout,
            program: program,
            startedAt: Date(),
            loggedSets: loggedSets
        )
        activeWorkout = workout
        activeProgram = program
    }

    func completeWorkout() async {
        guard var session = activeWorkoutSession,
              let userId = authService.currentUser?.id else { return }

        let endDate = Date()
        session.elapsedSeconds = Int(endDate.timeIntervalSince(session.startedAt))

        var totalVolume: Double = 0
        var prCount = 0
        var loggedExercises: [FPLoggedExercise] = []

        for exercise in session.workout.exercises {
            let sets = session.loggedSets[exercise.id] ?? []
            for set in sets where set.isCompleted {
                if let weight = Double(set.weight ?? ""),
                   let reps = Double(set.reps ?? "") {
                    totalVolume += weight * reps
                }
                if set.isPR { prCount += 1 }
            }
            loggedExercises.append(FPLoggedExercise(
                id: exercise.id,
                exerciseId: exercise.exerciseId,
                name: exercise.name,
                sets: sets
            ))
        }

        let log = FPWorkoutLog(
            id: UUID().uuidString,
            userId: userId,
            workoutId: session.workout.id,
            programId: session.program?.id,
            workoutName: session.workout.name,
            startedAt: session.startedAt,
            completedAt: endDate,
            durationSeconds: session.elapsedSeconds,
            exercises: loggedExercises,
            notes: session.notes.isEmpty ? nil : session.notes,
            totalVolume: totalVolume,
            prCount: prCount
        )

        do {
            try await syncEngine.pushWorkoutLog(log)
            workoutLogs.insert(log, at: 0)
            updateWeeklyProgress()
            selectNextWorkout()

            for pr in session.prCelebrations {
                try? await syncEngine.pushPersonalRecord(pr, userId: userId)
            }
        } catch {
            // Keep session open on failure
            return
        }

        activeWorkoutSession = nil
        activeWorkout = nil
    }

    func checkForPR(exercise: FPWorkoutExercise, set: FPLoggedSet) -> FPPersonalRecord? {
        guard set.isCompleted,
              let weightStr = set.weight,
              let weight = Double(weightStr),
              let repsStr = set.reps,
              let reps = Double(repsStr) else { return nil }

        let volume = weight * reps
        let existing = personalRecords.first {
            $0.exerciseId == exercise.exerciseId && $0.metric == "Weight"
        }

        if let existing, let prevWeight = Double(existing.value), weight <= prevWeight {
            return nil
        }

        return FPPersonalRecord(
            id: UUID().uuidString,
            exerciseId: exercise.exerciseId,
            exerciseName: exercise.name,
            metric: "Weight",
            value: weightStr,
            date: Date(),
            previousValue: existing?.value
        )
    }
}