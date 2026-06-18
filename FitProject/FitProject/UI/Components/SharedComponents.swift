import SwiftUI

// MARK: - BWS Card

struct BWSCard<Content: View>: View {
    let content: Content
    var padding: CGFloat = 16

    init(padding: CGFloat = 16, @ViewBuilder content: () -> Content) {
        self.padding = padding
        self.content = content()
    }

    var body: some View {
        content
            .padding(padding)
            .background(BWSTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: BWSTheme.cardRadius)
                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
            )
    }
}

// MARK: - Progress Ring

struct ProgressRing: View {
    let progress: Double
    var size: CGFloat = 56
    var lineWidth: CGFloat = 5
    var showLabel: Bool = true

    var body: some View {
        ZStack {
            Circle()
                .stroke(BWSTheme.surfaceHighlight, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: min(progress, 1.0))
                .stroke(BWSTheme.accentGradient, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.spring(response: 0.6), value: progress)
            if showLabel {
                Text("\(Int(progress * 100))%")
                    .font(.system(size: size * 0.22, weight: .bold, design: .rounded))
                    .foregroundStyle(BWSTheme.textPrimary)
            }
        }
        .frame(width: size, height: size)
    }
}

// MARK: - Week Dots

struct WeekProgressDots: View {
    let completed: Int
    let total: Int

    var body: some View {
        HStack(spacing: 8) {
            ForEach(1...max(total, 1), id: \.self) { day in
                Circle()
                    .fill(day <= completed ? BWSTheme.accent : BWSTheme.surfaceHighlight)
                    .frame(width: 10, height: 10)
                    .overlay(
                        Circle()
                            .stroke(day == completed + 1 ? BWSTheme.accent.opacity(0.5) : Color.clear, lineWidth: 2)
                            .frame(width: 14, height: 14)
                    )
            }
        }
    }
}

// MARK: - Primary Button

struct BWSPrimaryButton: View {
    let title: String
    var icon: String? = nil
    var isLoading: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    if let icon {
                        Image(systemName: icon)
                    }
                    Text(title)
                        .font(.system(size: 17, weight: .semibold))
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(BWSTheme.accentGradient)
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: BWSTheme.buttonRadius))
        }
        .disabled(isLoading)
    }
}

// MARK: - Metric Input Row

struct MetricInputRow: View {
    let label: String
    let color: Color
    @Binding var values: [String]
    let setCount: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Circle().fill(color).frame(width: 8, height: 8)
                Text(label)
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)
            }
            HStack(spacing: 8) {
                ForEach(0..<setCount, id: \.self) { index in
                    TextField("—", text: binding(for: index))
                        .font(BWSTheme.metricFont)
                        .multilineTextAlignment(.center)
                        .padding(.vertical, 10)
                        .background(BWSTheme.surfaceHighlight)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .keyboardType(label == "Reps" || label == "Weight" || label == "RPE" ? .decimalPad : .default)
                }
            }
        }
    }

    private func binding(for index: Int) -> Binding<String> {
        Binding(
            get: { index < values.count ? values[index] : "" },
            set: { newValue in
                while values.count <= index { values.append("") }
                values[index] = newValue
            }
        )
    }
}

// MARK: - Set Row

struct SetRowView: View {
    let setNumber: Int
    @Binding var set: FPLoggedSet
    let metrics: [FPWorkoutMetric]
    var onComplete: () -> Void

    var body: some View {
        HStack(spacing: 6) {
            Text("\(setNumber)")
                .font(BWSTheme.metricFont)
                .foregroundStyle(BWSTheme.textTertiary)
                .frame(width: 24)

            ForEach(metrics.sorted(by: { $0.index < $1.index }), id: \.id) { metric in
                metricField(for: metric)
            }

            Button(action: {
                set.isCompleted.toggle()
                if set.isCompleted { onComplete() }
            }) {
                Image(systemName: set.isCompleted ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(set.isCompleted ? BWSTheme.accent : BWSTheme.textTertiary)
            }
            .frame(width: 32)
        }
        .padding(.vertical, 4)
        .background(set.isPR ? BWSTheme.prGold.opacity(0.08) : Color.clear)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    @ViewBuilder
    private func metricField(for metric: FPWorkoutMetric) -> some View {
        let binding = metricBinding(for: metric.name)
        TextField("—", text: binding)
            .font(BWSTheme.metricFont)
            .multilineTextAlignment(.center)
            .frame(minWidth: 48)
            .padding(.vertical, 8)
            .background(metricColor(metric.name).opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .keyboardType(.decimalPad)
    }

    private func metricBinding(for name: String) -> Binding<String> {
        switch name {
        case "Reps": return $set.reps.mapped(default: "")
        case "Weight": return $set.weight.mapped(default: "")
        case "RPE": return $set.rpe.mapped(default: "")
        case "Rest": return $set.rest.mapped(default: "")
        case "Tempo": return $set.tempo.mapped(default: "")
        case "Time": return $set.time.mapped(default: "")
        default: return .constant("")
        }
    }

    private func metricColor(_ name: String) -> Color {
        switch name {
        case "Reps": return BWSTheme.repsColor
        case "Weight": return BWSTheme.weightColor
        case "RPE": return BWSTheme.rpeColor
        case "Rest": return BWSTheme.restColor
        case "Tempo": return BWSTheme.tempoColor
        case "Time": return BWSTheme.timeColor
        default: return BWSTheme.textSecondary
        }
    }
}

// MARK: - PR Badge

struct PRBadge: View {
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "trophy.fill")
            Text("New PR!")
        }
        .font(.system(size: 12, weight: .bold))
        .foregroundStyle(BWSTheme.prGold)
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .background(BWSTheme.prGold.opacity(0.15))
        .clipShape(Capsule())
    }
}

// MARK: - Rest Timer Overlay

struct RestTimerOverlay: View {
    let secondsRemaining: Int
    let onSkip: () -> Void
    let onAdd30: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("REST")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(BWSTheme.textSecondary)
                .tracking(2)

            Text(formatTime(secondsRemaining))
                .font(.system(size: 48, weight: .bold, design: .monospaced))
                .foregroundStyle(BWSTheme.accent)

            HStack(spacing: 16) {
                Button("+30s") { onAdd30() }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(BWSTheme.textSecondary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(BWSTheme.surfaceHighlight)
                    .clipShape(Capsule())

                Button("Skip") { onSkip() }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(BWSTheme.accent)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(BWSTheme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
        .shadow(color: .black.opacity(0.4), radius: 20, y: 8)
    }

    private func formatTime(_ seconds: Int) -> String {
        String(format: "%d:%02d", seconds / 60, seconds % 60)
    }
}

// MARK: - Habit Counter

struct HabitCounterView: View {
    let habit: FPHabit
    let onUpdate: (Double) -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(habit.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(BWSTheme.textPrimary)
                Text(habit.progressText)
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)
            }
            Spacer()
            HStack(spacing: 12) {
                Button {
                    onUpdate(max(0, habit.currentValue - 1))
                } label: {
                    Image(systemName: "minus")
                        .frame(width: 36, height: 36)
                        .background(BWSTheme.surfaceHighlight)
                        .clipShape(Circle())
                }
                Text(habit.currentValue.truncatingRemainder(dividingBy: 1) == 0
                     ? String(format: "%.0f", habit.currentValue)
                     : String(format: "%.1f", habit.currentValue))
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .frame(minWidth: 40)
                Button {
                    onUpdate(habit.currentValue + 1)
                } label: {
                    Image(systemName: "plus")
                        .frame(width: 36, height: 36)
                        .background(BWSTheme.accent.opacity(0.2))
                        .clipShape(Circle())
                }
            }
            .foregroundStyle(BWSTheme.textPrimary)
        }
        .padding(14)
        .background(BWSTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Profile Menu Row

struct ProfileMenuRow: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundStyle(BWSTheme.accent)
                .frame(width: 28, alignment: .leading)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(BWSTheme.textPrimary)
                Text(subtitle)
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)
            }

            Spacer(minLength: 8)

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(BWSTheme.textTertiary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
    }
}

struct ProfileMenuDivider: View {
    var body: some View {
        Divider()
            .overlay(BWSTheme.surfaceHighlight)
            .padding(.leading, 52)
    }
}

// MARK: - Helpers

extension Binding where Value == String? {
    func mapped(default defaultValue: String) -> Binding<String> {
        Binding<String>(
            get: { self.wrappedValue ?? defaultValue },
            set: { self.wrappedValue = $0.isEmpty ? nil : $0 }
        )
    }
}