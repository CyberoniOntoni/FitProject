import SwiftUI
import PhotosUI

struct AddProgressPhotoView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) private var dismiss

    @State private var draft = FPProgressPhotoDraft()
    @State private var activePose = "front"
    @State private var selectedItem: PhotosPickerItem?
    @State private var previewImage: UIImage?
    @State private var statusText = "Add at least one pose (front, side, or back)."
    @State private var isSaving = false

    private let poses = ["front", "side", "back"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                DatePicker("Session Date", selection: $draft.dateCreated, displayedComponents: .date)
                    .tint(BWSTheme.accent)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Pose")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)

                    HStack(spacing: 8) {
                        ForEach(poses, id: \.self) { pose in
                            Button {
                                activePose = pose
                                loadPreviewForActivePose()
                            } label: {
                                Text(pose.capitalized)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(activePose == pose ? .white : BWSTheme.textPrimary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(activePose == pose ? AnyShapeStyle(BWSTheme.accentGradient) : AnyShapeStyle(BWSTheme.surfaceHighlight))
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                BWSCard(padding: 0) {
                    ZStack {
                        if let previewImage {
                            Image(uiImage: previewImage)
                                .resizable()
                                .scaledToFill()
                                .frame(minHeight: 280)
                                .clipped()
                        } else {
                            VStack(spacing: 12) {
                                Image(systemName: "photo.on.rectangle.angled")
                                    .font(.system(size: 40))
                                    .foregroundStyle(BWSTheme.textTertiary)
                                Text("Tap below to add \(activePose) photo")
                                    .font(BWSTheme.captionFont)
                                    .foregroundStyle(BWSTheme.textSecondary)
                            }
                            .frame(maxWidth: .infinity, minHeight: 280)
                        }
                    }
                }

                PhotosPicker(selection: $selectedItem, matching: .images) {
                    HStack {
                        Image(systemName: "photo.badge.plus")
                        Text("Choose Photo")
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(BWSTheme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(BWSTheme.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(BWSTheme.surfaceHighlight, lineWidth: 1)
                    )
                }
                .disabled(isSaving)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Notes (optional)")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(BWSTheme.textPrimary)
                    TextField("How are you feeling?", text: $draft.notes, axis: .vertical)
                        .lineLimit(3...5)
                        .padding(12)
                        .background(BWSTheme.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                Text(statusText)
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .center)

                BWSPrimaryButton(title: isSaving ? "Uploading…" : "Save Session", isLoading: isSaving) {
                    saveSession()
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 40)
        }
        .background(BWSTheme.background)
        .navigationTitle("Add Progress Photo")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: selectedItem) { newItem in
            guard let newItem else { return }
            Task { await loadPhoto(from: newItem) }
        }
        .onAppear { loadPreviewForActivePose() }
    }

    private func loadPreviewForActivePose() {
        if let data = draft.poseImageData[activePose]?.data,
           let image = UIImage(data: data) {
            previewImage = image
        } else {
            previewImage = nil
        }
    }

    private func loadPhoto(from item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data) else { return }

        let jpegData = image.jpegData(compressionQuality: 0.85) ?? data
        draft.poseImageData[activePose] = (jpegData, "image/jpeg", "jpg")
        previewImage = image
        updateStatus()
    }

    private func updateStatus() {
        let count = draft.completedPoseCount
        statusText = count == 0
            ? "Add at least one pose (front, side, or back)."
            : "\(count) pose\(count == 1 ? "" : "s") ready — switch poses to add more."
    }

    private func saveSession() {
        guard draft.completedPoseCount > 0 else {
            statusText = "Please add at least one photo before saving."
            return
        }

        isSaving = true
        statusText = "Uploading photos…"
        Task {
            do {
                try await appState.saveProgressPhotoSession(draft)
                dismiss()
            } catch {
                statusText = "Upload failed: \(error.localizedDescription)"
                isSaving = false
            }
        }
    }
}