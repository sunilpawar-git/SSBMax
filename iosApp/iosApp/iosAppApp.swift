import SwiftUI

/// SwiftUI entry point. Wraps the Compose Multiplatform nav host
/// (`ComposeUIViewController` from `shared/iosMain/ui/MainViewController.kt`)
/// via `ContentView`.
///
/// Phase 6: adopts `AppDelegate` via `@UIApplicationDelegateAdaptor` so
/// `BGTaskScheduler` registration and APNs setup (both of which need real
/// `UIApplicationDelegate` launch callbacks -- see `AppDelegate.swift`'s
/// class doc) run before this `Scene` renders its first frame, not lazily
/// on first Compose render like the Phase 0/5 spike shell did.
@main
struct iosAppApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
