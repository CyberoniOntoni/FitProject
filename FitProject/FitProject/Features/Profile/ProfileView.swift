import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var appState: AppState
    @State private var showSignOutAlert = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    profileHeader
                    syncStatus
                    trackAndRecordSection
                    settingsSection
                    signOutButton
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
            .background(BWSTheme.background)
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .alert("Sign Out?", isPresented: $showSignOutAlert) {
                Button("Sign Out", role: .destructive) { appState.signOut() }
                Button("Cancel", role: .cancel) {}
            }
        }
    }

    private var profileHeader: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(BWSTheme.accentGradient)
                    .frame(width: 80, height: 80)
                Text(appState.authService.currentUser?.initials ?? "?")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(.white)
            }

            Text(appState.authService.currentUser?.displayName ?? "User")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)

            Text(appState.authService.currentUser?.email ?? "")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
        }
        .padding(.top, 20)
    }

    private var syncStatus: some View {
        BWSCard {
            HStack {
                Image(systemName: "arrow.triangle.2.circlepath")
                    .foregroundStyle(BWSTheme.accent)
                VStack(alignment: .leading, spacing: 2) {
                    Text("FitPros.io Sync")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    if let date = appState.syncEngine.lastSyncDate {
                        Text("Last synced \(date.formatted(.relative(presentation: .named)))")
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.textSecondary)
                    } else {
                        Text("Syncing...")
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.textSecondary)
                    }
                }
                Spacer()
                if appState.syncEngine.isSyncing {
                    ProgressView()
                } else {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(BWSTheme.success)
                }
            }
        }
    }

    private var trackAndRecordSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Track & Record")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)

            VStack(spacing: 0) {
                NavigationLink { HabitsView() } label: {
                    ProfileMenuRow(
                        icon: "checkmark.circle",
                        title: "Habits",
                        subtitle: "Record daily habits · \(appState.habits.count) active"
                    )
                }
                .buttonStyle(.plain)
                ProfileMenuDivider()
                NavigationLink { ProgressPhotosView() } label: {
                    ProfileMenuRow(
                        icon: "camera",
                        title: "Progress Photos",
                        subtitle: "Visual body transformation · \(appState.progressSessions.count) sessions"
                    )
                }
                .buttonStyle(.plain)
                ProfileMenuDivider()
                NavigationLink { MeasurementsView() } label: {
                    ProfileMenuRow(
                        icon: "ruler",
                        title: "Body Measurements",
                        subtitle: "Weight, body comp & more · \(appState.measurements.count) entries"
                    )
                }
                .buttonStyle(.plain)
                ProfileMenuDivider()
                NavigationLink { PersonalRecordsView() } label: {
                    ProfileMenuRow(
                        icon: "trophy",
                        title: "Personal Records",
                        subtitle: "\(appState.personalRecords.count) records synced from workouts"
                    )
                }
                .buttonStyle(.plain)
                ProfileMenuDivider()
                NavigationLink { FormsListView() } label: {
                    ProfileMenuRow(
                        icon: "doc.text",
                        title: "Forms",
                        subtitle: pendingFormsCount
                    )
                }
                .buttonStyle(.plain)
            }
            .background(BWSTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: BWSTheme.cardRadius)
                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
            )
        }
    }

    private var settingsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Settings")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)

            NavigationLink { SettingsView() } label: {
                ProfileMenuRow(
                    icon: "gearshape",
                    title: "Unit Preferences",
                    subtitle: "Weight, circumference & more · \(appState.unitPreferences.massAbbreviation)"
                )
            }
            .buttonStyle(.plain)
            .background(BWSTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: BWSTheme.cardRadius)
                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
            )
        }
    }

    private var pendingFormsCount: String {
        guard let userId = appState.authService.currentUser?.id else { return "0 pending" }
        let pending = appState.forms.filter { !$0.isCompleted(for: userId) }.count
        return "\(pending) pending"
    }

    private var signOutButton: some View {
        Button {
            showSignOutAlert = true
        } label: {
            Text("Sign Out")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(BWSTheme.error)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(BWSTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: BWSTheme.buttonRadius))
        }
    }
}