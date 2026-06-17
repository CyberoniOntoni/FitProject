import SwiftUI

struct LoginView: View {
    @EnvironmentObject var appState: AppState
    @State private var email = ""
    @State private var password = ""
    @State private var showPassword = false

    var body: some View {
        ZStack {
            BWSTheme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                VStack(spacing: 8) {
                    Image(systemName: "figure.strengthtraining.traditional")
                        .font(.system(size: 48))
                        .foregroundStyle(BWSTheme.accentGradient)

                    Text("Fit Project")
                        .font(.system(size: 36, weight: .bold, design: .rounded))
                        .foregroundStyle(BWSTheme.textPrimary)

                    Text("Science-backed training.\nSynced with FitPros.")
                        .font(BWSTheme.bodyFont)
                        .foregroundStyle(BWSTheme.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.bottom, 48)

                VStack(spacing: 16) {
                    TextField("Email", text: $email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .padding()
                        .background(BWSTheme.surface)
                        .clipShape(RoundedRectangle(cornerRadius: BWSTheme.buttonRadius))
                        .foregroundStyle(BWSTheme.textPrimary)

                    HStack {
                        if showPassword {
                            TextField("Password", text: $password)
                        } else {
                            SecureField("Password", text: $password)
                        }
                        Button {
                            showPassword.toggle()
                        } label: {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundStyle(BWSTheme.textTertiary)
                        }
                    }
                    .padding()
                    .background(BWSTheme.surface)
                    .clipShape(RoundedRectangle(cornerRadius: BWSTheme.buttonRadius))
                    .foregroundStyle(BWSTheme.textPrimary)

                    if let error = appState.authService.errorMessage {
                        Text(error)
                            .font(BWSTheme.captionFont)
                            .foregroundStyle(BWSTheme.error)
                    }

                    BWSPrimaryButton(
                        title: "Sign In",
                        isLoading: appState.authService.isLoading
                    ) {
                        Task {
                            try? await appState.authService.signIn(email: email, password: password)
                            if appState.authService.isAuthenticated {
                                await appState.loadData()
                            }
                        }
                    }
                }
                .padding(.horizontal, 24)

                Spacer()

                HStack(spacing: 4) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.caption)
                    Text("Syncs seamlessly with FitPros.io")
                        .font(BWSTheme.captionFont)
                }
                .foregroundStyle(BWSTheme.textTertiary)
                .padding(.bottom, 32)
            }
        }
    }
}