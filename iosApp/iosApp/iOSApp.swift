import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    
    init() {
        // Initialize Koin before app starts
        KoinHelper.shared.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
