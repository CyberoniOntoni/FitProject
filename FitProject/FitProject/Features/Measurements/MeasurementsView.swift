import SwiftUI

struct MeasurementsView: View {
    @EnvironmentObject var appState: AppState
    @State private var showAddSheet = false
    @State private var newName = "Weight"
    @State private var newValue = ""
    @State private var newUnit = "kg"

    private let defaultTypes = [
        FPMeasurementType(id: "weight", name: "Weight", unit: "kg", isDefault: true),
        FPMeasurementType(id: "bodyfat", name: "Body Fat", unit: "%", isDefault: true),
        FPMeasurementType(id: "chest", name: "Chest", unit: "cm", isDefault: false),
        FPMeasurementType(id: "waist", name: "Waist", unit: "cm", isDefault: false),
        FPMeasurementType(id: "hips", name: "Hips", unit: "cm", isDefault: false),
    ]

    var body: some View {
        ScrollView {
            if appState.measurements.isEmpty {
                emptyState
            } else {
                chartSection
                measurementsList
            }
        }
        .background(BWSTheme.background)
        .navigationTitle("Measurements")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(BWSTheme.accent)
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            addMeasurementSheet
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "ruler")
                .font(.system(size: 48))
                .foregroundStyle(BWSTheme.textTertiary)
            Text("No measurements logged")
                .font(BWSTheme.headlineFont)
                .foregroundStyle(BWSTheme.textPrimary)
            Text("Track weight, body composition, and custom metrics. Syncs with FitPros.io.")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            BWSPrimaryButton(title: "Log Measurement", icon: "plus") {
                showAddSheet = true
            }
            .padding(.horizontal, 40)
            .padding(.top, 8)
        }
        .padding(.top, 60)
    }

    private var chartSection: some View {
        let weightLogs = appState.measurements.filter { $0.name.lowercased().contains("weight") }.prefix(10)
        return Group {
            if !weightLogs.isEmpty {
                BWSCard {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Weight Trend")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(BWSTheme.textPrimary)

                        HStack(alignment: .bottom, spacing: 6) {
                            ForEach(Array(weightLogs.reversed()), id: \.id) { m in
                                let maxVal = weightLogs.map(\.value).max() ?? 1
                                let minVal = weightLogs.map(\.value).min() ?? 0
                                let range = max(maxVal - minVal, 1)
                                let height = CGFloat((m.value - minVal) / range) * 80 + 20

                                VStack(spacing: 4) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(BWSTheme.accentGradient)
                                        .frame(width: 20, height: height)
                                    Text(m.date.formatted(.dateTime.month(.abbreviated).day()))
                                        .font(.system(size: 9))
                                        .foregroundStyle(BWSTheme.textTertiary)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 120)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 8)
            }
        }
    }

    private var measurementsList: some View {
        LazyVStack(spacing: 12) {
            ForEach(appState.measurements) { measurement in
                BWSCard {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(measurement.name)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(BWSTheme.textPrimary)
                            Text(measurement.date.formatted(.dateTime.month(.wide).day().year()))
                                .font(BWSTheme.captionFont)
                                .foregroundStyle(BWSTheme.textSecondary)
                        }
                        Spacer()
                        HStack(alignment: .firstTextBaseline, spacing: 2) {
                            Text(String(format: "%.1f", measurement.value))
                                .font(.system(size: 24, weight: .bold, design: .rounded))
                                .foregroundStyle(BWSTheme.accent)
                            Text(measurement.unit)
                                .font(BWSTheme.captionFont)
                                .foregroundStyle(BWSTheme.textSecondary)
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 40)
    }

    private var addMeasurementSheet: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Picker("Type", selection: $newName) {
                    ForEach(defaultTypes) { type in
                        Text(type.name).tag(type.name)
                    }
                }
                .pickerStyle(.wheel)
                .frame(height: 120)

                TextField("Value", text: $newValue)
                    .keyboardType(.decimalPad)
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .multilineTextAlignment(.center)
                    .padding()
                    .background(BWSTheme.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                BWSPrimaryButton(title: "Save") {
                    saveMeasurement()
                }
                .padding(.horizontal, 20)

                Spacer()
            }
            .padding(.top, 20)
            .background(BWSTheme.background)
            .navigationTitle("Log Measurement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { showAddSheet = false }
                        .foregroundStyle(BWSTheme.textSecondary)
                }
            }
            .onChange(of: newName) { _, name in
                newUnit = defaultTypes.first { $0.name == name }?.unit ?? "kg"
            }
        }
        .presentationDetents([.medium])
    }

    private func saveMeasurement() {
        guard let value = Double(newValue),
              let userId = appState.authService.currentUser?.id else { return }

        let measurement = FPMeasurement(
            id: UUID().uuidString,
            name: newName,
            unit: newUnit,
            value: value,
            date: Date()
        )

        Task {
            try? await appState.syncEngine.pushMeasurement(measurement, userId: userId)
            appState.measurements.insert(measurement, at: 0)
            showAddSheet = false
            newValue = ""
        }
    }
}