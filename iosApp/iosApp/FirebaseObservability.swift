import Foundation
import SharedKit
import FirebaseCrashlytics
import FirebaseAnalytics

/// Swift-side implementations of `shared`'s `CrashReporter`/`AnalyticsTracker`
/// interfaces, handed to `AppBootstrapKt.ensureKoinStarted(...)` at launch.
///
/// These used to be Kotlin (`IosCrashReporter.kt`/`IosAnalyticsTracker.kt`),
/// reaching `FIRCrashlytics`/`FIRAnalytics` through Kotlin/Native cinterop.
/// That required `shared/build.gradle.kts` to declare
/// `pod("FirebaseCrashlytics")`/`pod("FirebaseAnalytics")` -- a second copy of
/// the Firebase native dependency graph next to the one this Xcode project
/// already resolves via SPM, and the only two pods that could not be declared
/// `linkOnly`, since raw cinterop needs the Kotlin bindings `linkOnly` drops.
///
/// Swift already owns Firebase here, so implementing the two Kotlin
/// interfaces on this side removes the duplicate graph entirely. Same
/// division of labour as `RealIosGoogleSignInLauncher.swift`, where Swift
/// likewise owns the SPM dependency (`GoogleSignIn-iOS`) behind a Kotlin
/// interface.
final class FirebaseCrashReporter: CrashReporter {

    /// Kotlin `Throwable` has no lossless `NSError` bridge, so this wraps it
    /// in a minimal `NSError` carrying the message. Crashlytics receives a
    /// real non-fatal record, but not a native Kotlin stack trace -- same
    /// trade-off the previous Kotlin implementation made, and acceptable for
    /// this seam's "log a caught error" role. A faithful trace would need
    /// Crashlytics' `ExceptionModel` custom-exception API.
    ///
    /// Note `Crashlytics` only captures native crashes once `FirebaseApp.configure()`
    /// has run -- `AppDelegate` does that before calling `ensureKoinStarted`.
    func recordException(throwable: KotlinThrowable) {
        let error = NSError(
            domain: "KotlinException.\(String(describing: type(of: throwable)))",
            code: 0,
            userInfo: [NSLocalizedDescriptionKey: throwable.message ?? throwable.description()]
        )
        Crashlytics.crashlytics().record(error: error)
    }

    func setUserId(userId: String) {
        Crashlytics.crashlytics().setUserID(userId)
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }
}

/// Analytics counterpart of `FirebaseCrashReporter`.
final class FirebaseAnalyticsTracker: AnalyticsTracker {

    /// Kotlin's `Map<String, Any?>` bridges to `[String: Any]` with `NSNull`
    /// standing in for nulls. Those are stripped rather than forwarded:
    /// `Analytics.logEvent` rejects unsupported parameter value types, and
    /// the Kotlin side's `params` is nullable-valued by declaration (see
    /// `AnalyticsTracker.kt`), so null values are expected input here, not a
    /// caller bug worth failing on.
    func trackEvent(name: String, params: [String: Any]) {
        let sanitized = params.filter { !($0.value is NSNull) }
        Analytics.logEvent(name, parameters: sanitized.isEmpty ? nil : sanitized)
    }
}
