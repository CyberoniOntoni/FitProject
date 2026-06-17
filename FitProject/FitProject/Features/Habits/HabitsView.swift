import SwiftUI

struct HabitsView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        ScrollView {
            if appState.habits.isEmpty {
                emptyState
            } else {
                VStack(spacing: 16) {
                    summaryCard
                    ForEach(appState.habits) { habit in
                        HabitDetailCard(habit: habit) { newValue in
                            Task {
                                guard let userId = appState.authService.currentUser?.id else { return }
                                try? await appState.syncEngine.pushHabitUpdate(habitId: habit.id, userId: userId, value: newValue)
                                if let idx = appState.habits.firstIndex(where: { $0.id == habit.id }) {
                                    appState.habits[idx].currentValue = newValue
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
        }
        .background(BWSTheme.background)
        .navigationTitle("Habits")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle")
                .font(.system(size: 48))
                .foregroundStyle(BWSTheme.textTertiary)
            Text("No habits tracked")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text("Habits set up on FitPros.io sync here automatically.")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .padding(.top, 60)
    }

    private var summaryCard: some View {
        BWSCard {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Today's Progress")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    let completed = appState.habits.filter { $0.progress >= 1 }.count
                    Text("\(completed) of \(appState.habits.count) habits complete")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }
                Spacer()
                ProgressRing(
                    progress: appState.habits.isEmpty ? 0 :
                        Double(appState.habits.filter { $0.progress >= 1 }.count) / Double(appState.habits.count)
                )
            }
        }
    }
}

struct HabitDetailCard: View {
    let habit: FPHabit
    let onUpdate: (Double) -> Void
    @State private var inputValue = ""

    var body: some View {
        BWSCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(habit.name)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    Spacer()
                    if habit.streak > 0 {
                        HStack(spacing: 4) {
                            Image(systemName: "flame.fill")
                                .foregroundStyle(BWSTheme.warning)
                            Text("\(habit.streak) day streak")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(BWSTheme.warning)
                        }
                    }
                }

                if let desc = habit.description {
                    Text(desc)
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }

                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(BWSTheme.surfaceHighlight)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(BWSTheme.accentGradient)
                            .frame(width: geo.size.width * habit.progress)
                    }
                }
                .frame(height: 6)

                Text(habit.progress >= 1 ? "✓ Complete" : habit.progressText)
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(habit.progress >= 1 ? BWSTheme.success : BWSTheme.textSecondary)

                HStack(spacing: 12) {
                    Text("Log value:")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                    TextField("0", text: $inputValue)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(BWSTheme.surfaceHighlight)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .frame(width: 100)
                    Button("Save") {
                        if let value = Double(inputValue) {
                            onUpdate(value)
                        }
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(BWSTheme.accentGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }

                HStack(spacing: 8) {
                    quickAction("-1") { onUpdate(max(0, habit.currentValue - 1)) }
                    quickAction("+1") { onUpdate(habit.currentValue + 1) }
                    quickAction("Hit target") { onUpdate(habit.targetValue) }
                }
            }
        }
        .onAppear { syncInput() }
        .onChange(of: habit.currentValue) { _, _ in syncInput() }
    }

    private func syncInput() {
        inputValue = habit.currentValue.truncatingRemainder(dividingBy: 1) == 0
            ? String(format: "%.0f", habit.currentValue)
            : String(format: "%.1f", habit.currentValue)
    }

    private func quickAction(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(BWSTheme.surfaceHighlight)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }
}