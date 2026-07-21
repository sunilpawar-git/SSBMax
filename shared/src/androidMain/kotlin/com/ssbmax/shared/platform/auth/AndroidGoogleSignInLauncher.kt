package com.ssbmax.shared.platform.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ssbmax.shared.domain.model.GoogleSignInData
import kotlinx.coroutines.CompletableDeferred

/**
 * Android actual — legacy `GoogleSignInClient`, matching the pre-KMP
 * `FirebaseAuthService`/`AuthRepositoryImpl` flow (web client ID read from
 * `google-services.json`'s generated `default_web_client_id` string
 * resource, ID token requested, no access token).
 *
 * [launcher] must be registered by the hosting `ComponentActivity` via
 * `registerForActivityResult(ActivityResultContracts.StartActivityForResult())`
 * before the activity reaches `STARTED` — same Android platform constraint
 * documented on `AndroidNotificationPermissionController`. See
 * `MainActivity.onCreate`, which constructs this class right after
 * registering the launcher and hands it to Compose via
 * `LocalGoogleSignInLauncher`.
 *
 * Single-flight: only one [signIn] call can be in-flight at a time, matching
 * the launcher's own one-callback-per-launch contract.
 */
class AndroidGoogleSignInLauncher(
    private val context: Context,
    private val launcher: ActivityResultLauncher<Intent>
) : GoogleSignInLauncher {

    private var pendingSignIn: CompletableDeferred<GoogleSignInData>? = null

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId())
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    private fun webClientId(): String {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        check(resId != 0) {
            "default_web_client_id string resource not found -- check google-services.json"
        }
        return context.getString(resId)
    }

    override suspend fun signIn(): GoogleSignInData {
        val deferred = CompletableDeferred<GoogleSignInData>()
        pendingSignIn = deferred
        launcher.launch(googleSignInClient.signInIntent)
        return deferred.await()
    }

    /** Called from the launcher's result callback registered in MainActivity. */
    fun onSignInResult(result: ActivityResult) {
        val deferred = pendingSignIn ?: return
        pendingSignIn = null

        if (result.resultCode != Activity.RESULT_OK) {
            deferred.complete(GoogleSignInData.Cancelled)
            return
        }

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) {
                deferred.complete(GoogleSignInData.Error("Google Sign-In returned no ID token"))
                return
            }
            deferred.complete(
                GoogleSignInData.ResultData(platformData = idToken to (null as String?))
            )
        } catch (e: ApiException) {
            deferred.complete(GoogleSignInData.Error(e.message ?: "Google Sign-In failed", e))
        }
    }
}
