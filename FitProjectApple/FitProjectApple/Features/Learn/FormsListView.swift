import SwiftUI

struct FormsListView: View {
    @EnvironmentObject var appState: AppState

    private var userId: String? { appState.authService.currentUser?.id }

    var body: some View {
        ScrollView {
            if appState.forms.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "doc.text")
                        .font(.system(size: 48))
                        .foregroundStyle(BWSTheme.textTertiary)
                    Text("No forms assigned")
                        .font(BWSTheme.headlineFont)
                        .foregroundStyle(BWSTheme.textPrimary)
                    Text("Check-in forms from your coach will appear here.")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                }
                .padding(.top, 80)
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(appState.forms) { form in
                        if let userId, form.isCompleted(for: userId) {
                            completedRow(form)
                        } else {
                            NavigationLink {
                                FormFillView(form: form)
                            } label: {
                                formRow(form, completed: false)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
        }
        .background(BWSTheme.background)
        .navigationTitle("Forms")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func formRow(_ form: FPForm, completed: Bool) -> some View {
        BWSCard {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(form.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    if let desc = form.description {
                        Text(desc)
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.textSecondary)
                            .lineLimit(2)
                    }
                }
                Spacer()
                if completed {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(BWSTheme.success)
                } else {
                    Text("Pending")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(BWSTheme.warning)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(BWSTheme.warning.opacity(0.15))
                        .clipShape(Capsule())
                    Image(systemName: "chevron.right")
                        .foregroundStyle(BWSTheme.textTertiary)
                }
            }
        }
    }

    private func completedRow(_ form: FPForm) -> some View {
        formRow(form, completed: true)
    }
}