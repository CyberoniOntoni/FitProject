import SwiftUI

struct ProgramsView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedProgram: FPProgram?
    @State private var programWeeks: [FPProgramWeek] = []

    var body: some View {
        NavigationStack {
            ScrollView {
                if appState.programs.isEmpty {
                    emptyState
                } else {
                    LazyVStack(spacing: 16) {
                        ForEach(appState.programs) { program in
                            Button {
                                selectedProgram = program
                                programWeeks = appState.programWeeks[program.id] ?? []
                            } label: {
                                ProgramDetailCard(program: program)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 100)
                }
            }
            .background(BWSTheme.background)
            .navigationTitle("Programs")
            .navigationBarTitleDisplayMode(.large)
            .navigationDestination(item: $selectedProgram) { program in
                ProgramDetailView(program: program, weeks: programWeeks)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "list.bullet.rectangle")
                .font(.system(size: 48))
                .foregroundStyle(BWSTheme.textTertiary)
            Text("No programs yet")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text("Programs assigned on FitPros.io will appear here automatically.")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .padding(.top, 80)
    }
}

struct ProgramDetailCard: View {
    let program: FPProgram

    var body: some View {
        BWSCard(padding: 0) {
            VStack(alignment: .leading, spacing: 0) {
                ZStack(alignment: .bottomLeading) {
                    if let url = program.imageUrl, let imageURL = URL(string: url) {
                        AsyncImage(url: imageURL) { image in
                            image.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Rectangle().fill(BWSTheme.surfaceHighlight)
                        }
                        .frame(height: 120)
                    } else {
                        Rectangle()
                            .fill(BWSTheme.surfaceHighlight)
                            .frame(height: 120)
                    }

                    LinearGradient(
                        colors: [.clear, BWSTheme.surface],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 120)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text(program.title)
                        .font(BWSTheme.headlineFont)
                        .foregroundStyle(BWSTheme.textPrimary)

                    if !program.description.isEmpty {
                        Text(program.description)
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.textSecondary)
                            .lineLimit(2)
                    }

                    HStack(spacing: 16) {
                        Label("\(program.totalWeekCount) weeks", systemImage: "calendar")
                        Label("\(program.totalWorkoutCount) workouts", systemImage: "figure.strengthtraining.traditional")
                    }
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textTertiary)

                    HStack {
                        ProgressRing(progress: program.progress, size: 36, lineWidth: 3, showLabel: false)
                        Text("\(program.progressPercent)% complete")
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.accent)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundStyle(BWSTheme.textTertiary)
                    }
                }
                .padding(16)
            }
            .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
        }
    }
}

struct ProgramDetailView: View {
    @EnvironmentObject var appState: AppState
    let program: FPProgram
    let weeks: [FPProgramWeek]
    @State private var selectedWeek: Int = 0
    @State private var showWorkout = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                programStats

                weekPicker

                if selectedWeek < weeks.count {
                    let week = weeks.sorted(by: { $0.index < $1.index })[selectedWeek]
                    ForEach(week.workouts.sorted(by: { $0.index < $1.index })) { workout in
                        WorkoutListItem(workout: workout, program: program) {
                            appState.startWorkout(workout, program: program)
                            showWorkout = true
                        }
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 40)
        }
        .background(BWSTheme.background)
        .navigationTitle(program.title)
        .navigationBarTitleDisplayMode(.inline)
        .fullScreenCover(isPresented: $showWorkout) {
            if let session = appState.activeWorkoutSession {
                WorkoutSessionView(session: session)
            }
        }
    }

    private var programStats: some View {
        HStack(spacing: 0) {
            statItem(value: "\(program.totalWeekCount)", label: "Weeks")
            Divider().frame(height: 40)
            statItem(value: "\(program.totalWorkoutCount)", label: "Workouts")
            Divider().frame(height: 40)
            statItem(value: "\(program.progressPercent)%", label: "Complete")
        }
        .padding(.vertical, 16)
        .background(BWSTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
    }

    private func statItem(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundStyle(BWSTheme.accent)
            Text(label)
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
        }
        .frame(maxWidth: .infinity)
    }

    private var weekPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(weeks.enumerated()), id: \.element.id) { index, week in
                    Button {
                        selectedWeek = index
                    } label: {
                        Text(week.name)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(selectedWeek == index ? .white : BWSTheme.textSecondary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(selectedWeek == index ? BWSTheme.accent : BWSTheme.surface)
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }
}

struct WorkoutListItem: View {
    let workout: FPWorkout
    let program: FPProgram
    let onStart: () -> Void

    var body: some View {
        BWSCard {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(workout.name)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    Text("\(workout.exerciseCount) exercises")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }
                Spacer()
                Button("Start", action: onStart)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(BWSTheme.accent)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(BWSTheme.accent.opacity(0.15))
                    .clipShape(Capsule())
            }
        }
    }
}