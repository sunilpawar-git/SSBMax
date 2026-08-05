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
            // Compose's shared SSBMaxAppScaffold (shared/commonMain) reserves
            // only the navigation-bar inset and expects each screen's own
            // TopAppBar to consume the real status-bar inset itself, painting
            // its background behind it -- matching Android's edge-to-edge
            // behavior. SwiftUI lays out UIViewControllerRepresentable content
            // *inside* the safe area by default, so without ignoresSafeArea()
            // here, Compose's WindowInsets.statusBars never sees a non-zero
            // value (the hosting view's frame already excludes it) and the
            // status-bar strip falls back to the window's default black
            // background instead of the TopAppBar's color.
            ContentView()
                .ignoresSafeArea()
        }
    }
}
