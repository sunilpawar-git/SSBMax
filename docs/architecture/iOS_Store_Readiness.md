# iOS Store Readiness

**Last updated:** July 2026 (KMP migration Phase 6 — "iOS shell & store readiness")
**Status:** Living document — update as remaining human-owned steps close.

Scope: is the `iosApp` Xcode project itself in a buildable, submittable *shape*?
This document does **not** cover App Store Connect listing content (screenshots,
marketing copy, privacy nutrition labels) — those are business/marketing tasks,
deliberately not fabricated here, and are tracked below as explicit human-owned
checklist items, the same way the KMP migration plan already tracks billing
product/price IDs as a business decision rather than something code can close.

---

## 1. What Phase 6 actually verified (code-level, re-run and confirmed in this session)

| Item | Status | Evidence |
|---|---|---|
| Bundle identifier | ✅ Fixed | `com.ssbmax.spike.iosApp` (Phase-0-spike leftover) → `com.ssbmax`, mirroring Android's real `applicationId` (`app/build.gradle.kts`). Set in both `Info.plist` and `PRODUCT_BUNDLE_IDENTIFIER` (Debug + Release) in `iosApp.xcodeproj/project.pbxproj`. |
| Version/build number scheme | ✅ Aligned | `CFBundleShortVersionString` = `1.0.0` (matches Android `versionName`), `CFBundleVersion` = `1` (matches Android `versionCode`). Bump both together going forward, same discipline as Android's own release process. |
| App icon asset catalog | ⚠️ Not present | No `Assets.xcassets`/`AppIcon` catalog exists in `iosApp/iosApp` yet — `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon` is already set as a build setting (inherited from the Xcode template), but there's no actual catalog backing it. A real app icon (even a placeholder square) needs to be added before this can archive for TestFlight/App Store — Xcode will accept a build without one for simulator debug runs, but App Store Connect validation rejects binaries with a missing/incomplete icon set. **Not fabricated here** — no default Compose/Kotlin icon asset exists to safely stand in for a real app icon. |
| Launch screen | ✅ Present | `UILaunchScreen` empty-dict entry in `Info.plist` (blank launch screen, Apple's supported "no storyboard" style) — sufficient to pass App Store validation, though a real launch screen matching the app's branding is a nice-to-have, not a blocker. |
| Duplicate/dead `Info.plist` | ✅ Cleaned up | A confusing second, unused `iosApp/Info.plist` (byte-identical to the real `iosApp/iosApp/Info.plist`, not referenced by any `INFOPLIST_FILE` build setting) was deleted as incidental tech-debt found while touching this file. |
| iOS Firebase config | ⚠️ Placeholder, human step required | See §2. |
| `BGTaskScheduler` registration | ✅ Wired | See §3. |
| Push notifications (APNs) | ⚠️ Code wired, real cert/account required | See §4. |
| The app actually builds for a real simulator target | ✅ Verified | `xcodebuild build -project iosApp.xcodeproj -scheme iosApp -destination "platform=iOS Simulator,name=iPhone 17"` → **BUILD SUCCEEDED** (this session, after adding the `FirebaseStorage` SPM package dependency and `libsqlite3.tbd` system library — both were previously-missing link inputs; see git history for the exact commit). This is the first time in this plan's history that the actual Xcode app target (not just Gradle's Kotlin/Native compile step) was verified to link successfully end-to-end against the real GitLive-Firebase + SQLDelight dependency stack. |

---

## 2. iOS `GoogleService-Info.plist` (plan item 1)

- **Real file:** never committed (`.gitignore`: `iosApp/iosApp/GoogleService-Info.plist`), same treatment as Android's `app/google-services.json`.
- **Template:** `iosApp/iosApp/GoogleService-Info.plist.example` — every value is an obvious placeholder (`REPLACE_WITH_REAL_...`), not a real (even if unused) credential.
- **Safeguard:** a new Xcode Run Script build phase ("Ensure GoogleService-Info.plist", runs before the Resources copy phase) auto-copies the placeholder template in if the real file is missing, and prints a build warning (not a hard failure, matching the Android side's own lack of a hard gate) if the resolved file still contains `REPLACE_WITH_REAL`. Verified this session: deleting the real file and rebuilding reproduces both warnings and still yields **BUILD SUCCEEDED**.
- **What a human must do** (cannot be done from this environment — no real Firebase iOS app is registered):
  1. Open the existing Firebase project (`ssbmax-49e68`, per the real `app/google-services.json`'s `project_id` — not reproduced verbatim here for the same reason it's gitignored) in the [Firebase console](https://console.firebase.google.com).
  2. Project Settings → Add app → iOS. Bundle ID: `com.ssbmax` (now matches `PRODUCT_BUNDLE_IDENTIFIER`).
  3. Download the real `GoogleService-Info.plist` and place it at `iosApp/iosApp/GoogleService-Info.plist` (not committed).
  4. Rebuild — the warning disappears once the sentinel text is gone.

---

## 3. `BGTaskScheduler` launch-time registration (plan item 3)

- **Kotlin side** (`shared/src/iosMain/kotlin/com/ssbmax/shared/platform/worker/BackgroundTaskRegistrar.kt`, new): real `BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(...)` calls for both periodic jobs already scoped in Phase 4 (`BGTaskSchedulerBackgroundTaskScheduler.CLEANUP_TASK_ID` = `com.ssbmax.questioncachecleanup`, `ARCHIVAL_TASK_ID` = `com.ssbmax.submissionarchival`), plus a Swift-callable `registerAndScheduleBackgroundTasks()` entry point that starts Koin, registers, and submits the first request for each.
- **Swift side** (`iosApp/iosApp/AppDelegate.swift`, new): calls the above from `application(_:didFinishLaunchingWithOptions:)` — before Apple's own documented deadline for `BGTaskScheduler.register`.
- **Info.plist**: `BGTaskSchedulerPermittedIdentifiers` array added with both identifiers, plus `UIBackgroundModes` (`fetch`, `processing`, `remote-notification`).
- **Deliberately NOT done** (matches the plan's explicit instruction not to silently resolve this): the launch handlers registered are a scheduled-completion no-op — they reschedule the next occurrence and call `task.setTaskCompletedWithSuccess(true)`, but do **not** run the actual cache-cleanup/archival business logic. That logic lives in Android-only `WorkManager` `CoroutineWorker` classes in `app` (`QuestionCacheCleanupWorker`, `ArchivalWorker`), which `shared` cannot depend on, and porting it to iOS was deliberately deferred pending a product decision on iOS's background-execution UX gap (`BGTaskScheduler` gives no execution guarantee — see the plan's Risk #2). This document re-confirms that decision is **still open, unchanged** at Phase 6 — not silently resolved by this registration work.

---

## 4. Push notifications via APNs (plan item 4)

- **Swift side** (`AppDelegate.swift`): real `UNUserNotificationCenter.requestAuthorization` + `UIApplication.registerForRemoteNotifications()`, plus a full `UNUserNotificationCenterDelegate` (foreground presentation, tap handling).
- **Token capture → storage**: `didRegisterForRemoteNotificationsWithDeviceToken` hex-encodes the raw APNs token and calls `IosPushNotificationBridge.kt`'s `onApnsDeviceTokenReceived(tokenHex:)`, which saves it via the existing `NotificationRepository.saveFCMToken(FCMToken(...))` — the same repository call Android's FCM path was always meant to use.
- **Important, not glossed over**: Android's own `SSBMaxFirebaseMessagingService.onNewToken` (`app/src/main/kotlin/com/ssbmax/notifications/`) is itself only a `TODO`-commented stub — it logs the token but never calls `NotificationRepository.saveFCMToken`. This iOS path is therefore the **first real implementation** of "persist the push token" in this codebase, not a port of a working Android original. Flagging this explicitly rather than implying parity that doesn't exist.
- **Also important**: the token saved is the raw APNs device token, not an FCM registration token — GitLive's Firebase Kotlin SDK has no Messaging module (confirmed absent; see the KMP migration plan's Risk #1 on thin GitLive coverage for Messaging/Analytics/Crashlytics). A server sending pushes to `platform == "ios"` rows in this collection must call Apple's APNs HTTP/2 API directly, not FCM's send API — this is a real backend-architecture gap for whoever wires server-side push, not something this client-side plumbing can paper over.
- **Entitlement**: `iosApp/iosApp/iosApp.entitlements` (new) declares `aps-environment = development`, wired via `CODE_SIGN_ENTITLEMENTS` in both Debug/Release build configs.
- **What a human must do** (genuinely cannot be done in this environment):
  1. Enroll in the **Apple Developer Program** (paid, $99/year) if not already enrolled.
  2. In the Apple Developer portal, enable the Push Notifications capability for the `com.ssbmax` App ID and generate a real APNs authentication key (or per-app push certificate).
  3. Upload that key/certificate to whichever backend will send pushes (Firebase Cloud Messaging can proxy APNs delivery if the project also uses FCM for Android — check whether that's desired before building a bespoke APNs sender).
  4. Switch `aps-environment` to `production` in `iosApp.entitlements` for the App Store release build (the current `development` value is correct for simulator/debug builds only).

---

## 5. `:shared:linkDebugTestIosSimulatorArm64` — investigated, root cause found, not fully closed

**Verification command:** `./gradlew :shared:linkDebugTestIosSimulatorArm64` — **still fails** (`ld: framework 'FirebaseCore' not found`), same error signature tracked since Phase 2. This is reported honestly, not silently carried forward again — real investigation happened this session (see below), it just didn't reach a passing state.

**Root cause, confirmed by direct inspection (not guessed):**

- GitLive's iOS cinterop `.klib`s (`firebase-app-cinterop-FirebaseCore`, `firebase-auth-cinterop-FirebaseAuth`, `firebase-firestore-cinterop-FirebaseFirestore`, `firebase-storage-cinterop-FirebaseStorage`, all under `~/.gradle/caches/modules-2/files-2.1/dev.gitlive/`) each declare `linkerOpts=-framework <Name>` in their manifest — an **explicit, hard-required** linker flag, not a soft auto-link hint.
- This project's SPM integration (confirmed via `xcodebuild -resolvePackageDependencies`, a full `xcodebuild build`, and exhaustive search of the resulting DerivedData tree — `SourcePackages/artifacts`, `Build/Products`, `Build/Intermediates.noindex`, including `PackageFrameworks/`, which is empty) compiles `FirebaseCore`/`FirebaseAuth`/`FirebaseFirestore` from source and **statically merges them directly into whichever Xcode target consumes them** (object files + Clang module maps) — it does **not** produce standalone, redistributable `.framework`/`.xcframework` bundles for these three. Only `FirebaseFirestoreInternal` and `FirebaseAnalytics` ship as Firebase-provided prebuilt binary xcframeworks (confirmed present on disk); the others are pure source.
- This is why the **real `iosApp` Xcode app target still builds successfully** despite the exact same "framework not found" condition: Xcode's own linker treats a missing *auto-linked* framework (discovered by scanning `LC_LINKER_OPTION` records that Swift/Clang's `@import` embeds) as a **warning**, not a fatal error, as long as the actual referenced symbols are satisfied by something else in the same link (which they are, once `FirebaseStorage` was added as an explicit package dependency and `libsqlite3.tbd` was linked — both done this session). Kotlin/Native's linker, by contrast, passes `-framework FirebaseCore` as an **explicit command-line flag** from the klib manifest, which `ld` always treats as fatal if the named framework can't be found on any search path — regardless of whether the symbols are otherwise available.
- Net effect: the isolated `:shared` Gradle-only iOS test binary has no "host app" alongside it statically compiling the same Firebase source to satisfy those classes, so it hits the hard-fail path that the real app avoids.

**Why this wasn't fixed today, stated plainly:** the only two ways to give Kotlin/Native's linker a real, standalone `FirebaseCore.framework` (etc.) to find are (a) Firebase's official prebuilt Carthage-style zip (`https://github.com/firebase/firebase-ios-sdk/releases/download/11.15.0/Firebase.zip`, confirmed to exist, but **719 MB** — impractical to fetch and inappropriate to vendor/commit for this fix), or (b) CocoaPods, which the project has explicitly opted out of (SPM-only, by direct instruction this phase). Both were ruled out rather than silently attempted. A third theoretical option — adding an auxiliary Xcode "Framework"-type target so SPM materializes these as embedded dynamic frameworks — was considered but not attempted, since hand-authoring a new native target directly in `project.pbxproj` text (without Xcode's own project-model validation) carries real risk of corrupting the project file for an unverified payoff, and this environment has no way to drive Xcode's GUI to add it safely.

**Practical impact:** this only blocks running `shared`'s own JVM/Kotlin-Native test suite as a standalone linked iOS binary via Gradle. It does **not** block the real app from building, running, or linking against Firebase — that path was verified working this session. `:shared:testDebugUnitTest` (JVM/Android target) and `:shared:compileKotlinIosSimulatorArm64`/`:shared:compileTestKotlinIosSimulatorArm64` (iOS compile, not link) both remain green, per every prior phase's verification.

**Recommended next step, if this needs closing in a future phase:** attempt the auxiliary-Framework-target approach interactively in Xcode's GUI (not blind pbxproj text editing), which can be visually verified before committing, and can be independently confirmed to produce real `.framework` bundles under `PackageFrameworks/` — genuinely uncertain without hands-on Xcode access, so not asserted as a guaranteed fix here.

---

## 6. Remaining human-owned checklist before App Store submission

None of the following can be fabricated or completed from a coding session — listed explicitly, same discipline as the KMP migration plan's own "billing product/price IDs are placeholder" tracking:

- [ ] Apple Developer Program enrollment (paid account).
- [ ] Real push certificate/key (§4).
- [ ] Real `GoogleService-Info.plist` (§2).
- [ ] Real app icon asset catalog (§1) — at minimum a placeholder 1024×1024 icon to pass App Store Connect validation.
- [ ] App Store Connect app record created, bundle ID `com.ssbmax` registered.
- [ ] Screenshots (all required device size classes).
- [ ] App description, keywords, support URL, marketing copy.
- [ ] Privacy nutrition label (App Privacy details in App Store Connect) — this app collects auth/profile/assessment data, so this needs real product/legal input, not a guess.
- [ ] TestFlight internal/external testing pass before public release.
- [ ] Switch `aps-environment` from `development` to `production` for the release build.
- [ ] Real Play Console / App Store Connect subscription product IDs (tracked in the KMP migration plan's own open-items table — not duplicated here, just cross-referenced).
- [ ] `aps-environment` = `production` + a real distribution provisioning profile once `CODE_SIGNING_ALLOWED`/`CODE_SIGNING_REQUIRED` are turned back on for a release/archive build (currently `NO` in both Debug and Release — fine for simulator-only work to date, but must change before a real device/App Store archive build).
