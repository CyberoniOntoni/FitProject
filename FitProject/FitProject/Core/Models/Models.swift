import Foundation

// MARK: - User

struct FPUser: Identifiable, Codable, Equatable {
    let id: String
    var email: String
    var firstName: String
    var lastName: String
    var profilePictureUrl: String?
    var timezone: String?
    var coachHasProTools: Bool

    var displayName: String {
        let name = "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces)
        return name.isEmpty ? email : name
    }

    var initials: String {
        let parts = displayName.split(separator: " ")
        let first = parts.first?.prefix(1) ?? ""
        let last = parts.count > 1 ? parts.last!.prefix(1) : ""
        return "\(first)\(last)".uppercased()
    }
}

// MARK: - Program

struct FPProgram: Identifiable, Codable, Equatable {
    let id: String
    var title: String
    var description: String
    var imageUrl: String?
    var totalWeekCount: Int
    var totalWorkoutCount: Int
    var published: Bool
    var creatorIds: [String]
    var completedWorkouts: Int = 0

    var progress: Double {
        guard totalWorkoutCount > 0 else { return 0 }
        return Double(completedWorkouts) / Double(totalWorkoutCount)
    }

    var progressPercent: Int { Int(progress * 100) }
}

struct FPProgramWeek: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var index: Int
    var programId: String
    var workouts: [FPWorkout] = []
}

// MARK: - Workout

struct FPWorkout: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var description: String?
    var index: Int
    var programId: String
    var programWeekId: String
    var notes: String?
    var exerciseGroups: [FPExerciseGroup] = []
    var exercises: [FPWorkoutExercise] = []
    var metrics: [FPWorkoutMetric] = []
    var metricValues: [FPMetricValue] = []
    var isCompleted: Bool = false
    var completedAt: Date?

    var exerciseCount: Int { exercises.count }

    var groupedExercises: [(group: FPExerciseGroup?, exercises: [FPWorkoutExercise])] {
        var result: [(FPExerciseGroup?, [FPWorkoutExercise])] = []
        let sorted = exercises.sorted { $0.index < $1.index }
        var currentGroupId: String?
        var currentExercises: [FPWorkoutExercise] = []

        for exercise in sorted {
            if exercise.groupId != currentGroupId {
                if !currentExercises.isEmpty {
                    let group = exerciseGroups.first { $0.id == currentGroupId }
                    result.append((group, currentExercises))
                }
                currentGroupId = exercise.groupId
                currentExercises = [exercise]
            } else {
                currentExercises.append(exercise)
            }
        }
        if !currentExercises.isEmpty {
            let group = exerciseGroups.first { $0.id == currentGroupId }
            result.append((group, currentExercises))
        }
        return result
    }
}

struct FPExerciseGroup: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var index: Int
    var type: String?
}

struct FPWorkoutExercise: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var exerciseId: String
    var youtubeId: String?
    var thumbnailUrl: String?
    var index: Int
    var sets: Int
    var coachNotes: String?
    var header: String?
    var headerVisible: Bool
    var groupId: String?
    var isSuperset: Bool = false

    var videoThumbnailURL: URL? {
        if let url = thumbnailUrl, !url.isEmpty { return URL(string: url) }
        if let yt = youtubeId, !yt.isEmpty {
            return URL(string: "https://img.youtube.com/vi/\(yt)/mqdefault.jpg")
        }
        return nil
    }

    var youtubeURL: URL? {
        guard let yt = youtubeId, !yt.isEmpty else { return nil }
        return URL(string: "https://www.youtube.com/watch?v=\(yt)")
    }
}

struct FPWorkoutMetric: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var index: Int
    var color: String
    var exerciseMetricId: String
    var workoutExerciseId: String
    var unitAbbreviation: String?

    var swiftUIColor: String { color }
}

struct FPMetricValue: Identifiable, Codable, Equatable {
    let id: String
    var workoutExerciseId: String
    var workoutExerciseMetricId: String
    var exerciseMetricId: String
    var exerciseId: String
    var set: Int
    var index: Int
    var value: String
    var loggedValue: String?
    var isCompleted: Bool = false
}

// MARK: - Workout Log

struct FPWorkoutLog: Identifiable, Codable, Equatable {
    let id: String
    var userId: String
    var workoutId: String
    var programId: String?
    var workoutName: String
    var startedAt: Date
    var completedAt: Date?
    var durationSeconds: Int?
    var exercises: [FPLoggedExercise] = []
    var notes: String?
    var totalVolume: Double = 0
    var prCount: Int = 0

    var isCompleted: Bool { completedAt != nil }
}

struct FPLoggedExercise: Identifiable, Codable, Equatable {
    let id: String
    var exerciseId: String
    var name: String
    var sets: [FPLoggedSet] = []
}

struct FPLoggedSet: Identifiable, Codable, Equatable {
    let id: String
    var setNumber: Int
    var reps: String?
    var weight: String?
    var rpe: String?
    var rest: String?
    var tempo: String?
    var time: String?
    var isCompleted: Bool = false
    var isPR: Bool = false
}

// MARK: - Habit

struct FPHabit: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var description: String?
    var targetValue: Double
    var unit: String
    var icon: String?
    var color: String?
    var currentValue: Double = 0
    var streak: Int = 0

    var progress: Double {
        guard targetValue > 0 else { return 0 }
        return min(currentValue / targetValue, 1.0)
    }

    var progressText: String {
        let current = currentValue.truncatingRemainder(dividingBy: 1) == 0
            ? String(format: "%.0f", currentValue)
            : String(format: "%.1f", currentValue)
        let target = targetValue.truncatingRemainder(dividingBy: 1) == 0
            ? String(format: "%.0f", targetValue)
            : String(format: "%.1f", targetValue)
        return "\(current) / \(target)\(unit.isEmpty ? "" : " \(unit)")"
    }
}

struct FPHabitLog: Identifiable, Codable, Equatable {
    let id: String
    var habitId: String
    var userId: String
    var date: Date
    var value: Double
}

// MARK: - Measurement

struct FPMeasurementTypeDef: Identifiable, Equatable {
    let id: String
    let name: String
    let category: String
    let color: String
    let unitId: String
    let unitName: String
    let unitType: String
    let unitAbbreviation: String

    var displayUnit: String {
        switch category {
        case "Circumference": return "cm"
        case "Body Composition" where unitType == "MASS": return "kg"
        case "Body Composition" where unitType == "PERCENT": return "%"
        default:
            switch unitAbbreviation {
            case "centimeter": return "cm"
            case "inches", "in", "inch": return "cm"
            default: return unitAbbreviation
            }
        }
    }
}

enum MeasurementCatalog {
    static let types: [FPMeasurementTypeDef] = [
        .init(id: "DfqsrFQBGi04aHWAPA7I", name: "Bodyweight", category: "Body Composition", color: "#c93477", unitId: "kg", unitName: "Kilograms", unitType: "MASS", unitAbbreviation: "kg"),
        .init(id: "body_fat_percentage", name: "Body Fat %", category: "Body Composition", color: "#FDCB6E", unitId: "%", unitName: "PERCENT", unitType: "PERCENT", unitAbbreviation: "%"),
        .init(id: "muscle_mass", name: "Muscle Mass", category: "Body Composition", color: "#E17055", unitId: "kg", unitName: "MASS", unitType: "MASS", unitAbbreviation: "kg"),
        .init(id: "body_water_percentage", name: "Body Water %", category: "Body Composition", color: "#06b6d4", unitId: "%", unitName: "PERCENT", unitType: "PERCENT", unitAbbreviation: "%"),
        .init(id: "bone_mass", name: "Bone Mass", category: "Body Composition", color: "#94a3b8", unitId: "bone_mass_unit", unitName: "kg", unitType: "MASS", unitAbbreviation: "kg"),
        .init(id: "vo2_max", name: "VO2 Max", category: "Body Composition", color: "#7c3aed", unitId: "vo2_max_unit", unitName: "ml/kg/min", unitType: "NUMERIC", unitAbbreviation: "ml/kg/min"),
        .init(id: "visceral_fat", name: "Visceral Fat", category: "Body Composition", color: "#f97316", unitId: "level", unitName: "NUMERIC", unitType: "NUMERIC", unitAbbreviation: "level"),
        .init(id: "waist", name: "Waist", category: "Circumference", color: "#4ECDC4", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "chest", name: "Chest", category: "Circumference", color: "#10b981", unitId: "cm", unitName: "Circumference", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "arms", name: "Arms", category: "Circumference", color: "#FFA07A", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "thighs", name: "Thighs", category: "Circumference", color: "#8b5cf6", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "hips", name: "Hips", category: "Circumference", color: "#45B7D1", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "neck", name: "Neck", category: "Circumference", color: "#06b6d4", unitId: "cm", unitName: "Circumference", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "shoulders", name: "Shoulders", category: "Circumference", color: "#FD79A8", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "calves", name: "Calves", category: "Circumference", color: "#00B894", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "forearms", name: "Forearms", category: "Circumference", color: "#74B9FF", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "wrist", name: "Wrist", category: "Circumference", color: "#a855f7", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
        .init(id: "ankle", name: "Ankle", category: "Circumference", color: "#fbbf24", unitId: "cm", unitName: "CIRCUMFERENCE", unitType: "CIRCUMFERENCE", unitAbbreviation: "cm"),
    ]

    static let categories = ["Body Composition", "Circumference"]

    static func findById(_ id: String?) -> FPMeasurementTypeDef? {
        guard let id else { return nil }
        return types.first { $0.id == id }
    }

    static func findByName(_ name: String) -> FPMeasurementTypeDef? {
        if let exact = types.first(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame }) {
            return exact
        }
        switch name.lowercased() {
        case "weight", "body weight": return findById("DfqsrFQBGi04aHWAPA7I")
        case "body fat", "bodyfat": return findById("body_fat_percentage")
        default: return types.first { $0.name.localizedCaseInsensitiveContains(name) }
        }
    }

    static func types(in category: String) -> [FPMeasurementTypeDef] {
        types.filter { $0.category == category }
    }
}

struct FPMeasurement: Identifiable, Codable, Equatable {
    let id: String
    var typeId: String?
    var name: String
    var unit: String
    var value: Double
    var date: Date
    var notes: String?
    var sessionId: String?
    var source: String = "measurementLogs"

    var matchKey: String { typeId ?? name.lowercased() }
}

// MARK: - Content / Learn

struct FPContent: Identifiable, Codable, Equatable {
    let id: String
    var title: String
    var body: String?
    var imageUrl: String?
    var type: String
    var dateCreated: Date?
    var isRead: Bool = false
}

// MARK: - Personal Record

struct FPPersonalRecord: Identifiable, Codable, Equatable {
    let id: String
    var exerciseId: String
    var exerciseName: String
    var metric: String
    var value: String
    var date: Date
    var previousValue: String?
}

// MARK: - Form

struct FPFormField: Identifiable, Codable, Equatable {
    let id: String
    var type: String
    var question: String
    var required: Bool = false
    var index: Int = 0
    var maxRating: Int = 5
    var scaleMin: Int = 1
    var scaleMax: Int = 10
    var scaleMinLabel: String?
    var scaleMaxLabel: String?
    var options: [String] = []
}

struct FPFormAnswer: Codable, Equatable {
    var fieldId: String
    var question: String
    var type: String
    var value: String
}

struct FPFormSubmission: Codable, Equatable {
    var clientId: String
    var submittedAt: Date
    var answers: [FPFormAnswer]
}

struct FPForm: Identifiable, Codable, Equatable {
    let id: String
    var title: String
    var description: String?
    var creatorId: String?
    var fields: [FPFormField] = []
    var submissions: [FPFormSubmission] = []
    var dueDate: Date?

    func isCompleted(for userId: String) -> Bool {
        submissions.contains { $0.clientId == userId }
    }
}

// MARK: - Assigned Program

struct FPAssignedProgram: Identifiable, Codable, Equatable {
    let id: String
    var programId: String
    var userId: String
    var coachId: String?
    var program: FPProgram?
    var startDate: Date?
    var currentWeek: Int = 1
    var completedWorkouts: Int = 0
}

// MARK: - Tab

enum AppTab: String, CaseIterable, Identifiable {
    case train = "Train"
    case programs = "Programs"
    case learn = "Learn"
    case history = "History"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .train: return "figure.strengthtraining.traditional"
        case .programs: return "list.bullet.rectangle"
        case .learn: return "book.fill"
        case .history: return "clock.arrow.circlepath"
        }
    }
}