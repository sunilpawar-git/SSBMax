import SwiftUI

// Minimal SwiftUI entry point for the Phase 0 KMP spike. Wraps the
// Compose Multiplatform screen (ComposeViewController from shared/) so the
// same UI defined once in shared/commonMain/ui/SpikeApp.kt renders here.
@main
struct iosAppApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
