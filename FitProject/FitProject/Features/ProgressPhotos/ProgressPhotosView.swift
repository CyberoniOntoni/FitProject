import SwiftUI

struct ProgressPhotosView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        ScrollView {
            if appState.progressSessions.isEmpty {
                emptyState
            } else {
                VStack(spacing: 16) {
                    ForEach(appState.progressSessions) { session in
                        sessionCard(session)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
        }
        .background(BWSTheme.background)
        .navigationTitle("Progress Photos")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera")
                .font(.system(size: 48))
                .foregroundStyle(BWSTheme.textTertiary)
            Text("No progress photos yet")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text("Take front, side, and back photos on FitPros.io to track your transformation over time. They sync here automatically.")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .padding(.top, 60)
    }

    private func sessionCard(_ session: FPProgressSession) -> some View {
        BWSCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(session.dateCreated.formatted(.dateTime.month(.wide).day().year()))
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    Spacer()
                    Text("\(session.pictures.count) photo\(session.pictures.count == 1 ? "" : "s")")
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }

                if let notes = session.notes, !notes.isEmpty {
                    Text(notes)
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }

                HStack(spacing: 8) {
                    ForEach(session.pictures) { picture in
                        VStack(spacing: 6) {
                            AsyncImage(url: URL(string: picture.imageUrl)) { phase in
                                switch phase {
                                case .success(let image):
                                    image
                                        .resizable()
                                        .scaledToFill()
                                default:
                                    Rectangle()
                                        .fill(BWSTheme.surfaceHighlight)
                                        .overlay {
                                            Image(systemName: "photo")
                                                .foregroundStyle(BWSTheme.textTertiary)
                                        }
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 140)
                            .clipShape(RoundedRectangle(cornerRadius: 8))

                            Text(picture.poseType.capitalized)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(BWSTheme.textSecondary)
                        }
                    }
                }
            }
        }
    }
}