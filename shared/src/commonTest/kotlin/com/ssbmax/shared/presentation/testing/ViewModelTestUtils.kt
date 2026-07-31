package com.ssbmax.shared.presentation.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

/**
 * Deterministically triggers [ViewModel.onCleared] from unit test code, for
 * ViewModels whose tests must force teardown mid-test (see
 * `TATTestViewModelTest`/`WATTestViewModelTest`/`SRTTestViewModelTest`/
 * `SDTTestViewModelTest` -- their viewing/writing/countdown timers use a
 * real-wall-clock `endTime`, not a virtual-time-friendly counter, so an
 * uncancelled timer job spins trying to catch up to real elapsed time once
 * `runTest()`'s own end-of-test cleanup drains the shared test scheduler to
 * idle. This is Phase 1 of the KMP-convergence plan's ViewModel-pattern
 * unification, converting these classes from a manual `CoroutineScope` +
 * public `close()` to real `androidx.lifecycle.ViewModel` + `viewModelScope`).
 *
 * `ViewModel.clear()` itself is `internal` to the `lifecycle-viewmodel`
 * module (see its own KDoc), so test code outside that module cannot call it
 * directly -- confirmed by reading the `lifecycle-viewmodel:2.9.0` sources
 * before writing this helper, not assumed. No `androidx.lifecycle.testing`
 * (or similarly-named) KMP artifact exists on this project's classpath either
 * (checked `shared/build.gradle.kts`'s `commonTest.dependencies` and
 * `gradle/libs.versions.toml` -- no such alias/dependency is declared), so
 * there is no sanctioned test-scoped shortcut to `onCleared()` beyond the
 * production API surface itself.
 *
 * [ViewModelStore.put]/[ViewModelStore.clear] ARE public (`@RestrictTo(LIBRARY_GROUP)`-annotated --
 * an Android-Lint-only advisory, not enforced by the Kotlin compiler, and this
 * repo's own Lint doesn't run on `commonTest` regardless per
 * `CLAUDE.md`/`ComponentMissingPreviewDetector`'s precedent of not reaching
 * `shared`) and are exactly the mechanism a real `ViewModelStoreOwner`
 * (an `Activity`/`Fragment`/`NavBackStackEntry`) uses in production to tear a
 * `ViewModel` down: storing it in a `ViewModelStore` and then clearing that
 * store invokes the `ViewModel`'s own internal `clear()`, which cancels
 * `viewModelScope` and invokes `onCleared()`. That's a real, non-reflective,
 * public entry point -- not a reflection hack -- so this helper uses it
 * rather than leaving the 4 wall-clock-timer tests as unresolved tech debt.
 */
fun ViewModel.clearForTest() {
    ViewModelStore().apply { put("clearForTest", this@clearForTest) }.clear()
}
