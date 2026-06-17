import SwiftUI

struct FormFillView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) private var dismiss

    let form: FPForm
    @State private var answers: [String: String] = [:]
    @State private var isSubmitting = false
    @State private var showValidationAlert = false
    @State private var missingFields: [String] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if let description = form.description, !description.isEmpty {
                    Text(description)
                        .font(BWSTheme.captionFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                }

                ForEach(form.fields) { field in
                    fieldView(field)
                }

                Button(action: submit) {
                    HStack {
                        if isSubmitting {
                            ProgressView().tint(.white)
                        }
                        Text(isSubmitting ? "Submitting..." : "Submit Form")
                            .font(.system(size: 16, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(BWSTheme.accentGradient)
                    .clipShape(RoundedRectangle(cornerRadius: BWSTheme.buttonRadius))
                }
                .disabled(isSubmitting)
            }
            .padding(20)
        }
        .background(BWSTheme.background)
        .navigationTitle(form.title)
        .navigationBarTitleDisplayMode(.inline)
        .alert("Required fields", isPresented: $showValidationAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Please complete: \(missingFields.joined(separator: ", "))")
        }
    }

    @ViewBuilder
    private func fieldView(_ field: FPFormField) -> some View {
        switch field.type {
        case "Header":
            Text(field.question.uppercased())
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(BWSTheme.accent)
                .kerning(1.2)
                .padding(.top, 8)
        case "Divider":
            Divider().background(BWSTheme.surfaceHighlight)
        case "Paragraph":
            Text(field.question)
                .font(BWSTheme.captionFont)
                .foregroundStyle(BWSTheme.textSecondary)
        case "Rating":
            ratingField(field)
        case "Linear Scale":
            scaleField(field)
        case "Multiple Choice":
            choiceField(field, multi: false)
        case "Checkboxes":
            choiceField(field, multi: true)
        case "Number":
            inputField(field, keyboard: .decimalPad)
        case "Date":
            inputField(field, keyboard: .default, placeholder: "YYYY-MM-DD")
        default:
            inputField(field, keyboard: .default, multiline: field.type == "Text")
        }
    }

    private func fieldHeader(_ field: FPFormField) -> some View {
        Text(field.question + (field.required ? " *" : ""))
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(BWSTheme.textPrimary)
    }

    private func ratingField(_ field: FPFormField) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            fieldHeader(field)
            HStack(spacing: 8) {
                ForEach(1...field.maxRating, id: \.self) { rating in
                    Button {
                        answers[field.id] = String(rating)
                    } label: {
                        Text("\(rating)")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(answers[field.id] == String(rating) ? .white : BWSTheme.textPrimary)
                            .frame(width: 40, height: 40)
                            .background(answers[field.id] == String(rating) ? BWSTheme.accent : BWSTheme.surfaceHighlight)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func scaleField(_ field: FPFormField) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            fieldHeader(field)
            let binding = Binding<Double>(
                get: { Double(answers[field.id] ?? String((field.scaleMin + field.scaleMax) / 2)) ?? Double((field.scaleMin + field.scaleMax) / 2) },
                set: { answers[field.id] = String(Int($0)) }
            )
            Slider(value: binding, in: Double(field.scaleMin)...Double(field.scaleMax), step: 1)
                .tint(BWSTheme.accent)
            Text(answers[field.id] ?? String((field.scaleMin + field.scaleMax) / 2))
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(BWSTheme.accent)
            HStack {
                Text(field.scaleMinLabel ?? String(field.scaleMin))
                Spacer()
                Text(field.scaleMaxLabel ?? String(field.scaleMax))
            }
            .font(BWSTheme.captionFont)
            .foregroundStyle(BWSTheme.textSecondary)
        }
        .onAppear {
            if answers[field.id] == nil {
                answers[field.id] = String((field.scaleMin + field.scaleMax) / 2)
            }
        }
    }

    private func choiceField(_ field: FPFormField, multi: Bool) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            fieldHeader(field)
            if multi {
                ForEach(field.options, id: \.self) { option in
                    let selected = (answers[field.id] ?? "").split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }
                    Toggle(option, isOn: Binding(
                        get: { selected.contains(option) },
                        set: { isOn in
                            var current = Set(selected)
                            if isOn { current.insert(option) } else { current.remove(option) }
                            answers[field.id] = current.sorted().joined(separator: ", ")
                        }
                    ))
                    .tint(BWSTheme.accent)
                    .foregroundStyle(BWSTheme.textPrimary)
                }
            } else {
                Picker("Select", selection: Binding(
                    get: { answers[field.id] ?? "" },
                    set: { answers[field.id] = $0 }
                )) {
                    Text("Select an option").tag("")
                    ForEach(field.options, id: \.self) { option in
                        Text(option).tag(option)
                    }
                }
                .pickerStyle(.menu)
                .tint(BWSTheme.accent)
            }
        }
    }

    private func inputField(_ field: FPFormField, keyboard: UIKeyboardType, placeholder: String = "Your answer", multiline: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            fieldHeader(field)
            if multiline {
                TextEditor(text: Binding(
                    get: { answers[field.id] ?? "" },
                    set: { answers[field.id] = $0 }
                ))
                .frame(minHeight: 80)
                .scrollContentBackground(.hidden)
                .padding(8)
                .background(BWSTheme.surfaceHighlight)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .foregroundStyle(BWSTheme.textPrimary)
            } else {
                TextField(placeholder, text: Binding(
                    get: { answers[field.id] ?? "" },
                    set: { answers[field.id] = $0 }
                ))
                .keyboardType(keyboard)
                .padding(12)
                .background(BWSTheme.surfaceHighlight)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .foregroundStyle(BWSTheme.textPrimary)
            }
        }
    }

    private func submit() {
        missingFields = form.fields
            .filter { $0.required && isInputField($0.type) }
            .filter { answers[$0.id]?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty != false }
            .map(\.question)

        guard missingFields.isEmpty else {
            showValidationAlert = true
            return
        }

        let submissionAnswers = form.fields
            .filter { isInputField($0.type) }
            .compactMap { field -> FPFormAnswer? in
                guard let value = answers[field.id], !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
                return FPFormAnswer(fieldId: field.id, question: field.question, type: field.type, value: value)
            }

        isSubmitting = true
        Task {
            do {
                try await appState.submitForm(form, answers: submissionAnswers)
                dismiss()
            } catch {
                missingFields = [error.localizedDescription]
                showValidationAlert = true
            }
            isSubmitting = false
        }
    }

    private func isInputField(_ type: String) -> Bool {
        type != "Header" && type != "Divider" && type != "Paragraph"
    }
}