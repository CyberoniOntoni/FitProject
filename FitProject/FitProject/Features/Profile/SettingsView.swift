import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @State private var isSaving = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Unit Preferences")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(BWSTheme.textPrimary)

                Text("Synced with FitPros.io — changes apply across web and mobile.")
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)

                BWSCard {
                    VStack(alignment: .leading, spacing: 16) {
                        preferenceGroup(
                            title: "Body Weight / Mass",
                            options: [("kg", "KILOGRAM"), ("lb", "POUND")],
                            key: "mass",
                            current: appState.unitPreferences.mass
                        )
                        preferenceGroup(
                            title: "Circumference",
                            options: [("cm", "CENTIMETER"), ("in", "INCH")],
                            key: "circumference",
                            current: appState.unitPreferences.circumference
                        )
                        preferenceGroup(
                            title: "Distance",
                            options: [("km", "KILOMETER"), ("mi", "MILE")],
                            key: "distance",
                            current: appState.unitPreferences.distance
                        )
                        preferenceGroup(
                            title: "Time",
                            options: [("sec", "SECOND"), ("min", "MINUTE")],
                            key: "time",
                            current: appState.unitPreferences.time
                        )
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(BWSTheme.background)
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .disabled(isSaving)
    }

    private func preferenceGroup(
        title: String,
        options: [(label: String, value: String)],
        key: String,
        current: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)

            HStack(spacing: 8) {
                ForEach(options, id: \.value) { option in
                    Button {
                        guard option.value != current else { return }
                        isSaving = true
                        Task {
                            try? await appState.updateUnitPreference(key: key, value: option.value)
                            isSaving = false
                        }
                    } label: {
                        Text(option.label)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(option.value == current ? .white : BWSTheme.textPrimary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(option.value == current ? AnyShapeStyle(BWSTheme.accentGradient) : AnyShapeStyle(BWSTheme.surfaceHighlight))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}