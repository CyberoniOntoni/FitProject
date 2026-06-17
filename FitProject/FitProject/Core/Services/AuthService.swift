import Foundation
import FirebaseAuth
import Combine

@MainActor
final class AuthService: ObservableObject {
    @Published var currentUser: FPUser?
    @Published var isAuthenticated = false
    @Published var isLoading = false
    @Published var errorMessage: String?

    private var authStateHandle: AuthStateDidChangeListenerHandle?

    init() {
        authStateHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in
                self?.isAuthenticated = user != nil
                if let user {
                    self?.currentUser = FPUser(
                        id: user.uid,
                        email: user.email ?? "",
                        firstName: user.displayName?.components(separatedBy: " ").first ?? "",
                        lastName: user.displayName?.components(separatedBy: " ").dropFirst().joined(separator: " ") ?? "",
                        profilePictureUrl: user.photoURL?.absoluteString,
                        timezone: TimeZone.current.identifier,
                        coachHasProTools: false
                    )
                } else {
                    self?.currentUser = nil
                }
            }
        }
    }

    deinit {
        if let handle = authStateHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    func signIn(email: String, password: String) async throws {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let result = try await Auth.auth().signIn(withEmail: email, password: password)
            let profile = try await FirestoreService.shared.fetchUserProfile(userId: result.user.uid)
            currentUser = profile
            isAuthenticated = true
        } catch {
            errorMessage = mapAuthError(error)
            throw error
        }
    }

    func signOut() throws {
        try Auth.auth().signOut()
        currentUser = nil
        isAuthenticated = false
    }

    func updateUserProfile(_ user: FPUser) {
        currentUser = user
    }

    private func mapAuthError(_ error: Error) -> String {
        let nsError = error as NSError
        switch nsError.code {
        case AuthErrorCode.wrongPassword.rawValue:
            return "Incorrect password. Please try again."
        case AuthErrorCode.invalidEmail.rawValue:
            return "Invalid email address."
        case AuthErrorCode.userNotFound.rawValue:
            return "No account found with this email."
        case AuthErrorCode.networkError.rawValue:
            return "Network error. Check your connection."
        default:
            return error.localizedDescription
        }
    }
}