import SwiftUI
import SharedKit

// Bridges SwiftUI to the Compose Multiplatform screen defined in
// shared/commonMain/ui/SpikeApp.kt via MainViewController() (shared/iosMain).
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
