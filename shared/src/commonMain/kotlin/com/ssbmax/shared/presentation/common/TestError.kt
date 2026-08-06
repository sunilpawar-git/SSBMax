package com.ssbmax.shared.presentation.common

/**
 * Typed replacement for the raw `error: String?` field that 6 test ViewModels
 * (TAT/WAT/SRT/SDT/PPDT/PIQ) previously set to interpolated exception text
 * (e.g. `"Failed to submit: ${error.message}"`), which put messages like
 * `PERMISSION_DENIED: Missing or insufficient permissions.` directly on
 * screen. [com.ssbmax.shared.ui.common.TestErrorState] maps each case to a
 * `Res.string.*` resource -- the real exception still reaches `logger.e`,
 * only the user-facing text changes.
 *
 * [CLOUD_REQUIRED] and [LOAD_FAILED] are both load-time failures, kept
 * distinct because they mean different things: [CLOUD_REQUIRED] is a genuine
 * fetch failure (session-creation/content-fetch `onFailure`), [LOAD_FAILED]
 * is "the fetch succeeded but returned no content" (e.g. an empty question
 * list) -- a content/data problem, not necessarily a network one.
 */
enum class TestError {
    AUTH_REQUIRED,
    NETWORK_UNAVAILABLE,
    CLOUD_REQUIRED,
    LOGIN_TO_SUBMIT,
    SUBMIT_FAILED,
    LOAD_FAILED
}
