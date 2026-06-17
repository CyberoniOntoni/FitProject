import SwiftUI

struct MeasurementTypeInfo: Identifiable {
    let id: String
    let name: String
    let unit: String
    let category: String
}

enum MeasurementCatalog {
    static let types: [MeasurementTypeInfo] = [
        .init(id: "weight", name: "Weight", unit: "kg", category: "Body Composition"),
        .init(id: "bodyfat", name: "Body Fat", unit: "%", category: "Body Composition"),
        .init(id: "chest", name: "Chest", unit: "cm", category: "Circumference"),
        .init(id: "waist", name: "Waist", unit: "cm", category: "Circumference"),
        .init(id: "hips", name: "Hips", unit: "cm", category: "Circumference"),
        .init(id: "arms", name: "Arms", unit: "cm", category: "Circumference"),
        .init(id: "thighs", name: "Thighs", unit: "cm", category: "Circumference"),
    ]

    static var categories: [String] {
        Array(Set(types.map(\.category))).sorted()
    }

    static func types(in category: String) -> [MeasurementTypeInfo] {
        types.filter { $0.category == category }
    }
}

struct MeasurementsView: View {
    @EnvironmentObject var appState: AppState
    @State private var showAddSheet = false
    @State private var newName = "Weight"
    @State private var newValue = ""
    @State private var newUnit = "kg"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                overviewSection
                chartSection
                historySection
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 40)
        }
        .background(BWSTheme.background)
        .navigationTitle("Body Measurements")
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

    private var overviewSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Current Measurements")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)

            Text("Tap a measurement to log a new entry.")
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)

            ForEach(MeasurementCatalog.categories, id: \.self) { category in
                Text(category.uppercased())
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(BWSTheme.accent)
                    .tracking(1.2)
                    .padding(.top, 4)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(MeasurementCatalog.types(in: category)) { type in
                        overviewCell(for: type)
                    }
                }
            }
        }
        .padding(.top, 8)
    }

    private func overviewCell(for type: MeasurementTypeInfo) -> some View {
        let latest = latestMeasurement(named: type.name)
        let hasValue = latest != nil

        return Button {
            newName = type.name
            newUnit = type.unit
            newValue = ""
            showAddSheet = true
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                Text(type.name)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(BWSTheme.textPrimary)

                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    Text(hasValue ? formatValue(latest!.value) : "0")
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundStyle(hasValue ? BWSTheme.accent : BWSTheme.textTertiary)
                    Text(type.unit)
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(hasValue ? BWSTheme.textSecondary : BWSTheme.textTertiary)
                }

                Text(hasValue
                     ? "Last: \(latest!.date.formatted(.dateTime.month(.abbreviated).day().year()))"
                     : "Not recorded")
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(hasValue ? BWSTheme.textSecondary : BWSTheme.textTertiary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(BWSTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(BWSTheme.surfaceHighlight, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var chartSection: some View {
        let weightLogs = appState.measurements
            .filter { $0.name.lowercased().contains("weight") }
            .sorted { $0.date < $1.date }
            .suffix(10)

        return Group {
            if weightLogs.count >= 2 {
                BWSCard {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Weight Trend")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(BWSTheme.textPrimary)

                        HStack(alignment: .bottom, spacing: 6) {
                            ForEach(Array(weightLogs), id: \.id) { m in
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
            }
        }
    }

    private var historySection: some View {
        let recent = appState.measurements.sorted { $0.date > $1.date }.prefix(20)

        return VStack(alignment: .leading, spacing: 12) {
            Text("Recent History")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BWSTheme.textPrimary)

            if recent.isEmpty {
                Text("No entries logged yet.")
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)
            } else {
                ForEach(Array(recent)) { measurement in
                    BWSCard {
                        HStack(alignment: .center) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(measurement.name)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(BWSTheme.textPrimary)
                                Text(measurement.date.formatted(.dateTime.month(.wide).day().year().hour().minute()))
                                    .font(BWSTheme.captionFont)
                                    .foregroundStyle(BWSTheme.textSecondary)
                            }
                            Spacer()
                            HStack(alignment: .firstTextBaseline, spacing: 2) {
                                Text(formatValue(measurement.value))
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
        }
    }

    private var addMeasurementSheet: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Picker("Type", selection: $newName) {
                    ForEach(MeasurementCatalog.types) { type in
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

                Text("Unit: \(newUnit)")
                    .font(BWSTheme.captionFont)
                    .foregroundStyle(BWSTheme.textSecondary)

                BWSPrimaryButton(title: "Save Measurement") {
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
                newUnit = MeasurementCatalog.types.first { $0.name == name }?.unit ?? "kg"
            }
        }
        .presentationDetents([.medium])
    }

    private func latestMeasurement(named name: String) -> FPMeasurement? {
        appState.measurements
            .filter { $0.name.caseInsensitiveCompare(name) == .orderedSame }
            .max(by: { $0.date < $1.date })
    }

    private func formatValue(_ value: Double) -> String {
        value.truncatingRemainder(dividingBy: 1) == 0
            ? String(format: "%.0f", value)
            : String(format: "%.1f", value)
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