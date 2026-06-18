import Foundation
import FirebaseFirestore
import Combine

@MainActor
final class SyncEngine: ObservableObject {
    @Published var isSyncing = false
    @Published var lastSyncDate: Date?
    @Published var syncError: String?

    private var listeners: [ListenerRegistration] = []
    private let firestore = FirestoreService.shared

    func startSync(userId: String, appState: AppState) {
        stopSync()

        let logsListener = firestore.listenToWorkoutLogs(userId: userId) { logs in
            Task { @MainActor in
                appState.workoutLogs = logs
                appState.updateWeeklyProgress()
            }
        }
        listeners.append(logsListener)

        let habitsListener = firestore.listenToHabits(userId: userId) { habits in
            Task { @MainActor in
                appState.habits = habits
            }
        }
        listeners.append(habitsListener)
    }

    func stopSync() {
        listeners.forEach { $0.remove() }
        listeners.removeAll()
    }

    func fullSync(userId: String, appState: AppState) async {
        isSyncing = true
        syncError = nil
        defer {
            isSyncing = false
            lastSyncDate = Date()
        }

        do {
            async let assignedPrograms = firestore.fetchAssignedPrograms(userId: userId)
            async let creatorPrograms = firestore.fetchCreatorPrograms(userId: userId)
            async let workoutLogs = firestore.fetchWorkoutLogs(userId: userId)
            async let habits = firestore.fetchHabits(userId: userId)
            async let progressPictures = firestore.fetchProgressPictures(userId: userId)
            async let measurements = firestore.fetchAllMeasurements(userId: userId)
            async let content = firestore.fetchContent(userId: userId)
            async let records = firestore.fetchPersonalRecords(userId: userId)
            async let forms = firestore.fetchForms(userId: userId)
            async let profile = firestore.fetchUserProfile(userId: userId)

            let (assigned, creator, logs, habitsResult, pictures, meas, cont, recs, formsResult, userProfile) = try await (
                assignedPrograms, creatorPrograms, workoutLogs, habits, progressPictures,
                measurements, content, records, forms, profile
            )

            var allPrograms: [FPProgram] = creator
            for item in assigned {
                if let program = item.program, !allPrograms.contains(where: { $0.id == program.id }) {
                    var p = program
                    p.completedWorkouts = item.completedWorkouts
                    allPrograms.append(p)
                }
            }

            var weeksByProgram: [String: [FPProgramWeek]] = [:]
            for program in allPrograms {
                weeksByProgram[program.id] = try await firestore.fetchProgramWeeks(programId: program.id)
            }

            appState.programs = allPrograms
            appState.programWeeks = weeksByProgram
            appState.assignedPrograms = assigned
            appState.workoutLogs = logs
            appState.habits = habitsResult
            appState.allProgressPictures = pictures
            appState.progressSessions = FirestoreService.groupProgressSessions(pictures)
            appState.measurements = meas
            appState.content = cont
            appState.personalRecords = recs
            appState.forms = formsResult
            appState.unitPreferences = userProfile.unitPreferences
            appState.authService.updateUserProfile(userProfile)
            appState.updateWeeklyProgress()
            appState.selectNextWorkout()
        } catch {
            syncError = error.localizedDescription
        }
    }

    func pushWorkoutLog(_ log: FPWorkoutLog) async throws {
        try await firestore.saveWorkoutLog(log)
    }

    func pushHabitUpdate(habit: FPHabit, userId: String, value: Double) async throws {
        try await firestore.saveUserHabitLog(habit, userId: userId, value: value)
    }

    func pushMeasurement(_ measurement: FPMeasurement, userId: String) async throws {
        try await firestore.saveMeasurement(measurement, userId: userId)
    }

    func pushPersonalRecord(_ record: FPPersonalRecord, userId: String) async throws {
        try await firestore.savePersonalRecord(record, userId: userId)
    }

    func pushFormSubmission(formId: String, userId: String, answers: [FPFormAnswer], appState: AppState) async throws {
        try await firestore.submitForm(formId: formId, clientId: userId, answers: answers)
        if let index = appState.forms.firstIndex(where: { $0.id == formId }) {
            var form = appState.forms[index]
            form.submissions.append(FPFormSubmission(
                clientId: userId,
                submittedAt: Date(),
                answers: answers
            ))
            appState.forms[index] = form
        }
    }

    func pushUnitPreference(userId: String, key: String, value: String, appState: AppState) async throws {
        try await firestore.updateUnitPreference(userId: userId, key: key, value: value)
        var prefs = appState.unitPreferences
        switch key {
        case "weight": prefs.weight = value
        case "mass": prefs.mass = value
        case "circumference": prefs.circumference = value
        case "distance": prefs.distance = value
        case "time": prefs.time = value
        default: break
        }
        appState.unitPreferences = prefs
        if var user = appState.authService.currentUser {
            user.unitPreferences = prefs
            appState.authService.updateUserProfile(user)
        }
    }

    func pushProgressPhotoSession(draft: FPProgressPhotoDraft, userId: String, appState: AppState) async throws {
        var poses: [(poseType: String, imageUrl: String, existingId: String?)] = []
        for (poseType, payload) in draft.poseImageData {
            let imageUrl = try await FirebaseStorageService.shared.uploadProgressPhoto(
                userId: userId,
                sessionId: draft.sessionId,
                poseType: poseType,
                data: payload.data,
                contentType: payload.contentType,
                fileExtension: payload.fileExtension
            )
            poses.append((poseType, imageUrl, nil))
        }

        try await firestore.saveProgressSession(
            userId: userId,
            sessionId: draft.sessionId,
            dateCreated: draft.dateCreated,
            notes: draft.notes.isEmpty ? nil : draft.notes,
            poses: poses
        )

        let pictures = poses.map { pose in
            FPProgressPicture(
                id: UUID().uuidString,
                userId: userId,
                sessionId: draft.sessionId,
                poseType: pose.poseType,
                imageUrl: pose.imageUrl,
                dateCreated: draft.dateCreated,
                notes: draft.notes.isEmpty ? nil : draft.notes
            )
        }
        appState.allProgressPictures.insert(contentsOf: pictures, at: 0)
        appState.progressSessions = FirestoreService.groupProgressSessions(appState.allProgressPictures)
    }
}