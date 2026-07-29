import SwiftUI
import SharedKit

// Bridges SwiftUI to the Compose Multiplatform screen defined in
// shared/commonMain/ui/SpikeApp.kt via MainViewController() (shared/iosMain).
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Real GIDSignIn-backed launcher, not the Kotlin-side stub default --
        // see RealIosGoogleSignInLauncher.swift's class doc.
        MainViewControllerKt.MainViewController(googleSignInLauncher: RealIosGoogleSignInLauncher())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
