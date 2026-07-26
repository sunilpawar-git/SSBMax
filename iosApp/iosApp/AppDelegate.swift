import UIKit
import UserNotifications
import SharedKit

/// Real app-launch lifecycle hook (Phase 6 of the KMP migration plan).
///
/// Wires two things that need to happen before/at launch, neither of which
/// can live in `MainViewController.kt`'s `ComposeUIViewController { ... }`
/// body (that only runs once Compose renders its first frame, which is too
/// late for both of these):
///
/// 1. `BGTaskScheduler` registration (plan item 3) -- Apple requires
///    `register(forTaskWithIdentifier:using:launchHandler:)` to be called
///    before `application(_:didFinishLaunchingWithOptions:)` returns.
/// 2. APNs registration + `UNUserNotificationCenterDelegate` (plan item 4).
///
/// The task identifiers registered here (via `registerAndScheduleBackgroundTasksKt`,
/// the Swift-visible name Kotlin/Native generates for `registerAndScheduleBackgroundTasks()`
/// in `BackgroundTaskRegistrar.kt`) MUST match `Info.plist`'s
/// `BGTaskSchedulerPermittedIdentifiers` array below -- both are wired
/// together in this same change.
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Starts Koin + registers both BGTaskScheduler identifiers + submits
        // their first requests. See BackgroundTaskRegistrar.kt's class doc
        // for what the launch handlers do (and, just as importantly, don't
        // do yet).
        AppBootstrapKt.ensureKoinStarted()
        BackgroundTaskRegistrarKt.registerAndScheduleBackgroundTasks()

        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            guard granted else { return }
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }

        return true
    }

    /// APNs device token arrived -- hand it to `shared` so it can be saved
    /// against the signed-in user, same collection Android's FCM token was
    /// always meant to be saved to (see `IosPushNotificationBridge.kt`'s
    /// class doc for why that Android path is itself only a stub today).
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenHex = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        IosPushNotificationBridgeKt.onApnsDeviceTokenReceived(tokenHex: tokenHex)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        // No ErrorLogger equivalent wired into iosMain yet (Android-only
        // today) -- NSLog is this codebase's iOS-side floor until a KMP
        // logging shim exists. Not silently swallowed.
        NSLog("SSBMax: APNs registration failed: \(error.localizedDescription)")
    }

    // MARK: - UNUserNotificationCenterDelegate

    /// Show the notification banner even while the app is in the
    /// foreground (default UIKit behavior suppresses it otherwise).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .badge, .sound])
    }

    /// User tapped a notification (background or terminated-state launch).
    /// Deep-link/navigation handling is left for whichever phase wires real
    /// push payload routing -- out of Phase 6's registration-plumbing scope.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        completionHandler()
    }
}
