package com.ssbmax.shared.platform.permissions

/**
 * Checks/requests the OS notification permission (Android 13+'s
 * `POST_NOTIFICATIONS` runtime permission; iOS's `UNUserNotificationCenter`
 * authorization). Used before TAT/WAT/SRT/SDT/PPDT worker results and
 * interview session results are delivered via local notification — both
 * request sites in `app` treat the outcome as best-effort (the flow
 * proceeds regardless of grant/deny).
 *
 * Implementations:
 * - Android: `AndroidNotificationPermissionController` — wraps an
 *   `ActivityResultLauncher<String>` that MUST be registered by the hosting
 *   `ComponentActivity` before it reaches `STARTED` (an Android platform
 *   constraint on `registerForActivityResult`, not something this shim can
 *   route around) — see `MainActivity.onCreate`.
 * - iOS: `IosNotificationPermissionController` — `UNUserNotificationCenter`,
 *   no equivalent lifecycle constraint.
 */
interface NotificationPermissionController {
    /** True if the permission is already granted (or the platform/OS
     *  version doesn't require an explicit runtime grant). */
    suspend fun isGranted(): Boolean

    /** Requests the permission if not already granted. Returns the
     *  resulting grant state. Safe to call even if already granted
     *  (returns true immediately without re-prompting). */
    suspend fun request(): Boolean
}
