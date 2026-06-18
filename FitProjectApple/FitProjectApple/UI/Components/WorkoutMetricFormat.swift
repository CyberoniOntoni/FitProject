import Foundation

enum WorkoutMetricFormat {
    static let highlightedMetrics: Set<String> = ["Reps", "Rest", "RPE", "Tempo"]

    static func isHighlighted(_ name: String) -> Bool {
        highlightedMetrics.contains(name)
    }

    static func fieldMinWidth(_ name: String) -> CGFloat {
        isHighlighted(name) ? 68 : 58
    }

    static func formatTempoDisplay(_ value: String?) -> String {
        guard let value, !value.isEmpty else { return "" }
        let digits = value.filter(\.isNumber)
        guard !digits.isEmpty else { return "" }
        let clipped = String(digits.prefix(3))
        let normalized = clipped.count >= 3
            ? clipped
            : String(repeating: "0", count: 3 - clipped.count) + clipped
        let trimmed = normalized.trimmingCharacters(in: CharacterSet(charactersIn: "0"))
        return trimmed.isEmpty ? "0" : trimmed
    }

    static func sanitizeTempoInput(_ input: String) -> String {
        String(input.filter(\.isNumber).prefix(3))
    }

    static func sanitizeMetricInput(_ name: String, _ input: String) -> String {
        switch name {
        case "Tempo":
            return sanitizeTempoInput(input)
        case "Reps", "Rest", "RPE":
            return String(input.filter { $0.isNumber || $0 == "." }.prefix(6))
        default:
            return input
        }
    }
}