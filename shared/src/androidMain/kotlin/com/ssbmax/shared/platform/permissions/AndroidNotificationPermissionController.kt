package com.ssbmax.shared.platform.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

/**
 * Android actual: `POST_NOTIFICATIONS` is only a runtime permission on
 * API 33+ (Tiramisu); below that, notifications are granted by default and
 * [isGranted]/[request] both short-circuit to `true`.
 *
 * [launcher] must be registered by the hosting `ComponentActivity` via
 * `registerForActivityResult(ActivityResultContracts.RequestPermission())`
 * before the activity reaches `STARTED` — Android disallows registering
 * later. See `MainActivity.onCreate`, which constructs this class right
 * after registering the launcher and hands it to Compose via
 * `LocalNotificationPermissionController`.
 */
class AndroidNotificationPermissionController(
    private val context: Context,
    private val launcher: ActivityResultLauncher<String>
) : NotificationPermissionController {

    // Single-flight: only one request() call can be in-flight at a time,
    // matching the launcher's own one-callback-per-launch contract.
    private var pendingRequest: CompletableDeferred<Boolean>? = null

    override suspend fun isGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun request(): Boolean {
        if (isGranted()) return true
        val deferred = CompletableDeferred<Boolean>()
        pendingRequest = deferred
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return deferred.await()
    }

    /** Called from the launcher's result callback registered in MainActivity. */
    fun onPermissionResult(granted: Boolean) {
        pendingRequest?.complete(granted)
        pendingRequest = null
    }
}
