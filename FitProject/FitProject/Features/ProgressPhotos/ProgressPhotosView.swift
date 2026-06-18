import SwiftUI

struct ProgressPhotosView: View {
    @EnvironmentObject var appState: AppState
    @State private var activeTab = "sessions"
    @State private var compareBeforeId: String?
    @State private var compareAfterId: String?
    @State private var sliderValue: Double = 0.5
    @State private var showAddPhoto = false

    private var sortedPictures: [FPProgressPicture] {
        appState.allProgressPictures.sorted { $0.dateCreated > $1.dateCreated }
    }

    var body: some View {
        VStack(spacing: 0) {
            tabBar
            ScrollView {
                if activeTab == "sessions" {
                    sessionsContent
                } else {
                    compareContent
                }
            }
        }
        .background(BWSTheme.background)
        .navigationTitle("Progress Photos")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showAddPhoto = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(BWSTheme.accent)
                }
            }
        }
        .sheet(isPresented: $showAddPhoto) {
            NavigationStack {
                AddProgressPhotoView()
                    .environmentObject(appState)
            }
        }
    }

    private var tabBar: some View {
        HStack(spacing: 8) {
            tabButton("Sessions", tag: "sessions")
            tabButton("Compare", tag: "compare")
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }

    private func tabButton(_ title: String, tag: String) -> some View {
        Button {
            activeTab = tag
        } label: {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(activeTab == tag ? .white : BWSTheme.textPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(activeTab == tag ? AnyShapeStyle(BWSTheme.accentGradient) : AnyShapeStyle(BWSTheme.surfaceHighlight))
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var sessionsContent: some View {
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

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera")
                .font(.system(size: 48))
                .foregroundStyle(BWSTheme.textTertiary)
            Text("No progress photos yet")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text("Tap + to add front, side, and back photos. They sync with FitPros.io automatically.")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .padding(.top, 60)
        .padding(.bottom, 40)
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
                                    image.resizable().scaledToFill()
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

    @ViewBuilder
    private var compareContent: some View {
        let pictures = sortedPictures
        VStack(spacing: 16) {
            if pictures.count < 2 {
                Text("Add at least two photos to compare your progress.")
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                    .padding(.top, 40)
            } else {
                let before = pictures.first { $0.id == compareBeforeId }
                let after = pictures.first { $0.id == compareAfterId }

                Text(compareInstruction(before: before, after: after))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(BWSTheme.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)

                if let before, let after {
                    compareSlider(before: before, after: after)
                    HStack {
                        Text("Before · \(before.dateCreated.formatted(.dateTime.month(.abbreviated).day().year()))")
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.textSecondary)
                        Spacer()
                        Text("After · \(after.dateCreated.formatted(.dateTime.month(.abbreviated).day().year()))")
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.accent)
                    }
                    .padding(.horizontal, 20)

                    Button("Reset selection") {
                        compareBeforeId = nil
                        compareAfterId = nil
                        sliderValue = 0.5
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(BWSTheme.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.horizontal, 20)
                }

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(pictures) { picture in
                        compareThumbnail(picture)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
        .padding(.bottom, 40)
    }

    private func compareInstruction(before: FPProgressPicture?, after: FPProgressPicture?) -> String {
        if before == nil { return "Select your \"before\" photo" }
        if after == nil { return "Now select your \"after\" photo" }
        return "Drag the slider to compare"
    }

    private func compareSlider(before: FPProgressPicture, after: FPProgressPicture) -> some View {
        VStack(spacing: 8) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    AsyncImage(url: URL(string: before.imageUrl)) { phase in
                        if case .success(let image) = phase {
                            image.resizable().scaledToFill()
                        } else {
                            Rectangle().fill(BWSTheme.surfaceHighlight)
                        }
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipped()

                    AsyncImage(url: URL(string: after.imageUrl)) { phase in
                        if case .success(let image) = phase {
                            image.resizable().scaledToFill()
                        } else {
                            Rectangle().fill(BWSTheme.surfaceHighlight)
                        }
                    }
                    .frame(width: geo.size.width * sliderValue, height: geo.size.height)
                    .clipped()
                    .overlay(alignment: .trailing) {
                        Rectangle()
                            .fill(Color.white)
                            .frame(width: 2)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .frame(height: 360)
            .padding(.horizontal, 20)

            Slider(value: $sliderValue, in: 0...1)
                .tint(BWSTheme.accent)
                .padding(.horizontal, 20)
        }
    }

    private func compareThumbnail(_ picture: FPProgressPicture) -> some View {
        let isBefore = picture.id == compareBeforeId
        let isAfter = picture.id == compareAfterId

        return Button {
            onComparePhotoSelected(picture)
        } label: {
            VStack(spacing: 4) {
                AsyncImage(url: URL(string: picture.imageUrl)) { phase in
                    if case .success(let image) = phase {
                        image.resizable().scaledToFill()
                    } else {
                        Rectangle().fill(BWSTheme.surfaceHighlight)
                    }
                }
                .frame(height: 100)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(
                            isBefore ? BWSTheme.textSecondary : isAfter ? BWSTheme.accent : BWSTheme.surfaceHighlight,
                            lineWidth: isBefore || isAfter ? 2 : 1
                        )
                )

                Text(picture.dateCreated.formatted(.dateTime.month(.abbreviated).day().year()))
                    .font(.system(size: 11))
                    .foregroundStyle(BWSTheme.textSecondary)

                if isBefore || isAfter {
                    Text(isBefore ? "Before" : "After")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(isBefore ? BWSTheme.textSecondary : BWSTheme.accent)
                }
            }
        }
        .buttonStyle(.plain)
    }

    private func onComparePhotoSelected(_ picture: FPProgressPicture) {
        if compareBeforeId == nil {
            compareBeforeId = picture.id
        } else if compareAfterId == nil, picture.id != compareBeforeId {
            compareAfterId = picture.id
        } else {
            compareBeforeId = picture.id
            compareAfterId = nil
        }
    }
}