import Foundation
import FirebaseFirestore

final class FirestoreService {
    static let shared = FirestoreService()
    private let db = Firestore.firestore()
    private let decoder = Firestore.Decoder()
    private let encoder = Firestore.Encoder()

    private init() {}

    // MARK: - User

    func fetchUserProfile(userId: String) async throws -> FPUser {
        let doc = try await db.collection("users").document(userId).getDocument()
        guard let data = doc.data() else {
            throw FitProsError.notFound("User profile")
        }
        return FPUser(
            id: userId,
            email: data["email"] as? String ?? "",
            firstName: data["firstName"] as? String ?? "",
            lastName: data["lastName"] as? String ?? "",
            profilePictureUrl: data["profilePictureUrl"] as? String,
            timezone: data["timezone"] as? String,
            coachHasProTools: data["coachHasProTools"] as? Bool ?? false
        )
    }

    // MARK: - Programs

    func fetchAssignedPrograms(userId: String) async throws -> [FPAssignedProgram] {
        let snapshot = try await db.collection("assignedPrograms")
            .whereField("userId", isEqualTo: userId)
            .getDocuments()

        var assigned: [FPAssignedProgram] = []
        for doc in snapshot.documents {
            let data = doc.data()
            let programId = data["programId"] as? String ?? ""
            var item = FPAssignedProgram(
                id: doc.documentID,
                programId: programId,
                userId: userId,
                coachId: data["coachId"] as? String,
                startDate: (data["startDate"] as? Timestamp)?.dateValue(),
                currentWeek: data["currentWeek"] as? Int ?? 1,
                completedWorkouts: data["completedWorkouts"] as? Int ?? 0
            )
            if !programId.isEmpty {
                item.program = try? await fetchProgram(programId: programId)
            }
            assigned.append(item)
        }
        return assigned
    }

    func fetchCreatorPrograms(userId: String) async throws -> [FPProgram] {
        let snapshot = try await db.collection("programs")
            .whereField("creatorIds", arrayContains: userId)
            .getDocuments()
        return snapshot.documents.compactMap { parseProgram($0) }
    }

    func fetchProgram(programId: String) async throws -> FPProgram {
        let doc = try await db.collection("programs").document(programId).getDocument()
        guard let program = parseProgram(doc) else {
            throw FitProsError.notFound("Program")
        }
        return program
    }

    func fetchProgramWeeks(programId: String) async throws -> [FPProgramWeek] {
        let snapshot = try await db.collection("programs").document(programId)
            .collection("programWeeks")
            .order(by: "index")
            .getDocuments()

        var weeks: [FPProgramWeek] = []
        for doc in snapshot.documents {
            let data = doc.data()
            var week = FPProgramWeek(
                id: doc.documentID,
                name: data["name"] as? String ?? "Week",
                index: data["index"] as? Int ?? 0,
                programId: programId
            )
            week.workouts = try await fetchWorkouts(programId: programId, weekId: doc.documentID)
            weeks.append(week)
        }
        return weeks
    }

    func fetchWorkouts(programId: String, weekId: String) async throws -> [FPWorkout] {
        let snapshot = try await db.collection("programs").document(programId)
            .collection("programWeeks").document(weekId)
            .collection("workouts")
            .order(by: "index")
            .getDocuments()

        return snapshot.documents.compactMap { parseWorkout($0, programId: programId, weekId: weekId) }
            .sorted { $0.index < $1.index }
    }

    func fetchWorkout(programId: String, weekId: String, workoutId: String) async throws -> FPWorkout {
        let doc = try await db.collection("programs").document(programId)
            .collection("programWeeks").document(weekId)
            .collection("workouts").document(workoutId)
            .getDocument()

        guard let workout = parseWorkout(doc, programId: programId, weekId: weekId) else {
            throw FitProsError.notFound("Workout")
        }
        return workout
    }

    // MARK: - Workout Logs

    func fetchWorkoutLogs(userId: String, limit: Int = 50) async throws -> [FPWorkoutLog] {
        let snapshot = try await db.collection("workoutLogs")
            .whereField("userId", isEqualTo: userId)
            .order(by: "startedAt", descending: true)
            .limit(to: limit)
            .getDocuments()

        return snapshot.documents.compactMap { parseWorkoutLog($0) }
    }

    func saveWorkoutLog(_ log: FPWorkoutLog) async throws {
        let ref = db.collection("workoutLogs").document(log.id)
        var data: [String: Any] = [
            "userId": log.userId,
            "workoutId": log.workoutId,
            "workoutName": log.workoutName,
            "startedAt": Timestamp(date: log.startedAt),
            "totalVolume": log.totalVolume,
            "prCount": log.prCount,
            "exercises": log.exercises.map { exerciseToDict($0) }
        ]
        if let programId = log.programId { data["programId"] = programId }
        if let completedAt = log.completedAt { data["completedAt"] = Timestamp(date: completedAt) }
        if let duration = log.durationSeconds { data["durationSeconds"] = duration }
        if let notes = log.notes { data["notes"] = notes }
        try await ref.setData(data, merge: true)
    }

    // MARK: - Habits

    func fetchHabits(userId: String) async throws -> [FPHabit] {
        let snapshot = try await db.collection("habits")
            .whereField("userId", isEqualTo: userId)
            .getDocuments()

        return snapshot.documents.compactMap { doc in
            let data = doc.data()
            return FPHabit(
                id: doc.documentID,
                name: data["name"] as? String ?? "",
                description: data["description"] as? String,
                targetValue: data["targetValue"] as? Double ?? 0,
                unit: data["unit"] as? String ?? "",
                icon: data["icon"] as? String,
                color: data["color"] as? String,
                currentValue: data["currentValue"] as? Double ?? 0,
                streak: data["streak"] as? Int ?? 0
            )
        }
    }

    func saveHabitLog(_ log: FPHabitLog) async throws {
        let ref = db.collection("habitLogs").document(log.id)
        try await ref.setData([
            "habitId": log.habitId,
            "userId": log.userId,
            "date": Timestamp(date: log.date),
            "value": log.value
        ])
    }

    func updateHabitValue(habitId: String, value: Double) async throws {
        try await db.collection("habits").document(habitId).updateData([
            "currentValue": value,
            "lastUpdated": FieldValue.serverTimestamp()
        ])
    }

    // MARK: - Measurements

    func fetchMeasurements(userId: String) async throws -> [FPMeasurement] {
        let snapshot = try await db.collection("measurements")
            .whereField("userId", isEqualTo: userId)
            .order(by: "date", descending: true)
            .getDocuments()

        return snapshot.documents.compactMap { doc in
            let data = doc.data()
            guard let date = (data["date"] as? Timestamp)?.dateValue() else { return nil }
            return FPMeasurement(
                id: doc.documentID,
                name: data["name"] as? String ?? "",
                unit: data["unit"] as? String ?? "",
                value: data["value"] as? Double ?? 0,
                date: date,
                notes: data["notes"] as? String
            )
        }
    }

    func saveMeasurement(_ measurement: FPMeasurement, userId: String) async throws {
        try await db.collection("measurements").document(measurement.id).setData([
            "userId": userId,
            "name": measurement.name,
            "unit": measurement.unit,
            "value": measurement.value,
            "date": Timestamp(date: measurement.date),
            "notes": measurement.notes as Any
        ])
    }

    // MARK: - Content

    func fetchContent(userId: String) async throws -> [FPContent] {
        let snapshot = try await db.collection("content")
            .whereField("userIds", arrayContains: userId)
            .getDocuments()

        return snapshot.documents.compactMap { doc in
            let data = doc.data()
            return FPContent(
                id: doc.documentID,
                title: data["title"] as? String ?? "",
                body: data["body"] as? String,
                imageUrl: data["imageUrl"] as? String,
                type: data["type"] as? String ?? "article",
                dateCreated: (data["dateCreated"] as? Timestamp)?.dateValue()
            )
        }
    }

    // MARK: - Personal Records

    func fetchPersonalRecords(userId: String) async throws -> [FPPersonalRecord] {
        let snapshot = try await db.collection("personalRecords")
            .whereField("userId", isEqualTo: userId)
            .order(by: "date", descending: true)
            .getDocuments()

        return snapshot.documents.compactMap { doc in
            let data = doc.data()
            guard let date = (data["date"] as? Timestamp)?.dateValue() else { return nil }
            return FPPersonalRecord(
                id: doc.documentID,
                exerciseId: data["exerciseId"] as? String ?? "",
                exerciseName: data["exerciseName"] as? String ?? "",
                metric: data["metric"] as? String ?? "",
                value: data["value"] as? String ?? "",
                date: date,
                previousValue: data["previousValue"] as? String
            )
        }
    }

    func savePersonalRecord(_ record: FPPersonalRecord, userId: String) async throws {
        try await db.collection("personalRecords").document(record.id).setData([
            "userId": userId,
            "exerciseId": record.exerciseId,
            "exerciseName": record.exerciseName,
            "metric": record.metric,
            "value": record.value,
            "date": Timestamp(date: record.date),
            "previousValue": record.previousValue as Any
        ])
    }

    // MARK: - Forms

    func fetchForms(userId: String) async throws -> [FPForm] {
        let snapshot = try await db.collection("forms")
            .whereField("userIds", arrayContains: userId)
            .getDocuments()

        return snapshot.documents.compactMap { doc in
            let data = doc.data()
            return FPForm(
                id: doc.documentID,
                title: data["title"] as? String ?? "",
                description: data["description"] as? String,
                isCompleted: data["isCompleted"] as? Bool ?? false,
                dueDate: (data["dueDate"] as? Timestamp)?.dateValue()
            )
        }
    }

    // MARK: - Real-time Listeners

    func listenToWorkoutLogs(userId: String, onChange: @escaping ([FPWorkoutLog]) -> Void) -> ListenerRegistration {
        db.collection("workoutLogs")
            .whereField("userId", isEqualTo: userId)
            .order(by: "startedAt", descending: true)
            .limit(to: 50)
            .addSnapshotListener { snapshot, _ in
                let logs = snapshot?.documents.compactMap { self.parseWorkoutLog($0) } ?? []
                onChange(logs)
            }
    }

    func listenToHabits(userId: String, onChange: @escaping ([FPHabit]) -> Void) -> ListenerRegistration {
        db.collection("habits")
            .whereField("userId", isEqualTo: userId)
            .addSnapshotListener { snapshot, _ in
                let habits = snapshot?.documents.compactMap { doc -> FPHabit? in
                    let data = doc.data()
                    return FPHabit(
                        id: doc.documentID,
                        name: data["name"] as? String ?? "",
                        description: data["description"] as? String,
                        targetValue: data["targetValue"] as? Double ?? 0,
                        unit: data["unit"] as? String ?? "",
                        icon: data["icon"] as? String,
                        color: data["color"] as? String,
                        currentValue: data["currentValue"] as? Double ?? 0,
                        streak: data["streak"] as? Int ?? 0
                    )
                } ?? []
                onChange(habits)
            }
    }

    // MARK: - Parsing

    private func parseProgram(_ doc: DocumentSnapshot) -> FPProgram? {
        guard let data = doc.data() else { return nil }
        return FPProgram(
            id: doc.documentID,
            title: data["title"] as? String ?? "",
            description: data["description"] as? String ?? "",
            imageUrl: data["imageUrl"] as? String,
            totalWeekCount: data["totalWeekCount"] as? Int ?? 0,
            totalWorkoutCount: data["totalWorkoutCount"] as? Int ?? 0,
            published: data["published"] as? Bool ?? false,
            creatorIds: data["creatorIds"] as? [String] ?? []
        )
    }

    private func parseWorkout(_ doc: DocumentSnapshot, programId: String, weekId: String) -> FPWorkout? {
        guard let data = doc.data() else { return nil }

        let groups = (data["workoutExerciseGroups"] as? [[String: Any]] ?? []).map { g in
            FPExerciseGroup(
                id: g["id"] as? String ?? UUID().uuidString,
                name: g["name"] as? String ?? "",
                index: g["index"] as? Int ?? 0,
                type: g["type"] as? String
            )
        }

        let exercises = (data["workoutExercises"] as? [[String: Any]] ?? []).map { e in
            FPWorkoutExercise(
                id: e["id"] as? String ?? UUID().uuidString,
                name: e["name"] as? String ?? "",
                exerciseId: e["exerciseId"] as? String ?? "",
                youtubeId: e["youtubeId"] as? String,
                thumbnailUrl: e["thumbnailUrl"] as? String,
                index: e["index"] as? Int ?? 0,
                sets: e["sets"] as? Int ?? 0,
                coachNotes: e["coachNotes"] as? String,
                header: e["header"] as? String,
                headerVisible: e["headerVisible"] as? Bool ?? false,
                groupId: e["workoutExerciseGroupId"] as? String,
                isSuperset: e["isSuperset"] as? Bool ?? false
            )
        }

        let metrics = (data["workoutExerciseMetrics"] as? [[String: Any]] ?? []).map { m in
            let unit = m["unitOfMeasurement"] as? [String: Any]
            return FPWorkoutMetric(
                id: m["id"] as? String ?? UUID().uuidString,
                name: m["name"] as? String ?? "",
                index: m["index"] as? Int ?? 0,
                color: m["color"] as? String ?? "#ffffff",
                exerciseMetricId: m["exerciseMetricId"] as? String ?? "",
                workoutExerciseId: m["workoutExerciseId"] as? String ?? "",
                unitAbbreviation: unit?["abbreviation"] as? String
            )
        }

        let values = (data["workoutExerciseMetricValues"] as? [[String: Any]] ?? []).map { v in
            FPMetricValue(
                id: v["id"] as? String ?? UUID().uuidString,
                workoutExerciseId: v["workoutExerciseId"] as? String ?? "",
                workoutExerciseMetricId: v["workoutExerciseMetricId"] as? String ?? "",
                exerciseMetricId: v["exerciseMetricId"] as? String ?? "",
                exerciseId: v["exerciseId"] as? String ?? "",
                set: v["set"] as? Int ?? 1,
                index: v["index"] as? Int ?? 0,
                value: v["value"] as? String ?? "",
                loggedValue: v["loggedValue"] as? String,
                isCompleted: v["isCompleted"] as? Bool ?? false
            )
        }

        return FPWorkout(
            id: doc.documentID,
            name: data["name"] as? String ?? "",
            description: data["description"] as? String,
            index: data["index"] as? Int ?? 0,
            programId: programId,
            programWeekId: weekId,
            notes: data["notes"] as? String,
            exerciseGroups: groups,
            exercises: exercises,
            metrics: metrics,
            metricValues: values
        )
    }

    private func parseWorkoutLog(_ doc: DocumentSnapshot) -> FPWorkoutLog? {
        let data = doc.data() ?? [:]
        guard let startedAt = (data["startedAt"] as? Timestamp)?.dateValue() else { return nil }

        let exercises = (data["exercises"] as? [[String: Any]] ?? []).map { e -> FPLoggedExercise in
            let sets = (e["sets"] as? [[String: Any]] ?? []).map { s in
                FPLoggedSet(
                    id: s["id"] as? String ?? UUID().uuidString,
                    setNumber: s["setNumber"] as? Int ?? 1,
                    reps: s["reps"] as? String,
                    weight: s["weight"] as? String,
                    rpe: s["rpe"] as? String,
                    rest: s["rest"] as? String,
                    tempo: s["tempo"] as? String,
                    time: s["time"] as? String,
                    isCompleted: s["isCompleted"] as? Bool ?? false,
                    isPR: s["isPR"] as? Bool ?? false
                )
            }
            return FPLoggedExercise(
                id: e["id"] as? String ?? UUID().uuidString,
                exerciseId: e["exerciseId"] as? String ?? "",
                name: e["name"] as? String ?? "",
                sets: sets
            )
        }

        return FPWorkoutLog(
            id: doc.documentID,
            userId: data["userId"] as? String ?? "",
            workoutId: data["workoutId"] as? String ?? "",
            programId: data["programId"] as? String,
            workoutName: data["workoutName"] as? String ?? "",
            startedAt: startedAt,
            completedAt: (data["completedAt"] as? Timestamp)?.dateValue(),
            durationSeconds: data["durationSeconds"] as? Int,
            exercises: exercises,
            notes: data["notes"] as? String,
            totalVolume: data["totalVolume"] as? Double ?? 0,
            prCount: data["prCount"] as? Int ?? 0
        )
    }

    private func exerciseToDict(_ exercise: FPLoggedExercise) -> [String: Any] {
        [
            "id": exercise.id,
            "exerciseId": exercise.exerciseId,
            "name": exercise.name,
            "sets": exercise.sets.map { set in
                var dict: [String: Any] = [
                    "id": set.id,
                    "setNumber": set.setNumber,
                    "isCompleted": set.isCompleted,
                    "isPR": set.isPR
                ]
                if let reps = set.reps { dict["reps"] = reps }
                if let weight = set.weight { dict["weight"] = weight }
                if let rpe = set.rpe { dict["rpe"] = rpe }
                if let rest = set.rest { dict["rest"] = rest }
                if let tempo = set.tempo { dict["tempo"] = tempo }
                if let time = set.time { dict["time"] = time }
                return dict
            }
        ]
    }
}

enum FitProsError: LocalizedError {
    case notFound(String)
    case syncFailed(String)

    var errorDescription: String? {
        switch self {
        case .notFound(let item): return "\(item) not found."
        case .syncFailed(let reason): return "Sync failed: \(reason)"
        }
    }
}