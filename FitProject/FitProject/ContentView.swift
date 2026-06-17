import SwiftUI

struct ContentView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        ZStack(alignment: .bottom) {
            TabContent()
                .padding(.bottom, BWSTheme.tabBarHeight)

            CustomTabBar(selectedTab: $appState.selectedTab)
        }
        .ignoresSafeArea(.keyboard)
    }
}

struct TabContent: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        switch appState.selectedTab {
        case .train:
            NavigationStack { HomeView() }
        case .programs:
            ProgramsView()
        case .learn:
            LearnView()
        case .history:
            HistoryView()
        }
    }
}

struct CustomTabBar: View {
    @Binding var selectedTab: AppTab
    @EnvironmentObject var appState: AppState
    @State private var showProfile = false

    var body: some View {
        VStack(spacing: 0) {
            Divider().background(Color.white.opacity(0.06))

            HStack {
                ForEach(AppTab.allCases) { tab in
                    Button {
                        withAnimation(.spring(response: 0.3)) {
                            selectedTab = tab
                        }
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: tab.icon)
                                .font(.system(size: 20))
                                .symbolVariant(selectedTab == tab ? .fill : .none)
                            Text(tab.rawValue)
                                .font(.system(size: 10, weight: .medium))
                        }
                        .foregroundStyle(selectedTab == tab ? BWSTheme.accent : BWSTheme.textTertiary)
                        .frame(maxWidth: .infinity)
                    }
                }

                Button { showProfile = true } label: {
                    VStack(spacing: 4) {
                        Image(systemName: "person.circle")
                            .font(.system(size: 20))
                        Text("Profile")
                            .font(.system(size: 10, weight: .medium))
                    }
                    .foregroundStyle(BWSTheme.textTertiary)
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.top, 10)
            .padding(.bottom, 24)
            .background(BWSTheme.surfaceElevated)
        }
        .sheet(isPresented: $showProfile) {
            ProfileView()
        }
    }
}