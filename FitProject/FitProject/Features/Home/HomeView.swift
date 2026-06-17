import SwiftUI

struct HomeView: View {
    @EnvironmentObject var appState: AppState
    @State private var showWorkoutSession = false

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12: return "Good morning"
        case 12..<17: return "Good afternoon"
        default: return "Good evening"
        }
    }

    private var dateString: String {
        Date().formatted(.dateTime.weekday(.wide).month(.wide).day())
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                headerSection
                weeklyProgressSection
                continueTrainingSection
                habitsSection
                programsPreviewSection
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 100)
        }
        .background(BWSTheme.background)
        .fullScreenCover(isPresented: $showWorkoutSession) {
            if let session = appState.activeWorkoutSession {
                WorkoutSessionView(session: session)
            }
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(greeting + ",")
                .font(BWSTheme.titleFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text(appState.authService.currentUser?.firstName.isEmpty == false
                 ? appState.authService.currentUser!.firstName
                 : "Athlete")
                .font(BWSTheme.titleFont)
                .foregroundStyle(BWSTheme.accent)
            Text(dateString)
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .padding(.top, 4)
        }
        .padding(.top, 8)
    }

    private var weeklyProgressSection: some View {
        BWSCard {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("This Week")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(BWSTheme.textPrimary)
                        Text("\(appState.weeklyWorkoutsCompleted) of \(appState.weeklyWorkoutGoal) workouts")
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.textSecondary)
                    }
                    Spacer()
                    ProgressRing(
                        progress: Double(appState.weeklyWorkoutsCompleted) / Double(appState.weeklyWorkoutGoal),
                        size: 52
                    )
                }
                WeekProgressDots(
                    completed: appState.weeklyWorkoutsCompleted,
                    total: appState.weeklyWorkoutGoal
                )
            }
        }
    }

    private var continueTrainingSection: some View {
        Group {
            if let workout = appState.nextWorkout, let program = appState.nextProgram {
                BWSCard(padding: 0) {
                    VStack(alignment: .leading, spacing: 0) {
                        if let url = workout.exercises.first?.videoThumbnailURL {
                            AsyncImage(url: url) { image in
                                image.resizable().aspectRatio(contentMode: .fill)
                            } placeholder: {
                                Rectangle().fill(BWSTheme.surfaceHighlight)
                            }
                            .frame(height: 140)
                            .clipped()
                        }

                        VStack(alignment: .leading, spacing: 12) {
                            Text("Continue Training")
                                .font(BWSTheme.captionFont)
                                .foregroundStyle(BWSTheme.accent)
                                .textCase(.uppercase)
                                .tracking(1)

                            Text(workout.name)
                                .font(BWSTheme.headlineFont)
                                .foregroundStyle(BWSTheme.textPrimary)

                            Text(program.title)
                                .font(BWSTheme.captionFont)
                                .foregroundStyle(BWSTheme.textSecondary)

                            HStack {
                                Label("\(workout.exerciseCount) exercises", systemImage: "list.bullet")
                                Spacer()
                                Text("\(program.completedWorkouts)/\(program.totalWorkoutCount)")
                                    .foregroundStyle(BWSTheme.accent)
                            }
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.textSecondary)

                            BWSPrimaryButton(title: "Start Workout", icon: "play.fill") {
                                appState.startWorkout(workout, program: program)
                                showWorkoutSession = true
                            }
                        }
                        .padding(16)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
                }
            }
        }
    }

    private var habitsSection: some View {
        Group {
            if !appState.habits.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Habits")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(BWSTheme.textPrimary)
                        Spacer()
                        NavigationLink {
                            HabitsView()
                        } label: {
                            Text("See all")
                                .font(BWSTheme.captionFont)
                                .foregroundStyle(BWSTheme.accent)
                        }
                    }

                    ForEach(appState.habits) { habit in
                        HabitCounterView(habit: habit) { newValue in
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
            }
        }
    }

    private var programsPreviewSection: some View {
        Group {
            if !appState.programs.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Your Programs")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(BWSTheme.textPrimary)
                        Spacer()
                        Button("See All") {
                            appState.selectedTab = .programs
                        }
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.accent)
                    }

                    ForEach(appState.programs.prefix(2)) { program in
                        ProgramCard(program: program)
                    }
                }
            }
        }
    }
}

struct ProgramCard: View {
    let program: FPProgram

    var body: some View {
        BWSCard {
            HStack(spacing: 14) {
                if let url = program.imageUrl, let imageURL = URL(string: url) {
                    AsyncImage(url: imageURL) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(BWSTheme.surfaceHighlight)
                    }
                    .frame(width: 56, height: 56)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                } else {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(BWSTheme.surfaceHighlight)
                        .frame(width: 56, height: 56)
                        .overlay(
                            Image(systemName: "dumbbell.fill")
                                .foregroundStyle(BWSTheme.accent)
                        )
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(program.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    Text("\(program.totalWeekCount) weeks · \(program.totalWorkoutCount) workouts")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }

                Spacer()

                ProgressRing(progress: program.progress, size: 40, lineWidth: 4)
            }
        }
    }
}