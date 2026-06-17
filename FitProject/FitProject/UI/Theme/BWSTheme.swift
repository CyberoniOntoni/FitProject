import SwiftUI

enum BWSTheme {
    // BWS+-inspired dark palette with FitPros integration accents
    static let background = Color(hex: "0D0D0F")
    static let surface = Color(hex: "1A1A1E")
    static let surfaceElevated = Color(hex: "242428")
    static let surfaceHighlight = Color(hex: "2E2E34")

    static let accent = Color(hex: "00C9B7")
    static let accentSecondary = Color(hex: "3B82F6")
    static let accentGradient = LinearGradient(
        colors: [Color(hex: "00C9B7"), Color(hex: "3B82F6")],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let textPrimary = Color.white
    static let textSecondary = Color(hex: "A0A0A8")
    static let textTertiary = Color(hex: "6B6B73")

    static let success = Color(hex: "34D399")
    static let warning = Color(hex: "FBBF24")
    static let error = Color(hex: "F87171")
    static let prGold = Color(hex: "FFD700")

    // Metric colors (FitPros standard)
    static let repsColor = Color(hex: "513BD1")
    static let weightColor = Color(hex: "FC4747")
    static let restColor = Color(hex: "4BD685")
    static let rpeColor = Color(hex: "F5A623")
    static let tempoColor = Color(hex: "9B59B6")
    static let timeColor = Color(hex: "3B86D1")

    static let cardRadius: CGFloat = 16
    static let buttonRadius: CGFloat = 12
    static let tabBarHeight: CGFloat = 84

    static let titleFont = Font.system(size: 28, weight: .bold, design: .rounded)
    static let headlineFont = Font.system(size: 20, weight: .semibold, design: .rounded)
    static let bodyFont = Font.system(size: 16, weight: .regular, design: .default)
    static let captionFont = Font.system(size: 13, weight: .medium, design: .default)
    static let metricFont = Font.system(size: 15, weight: .semibold, design: .monospaced)
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r, g, b: UInt64
        switch hex.count {
        case 6:
            (r, g, b) = ((int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            (r, g, b) = (0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: 1
        )
    }
}