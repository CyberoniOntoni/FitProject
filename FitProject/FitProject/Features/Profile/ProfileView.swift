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
                    menuSection
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

    private var menuSection: some View {
        VStack(spacing: 2) {
            Text("Track & Record")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, 4)

            NavigationLink {
                HabitsView()
            } label: {
                trackMenuRow(
                    icon: "checkmark.circle",
                    title: "Habits",
                    subtitle: "Record daily habits · \(appState.habits.count) active"
                )
            }

            NavigationLink {
                MeasurementsView()
            } label: {
                trackMenuRow(
                    icon: "ruler",
                    title: "Body Measurements",
                    subtitle: "Weight, body comp & more · \(appState.measurements.count) entries"
                )
            }

            NavigationLink {
                ProgressPhotosView()
            } label: {
                trackMenuRow(
                    icon: "camera",
                    title: "Progress Photos",
                    subtitle: "Visual body transformation · \(appState.progressSessions.count) sessions"
                )
            }

            NavigationLink {
                PersonalRecordsView()
            } label: {
                trackMenuRow(
                    icon: "trophy",
                    title: "Personal Records",
                    subtitle: "\(appState.personalRecords.count) records logged"
                )
            }

            NavigationLink {
                FormsListView()
            } label: {
                trackMenuRow(
                    icon: "doc.text",
                    title: "Forms",
                    subtitle: pendingFormsCount
                )
            }
        }
        .background(BWSTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
    }

    private func trackMenuRow(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundStyle(BWSTheme.accent)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(BWSTheme.textPrimary)
                Text(subtitle)
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(BWSTheme.textTertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
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