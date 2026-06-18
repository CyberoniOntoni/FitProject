import SwiftUI

struct RootView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        Group {
            if appState.authService.isAuthenticated {
                ContentView()
            } else {
                LoginView()
            }
        }
        .animation(.easeInOut(duration: 0.3), value: appState.authService.isAuthenticated)
    }
}