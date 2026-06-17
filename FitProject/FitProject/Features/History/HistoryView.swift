import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedLog: FPWorkoutLog?

    var body: some View {
        NavigationStack {
            ScrollView {
                if appState.workoutLogs.isEmpty {
                    emptyState
                } else {
                    recordsSection
                    historyList
                }
            }
            .background(BWSTheme.background)
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.large)
            .navigationDestination(item: $selectedLog) { log in
                WorkoutLogDetailView(log: log)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 48))
                .foregroundStyle(BWSTheme.textTertiary)
            Text("No workout history")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text("Completed workouts sync automatically with FitPros.io")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .padding(.top, 80)
    }

    private var recordsSection: some View {
        Group {
            if !appState.personalRecords.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Personal Records")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                        .padding(.horizontal, 20)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(appState.personalRecords.prefix(10)) { record in
                                BWSCard {
                                    VStack(alignment: .leading, spacing: 4) {
                                        HStack {
                                            Image(systemName: "trophy.fill")
                                                .foregroundStyle(BWSTheme.prGold)
                                            Text("PR")
                                                .font(.system(size: 11, weight: .bold))
                                                .foregroundStyle(BWSTheme.prGold)
                                        }
                                        Text(record.exerciseName)
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundStyle(BWSTheme.textPrimary)
                                            .lineLimit(1)
                                        Text("\(record.value) \(record.metric)")
                                            .font(.system(size: 18, weight: .bold, design: .rounded))
                                            .foregroundStyle(BWSTheme.accent)
                                    }
                                    .frame(width: 140)
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                }
                .padding(.bottom, 8)
            }
        }
    }

    private var historyList: some View {
        LazyVStack(spacing: 12) {
            ForEach(appState.workoutLogs) { log in
                Button { selectedLog = log } label: {
                    HistoryCard(log: log)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 100)
    }
}

struct HistoryCard: View {
    let log: FPWorkoutLog

    var body: some View {
        BWSCard {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text(log.workoutName)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)

                    HStack(spacing: 12) {
                        if let date = log.completedAt {
                            Label(date.formatted(.dateTime.month(.abbreviated).day()), systemImage: "calendar")
                        }
                        if let duration = log.durationSeconds {
                            Label(formatDuration(duration), systemImage: "clock")
                        }
                        Label("\(log.exercises.count)", systemImage: "list.bullet")
                    }
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)

                    if log.prCount > 0 {
                        PRBadge()
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(BWSTheme.textTertiary)
            }
        }
    }

    private func formatDuration(_ seconds: Int) -> String {
        "\(seconds / 60) min"
    }
}

struct WorkoutLogDetailView: View {
    let log: FPWorkoutLog

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let date = log.completedAt {
                    Text(date.formatted(.dateTime.weekday(.wide).month(.wide).day().hour().minute()))
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }

                HStack(spacing: 20) {
                    if let duration = log.durationSeconds {
                        detailStat("Duration", value: "\(duration / 60) min")
                    }
                    detailStat("Volume", value: String(format: "%.0f kg", log.totalVolume))
                    if log.prCount > 0 {
                        detailStat("PRs", value: "\(log.prCount)")
                    }
                }

                if let notes = log.notes, !notes.isEmpty {
                    BWSCard {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Notes")
                                .font(BWSTheme.captionFont)
                                .foregroundStyle(BWSTheme.textSecondary)
                            Text(notes)
                                .foregroundStyle(BWSTheme.textPrimary)
                        }
                    }
                }

                ForEach(log.exercises) { exercise in
                    BWSCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(exercise.name)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(BWSTheme.textPrimary)

                            ForEach(exercise.sets.filter(\.isCompleted)) { set in
                                HStack {
                                    Text("Set \(set.setNumber)")
                                        .font(BWSTheme.captionFont)
                                        .foregroundStyle(BWSTheme.textTertiary)
                                        .frame(width: 50, alignment: .leading)
                                    if let weight = set.weight, let reps = set.reps {
                                        Text("\(weight) kg × \(reps)")
                                            .font(BWSTheme.metricFont)
                                            .foregroundStyle(BWSTheme.textPrimary)
                                    }
                                    if set.isPR {
                                        PRBadge()
                                    }
                                    Spacer()
                                }
                            }
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(BWSTheme.background)
        .navigationTitle(log.workoutName)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func detailStat(_ label: String, value: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 18, weight: .bold, design: .rounded))
                .foregroundStyle(BWSTheme.accent)
            Text(label)
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
        }
    }
}