package com.ssbmax.shared.platform

/**
 * Whether this binary was built in debug configuration.
 *
 * Deliberately `expect/actual` rather than a Koin-injected property: iOS has
 * call sites for `ensureKoinStarted()` that run before `AppDelegate` supplies
 * any arguments (background tasks, deep links, push notifications), so a
 * property with a default would silently disagree with the real build type
 * on those paths. `expect/actual` reads the actual binary, so every call
 * site agrees regardless of how Koin was started.
 */
expect fun isDebugBuild(): Boolean
