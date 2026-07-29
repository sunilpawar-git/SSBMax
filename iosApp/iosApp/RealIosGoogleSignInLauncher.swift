import SharedKit
import GoogleSignIn
import UIKit

/// Real `GIDSignIn`-backed implementation of the Kotlin `GoogleSignInLauncher`
/// interface (`shared/commonMain/.../platform/auth/GoogleSignInLauncher.kt`).
///
/// Lives in Swift, not Kotlin, because K/N's `firebase-auth` (GitLive) can
/// only *consume* a Google ID token via `GoogleAuthProvider.credential(...)`
/// -- it doesn't provide the native sign-in picker. `GIDSignIn` (from the
/// `GoogleSignIn-iOS` SPM package) is what actually produces that token.
/// `ContentView.swift` constructs this and passes it into
/// `MainViewController(googleSignInLauncher:)`.
///
/// Kotlin's `suspend fun signIn()` is exported as an Objective-C protocol
/// method taking a completion handler; per the generated header's note, a
/// non-nil `error` passed there surfaces as a *thrown* Kotlin exception, so
/// every outcome here (including cancel/failure) is instead represented as a
/// `GoogleSignInData` value with `error` always nil, matching the "fail
/// loud only for truly unexpected states" contract `GoogleSignInData.Error`
/// exists for.
final class RealIosGoogleSignInLauncher: GoogleSignInLauncher {
    func signIn(completionHandler: @escaping (GoogleSignInData?, Error?) -> Void) {
        guard let presenter = Self.topViewController() else {
            completionHandler(
                GoogleSignInData.Error(message: "No presenting view controller available", exception: nil),
                nil
            )
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                if error.code == GIDSignInError.canceled.rawValue {
                    completionHandler(GoogleSignInData.Cancelled.shared, nil)
                } else {
                    completionHandler(GoogleSignInData.Error(message: error.localizedDescription, exception: nil), nil)
                }
                return
            }

            guard let idToken = result?.user.idToken?.tokenString else {
                completionHandler(
                    GoogleSignInData.Error(message: "Google Sign-In returned no ID token", exception: nil),
                    nil
                )
                return
            }

            // Matches AndroidGoogleSignInLauncher / GitLiveAuthRepository's convention:
            // ResultData.platformData wraps a Kotlin Pair<String, String?> of (idToken, accessToken).
            let accessToken = result?.user.accessToken.tokenString
            let tokenPair = KotlinPair(first: idToken, second: accessToken)
            completionHandler(GoogleSignInData.ResultData(platformData: tokenPair), nil)
        }
    }

    /// Walks the key window's presented-view-controller chain so the picker
    /// presents on top of whatever screen is currently showing, including
    /// any already-presented sheet/alert.
    private static func topViewController() -> UIViewController? {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }

        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
