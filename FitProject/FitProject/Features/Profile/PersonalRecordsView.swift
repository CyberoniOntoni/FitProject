import SwiftUI

struct PersonalRecordsView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        ScrollView {
            if appState.personalRecords.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "trophy")
                        .font(.system(size: 48))
                        .foregroundStyle(BWSTheme.textTertiary)
                    Text("No personal records yet")
                        .font(BWSTheme.headlineFont)
                        .foregroundStyle(BWSTheme.textPrimary)
                    Text("PRs are logged automatically when you beat your best during workouts.")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                }
                .padding(.top, 80)
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(appState.personalRecords) { record in
                        BWSCard {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack(spacing: 6) {
                                        Image(systemName: "trophy.fill")
                                            .foregroundStyle(BWSTheme.prGold)
                                        Text("PR")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundStyle(BWSTheme.prGold)
                                    }
                                    Text(record.exerciseName)
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundStyle(BWSTheme.textPrimary)
                                    Text(record.date.formatted(.dateTime.month(.abbreviated).day().year()))
                                        .font(BWSTheme.captionFont)
                                        .foregroundStyle(BWSTheme.textSecondary)
                                    if let previous = record.previousValue {
                                        Text("Previous: \(previous)")
                                            .font(BWSTheme.captionFont)
                                            .foregroundStyle(BWSTheme.textTertiary)
                                    }
                                }
                                Spacer()
                                Text("\(record.value) \(record.metric)")
                                    .font(.system(size: 22, weight: .bold, design: .rounded))
                                    .foregroundStyle(BWSTheme.accent)
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
        }
        .background(BWSTheme.background)
        .navigationTitle("Personal Records")
        .navigationBarTitleDisplayMode(.inline)
    }
}