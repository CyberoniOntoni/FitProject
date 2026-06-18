import SwiftUI

struct LearnView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedContent: FPContent?
    @State private var selectedForm: FPForm?

    private var userId: String? { appState.authService.currentUser?.id }

    var body: some View {
        NavigationStack {
            ScrollView {
                if appState.content.isEmpty && appState.forms.isEmpty {
                    emptyState
                } else {
                    if !appState.forms.isEmpty {
                        formsSection
                    }
                    if !appState.content.isEmpty {
                        contentSection
                    }
                }
            }
            .background(BWSTheme.background)
            .navigationTitle("Learn")
            .navigationBarTitleDisplayMode(.large)
            .navigationDestination(item: $selectedContent) { content in
                ContentDetailView(content: content)
            }
            .navigationDestination(item: $selectedForm) { form in
                FormFillView(form: form)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "book.fill")
                .font(.system(size: 48))
                .foregroundStyle(BWSTheme.textTertiary)
            Text("No content yet")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text("Guides, articles, and check-in forms from your coach will appear here.")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .padding(.top, 80)
    }

    private var formsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Check-In Forms")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)
                .padding(.horizontal, 20)

            ForEach(appState.forms) { form in
                Group {
                    if let userId, form.isCompleted(for: userId) {
                        formCard(form, completed: true)
                    } else {
                        Button { selectedForm = form } label: {
                            formCard(form, completed: false)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
        .padding(.bottom, 16)
    }

    private func formCard(_ form: FPForm, completed: Bool) -> some View {
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

    private var contentSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Guides & Articles")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)
                .padding(.horizontal, 20)

            LazyVStack(spacing: 12) {
                ForEach(appState.content) { item in
                    Button { selectedContent = item } label: {
                        BWSCard(padding: 0) {
                            HStack(spacing: 0) {
                                if let url = item.imageUrl, let imageURL = URL(string: url) {
                                    AsyncImage(url: imageURL) { image in
                                        image.resizable().aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        Rectangle().fill(BWSTheme.surfaceHighlight)
                                    }
                                    .frame(width: 80, height: 80)
                                    .clipped()
                                }

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.title)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(BWSTheme.textPrimary)
                                        .lineLimit(2)
                                    Text(item.type.capitalized)
                                        .font(BWSTheme.captionFont)
                                        .foregroundStyle(BWSTheme.accent)
                                }
                                .padding(12)

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .foregroundStyle(BWSTheme.textTertiary)
                                    .padding(.trailing, 12)
                            }
                            .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 100)
        }
    }
}

struct ContentDetailView: View {
    let content: FPContent

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let url = content.imageUrl, let imageURL = URL(string: url) {
                    AsyncImage(url: imageURL) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(BWSTheme.surfaceHighlight)
                    }
                    .frame(height: 200)
                    .clipShape(RoundedRectangle(cornerRadius: BWSTheme.cardRadius))
                }

                Text(content.title)
                    .font(BWSTheme.headlineFont)
                    .foregroundStyle(BWSTheme.textPrimary)

                if let body = content.body {
                    Text(body)
                        .font(BWSTheme.bodyFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                        .lineSpacing(4)
                }
            }
            .padding(20)
        }
        .background(BWSTheme.background)
        .navigationBarTitleDisplayMode(.inline)
    }
}