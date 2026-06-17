import SwiftUI

struct WorkoutSessionView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) private var dismiss

    let session: AppState.WorkoutSessionState

    @State private var currentExerciseIndex: Int = 0
    @State private var loggedSets: [String: [FPLoggedSet]] = [:]
    @State private var notes: String = ""
    @State private var showCompleteAlert = false
    @State private var restSecondsRemaining: Int = 0
    @State private var restTimerActive = false
    @State private var showPRToast = false
    @State private var latestPR: FPPersonalRecord?
    @State private var elapsedSeconds: Int = 0
    @State private var timer: Timer?

    private var exercises: [FPWorkoutExercise] {
        session.workout.exercises.sorted { $0.index < $1.index }
    }

    private var currentExercise: FPWorkoutExercise? {
        guard currentExerciseIndex < exercises.count else { return nil }
        return exercises[currentExerciseIndex]
    }

    var body: some View {
        ZStack {
            BWSTheme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                sessionHeader
                exerciseProgressBar

                if let exercise = currentExercise {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            exerciseHeader(exercise)
                            setsSection(exercise)
                            notesSection
                        }
                        .padding(20)
                        .padding(.bottom, 120)
                    }
                }

                bottomBar
            }

            if restTimerActive {
                VStack {
                    Spacer()
                    RestTimerOverlay(
                        secondsRemaining: restSecondsRemaining,
                        onSkip: { stopRestTimer() },
                        onAdd30: { restSecondsRemaining += 30 }
                    )
                    .padding(.horizontal, 20)
                    .padding(.bottom, 100)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            if showPRToast, let pr = latestPR {
                VStack {
                    PRBadge()
                        .padding(.top, 60)
                    Text("\(pr.exerciseName) — \(pr.value) kg")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textPrimary)
                        .padding(.top, 4)
                    Spacer()
                }
                .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .onAppear {
            loggedSets = session.loggedSets
            notes = session.notes
            startElapsedTimer()
        }
        .onDisappear { timer?.invalidate() }
        .alert("Complete Workout?", isPresented: $showCompleteAlert) {
            Button("Complete", role: .none) {
                finishWorkout()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Your workout will sync to FitPros.io")
        }
    }

    private var sessionHeader: some View {
        HStack {
            Button { dismiss() } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(BWSTheme.textSecondary)
                    .frame(width: 36, height: 36)
                    .background(BWSTheme.surface)
                    .clipShape(Circle())
            }

            VStack(spacing: 2) {
                Text(session.workout.name)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(BWSTheme.textPrimary)
                Text(formatElapsed(elapsedSeconds))
                    .font(.system(size: 13, weight: .medium, design: .monospaced))
                    .foregroundStyle(BWSTheme.textSecondary)
            }

            Spacer()

            Button { showCompleteAlert = true } label: {
                Text("Finish")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(BWSTheme.accent)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }

    private var exerciseProgressBar: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Rectangle().fill(BWSTheme.surfaceHighlight)
                Rectangle()
                    .fill(BWSTheme.accentGradient)
                    .frame(width: geo.size.width * CGFloat(currentExerciseIndex + 1) / CGFloat(max(exercises.count, 1)))
            }
        }
        .frame(height: 3)
    }

    private func exerciseHeader(_ exercise: FPWorkoutExercise) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if exercise.headerVisible, let header = exercise.header {
                Text(header.uppercased())
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(BWSTheme.accent)
                    .tracking(1.5)
            }

            if let url = exercise.videoThumbnailURL {
                ZStack {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(16/9, contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(BWSTheme.surfaceHighlight)
                    }
                    .frame(height: 200)
                    .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))

                    if exercise.youtubeURL != nil {
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(.white.opacity(0.9))
                    }
                }
                .onTapGesture {
                    if let url = exercise.youtubeURL {
                        UIApplication.shared.open(url)
                    }
                }
            }

            Text(exercise.name)
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)

            if let coachNotes = exercise.coachNotes, !coachNotes.isEmpty {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "quote.opening")
                        .font(.caption)
                        .foregroundStyle(BWSTheme.accent)
                    Text(coachNotes)
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }
                .padding(12)
                .background(BWSTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }

            HStack {
                Text("Exercise \(currentExerciseIndex + 1) of \(exercises.count)")
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textTertiary)
                Spacer()
                Button("View History") {
                    appState.selectedTab = .history
                }
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.accentSecondary)
            }
        }
    }

    private func setsSection(_ exercise: FPWorkoutExercise) -> some View {
        let metrics = session.workout.metrics
            .filter { $0.workoutExerciseId == exercise.id }
            .sorted { $0.index < $1.index }

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("SET")
                    .frame(width: 24)
                ForEach(metrics, id: \.id) { metric in
                    Text(metric.name.uppercased())
                        .frame(minWidth: 48)
                }
                Spacer().frame(width: 32)
            }
            .font(.system(size: 11, weight: .bold))
            .foregroundStyle(BWSTheme.textTertiary)

            let sets = loggedSets[exercise.id] ?? []
            ForEach(Array(sets.enumerated()), id: \.element.id) { index, set in
                SetRowView(
                    setNumber: index + 1,
                    set: binding(for: exercise.id, index: index),
                    metrics: metrics
                ) {
                    handleSetComplete(exercise: exercise, set: sets[index])
                }
            }

            Button {
                addSet(for: exercise)
            } label: {
                HStack {
                    Image(systemName: "plus")
                    Text("Add Set")
                }
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(BWSTheme.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(BWSTheme.accent.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }

    private var notesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Notes")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(BWSTheme.textSecondary)
            TextField("Add a note...", text: $notes, axis: .vertical)
                .lineLimit(3...6)
                .padding(12)
                .background(BWSTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(BWSTheme.textPrimary)
        }
    }

    private var bottomBar: some View {
        HStack(spacing: 16) {
            Button {
                if currentExerciseIndex > 0 { currentExerciseIndex -= 1 }
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .frame(width: 48, height: 48)
                    .background(BWSTheme.surface)
                    .clipShape(Circle())
            }
            .disabled(currentExerciseIndex == 0)
            .opacity(currentExerciseIndex == 0 ? 0.4 : 1)

            BWSPrimaryButton(
                title: currentExerciseIndex < exercises.count - 1 ? "Next Exercise" : "Complete Workout",
                icon: currentExerciseIndex < exercises.count - 1 ? "arrow.right" : "checkmark"
            ) {
                if currentExerciseIndex < exercises.count - 1 {
                    currentExerciseIndex += 1
                } else {
                    showCompleteAlert = true
                }
            }

            Button {
                if currentExerciseIndex < exercises.count - 1 { currentExerciseIndex += 1 }
            } label: {
                Image(systemName: "chevron.right")
                    .font(.system(size: 18, weight: .semibold))
                    .frame(width: 48, height: 48)
                    .background(BWSTheme.surface)
                    .clipShape(Circle())
            }
            .disabled(currentExerciseIndex >= exercises.count - 1)
            .opacity(currentExerciseIndex >= exercises.count - 1 ? 0.4 : 1)
        }
        .foregroundStyle(BWSTheme.textPrimary)
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(BWSTheme.surfaceElevated)
    }

    private func binding(for exerciseId: String, index: Int) -> Binding<FPLoggedSet> {
        Binding(
            get: { loggedSets[exerciseId]?[index] ?? FPLoggedSet(id: UUID().uuidString, setNumber: index + 1) },
            set: { newValue in
                if loggedSets[exerciseId] == nil { loggedSets[exerciseId] = [] }
                loggedSets[exerciseId]![index] = newValue
            }
        )
    }

    private func addSet(for exercise: FPWorkoutExercise) {
        var sets = loggedSets[exercise.id] ?? []
        let lastSet = sets.last
        sets.append(FPLoggedSet(
            id: UUID().uuidString,
            setNumber: sets.count + 1,
            reps: lastSet?.reps,
            weight: lastSet?.weight,
            rpe: lastSet?.rpe,
            rest: lastSet?.rest,
            tempo: lastSet?.tempo,
            time: lastSet?.time
        ))
        loggedSets[exercise.id] = sets
    }

    private func handleSetComplete(exercise: FPWorkoutExercise, set: FPLoggedSet) {
        if var sets = loggedSets[exercise.id],
           let index = sets.firstIndex(where: { $0.id == set.id }) {
            if let pr = appState.checkForPR(exercise: exercise, set: sets[index]) {
                sets[index].isPR = true
                loggedSets[exercise.id] = sets
                latestPR = pr
                withAnimation { showPRToast = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                    withAnimation { showPRToast = false }
                }
            }
        }

        if let restStr = set.rest, let rest = Int(restStr), rest > 0 {
            startRestTimer(seconds: rest)
        }
    }

    private func startRestTimer(seconds: Int) {
        restSecondsRemaining = seconds
        restTimerActive = true
        Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { t in
            if restSecondsRemaining > 0 {
                restSecondsRemaining -= 1
            } else {
                t.invalidate()
                restTimerActive = false
            }
        }
    }

    private func stopRestTimer() {
        restTimerActive = false
        restSecondsRemaining = 0
    }

    private func startElapsedTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            elapsedSeconds += 1
        }
    }

    private func finishWorkout() {
        timer?.invalidate()
        if var activeSession = appState.activeWorkoutSession {
            activeSession.loggedSets = loggedSets
            activeSession.notes = notes
            activeSession.elapsedSeconds = elapsedSeconds
            if let pr = latestPR {
                activeSession.prCelebrations.append(pr)
            }
            appState.activeWorkoutSession = activeSession
        }
        Task {
            await appState.completeWorkout()
            dismiss()
        }
    }

    private func formatElapsed(_ seconds: Int) -> String {
        String(format: "%02d:%02d", seconds / 60, seconds % 60)
    }
}