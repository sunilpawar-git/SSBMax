package com.ssbmax.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.ssbmax.shared.platform.billing.BillingClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.koin.test.check.checkModules

/**
 * Phase 0d (KMP-convergence plan): pins that every constructor dependency in
 * `appModules` (the full Koin graph `startKoin()` loads — see
 * [KoinModules.kt]) resolves. Real instantiation, not a static check --
 * `RepositoryModule`/`FirebaseModule` declare bindings as raw `single { ... }`
 * lambdas (not `singleOf(::Ctor)`), which Koin can't introspect without
 * running them, so `Module.verify()`'s static mode doesn't apply here.
 *
 * Two things can't run for real in a JVM unit test, so they're substituted:
 * - `Context` (needed by `androidContext()` in `platformModule`,
 *   `workManagerModule`, `FirebaseModule`'s analytics single)
 *   → `withInstance<Context>()`, per this file's own Koin-test contract.
 * - The real Firebase Android SDK would throw ("Default FirebaseApp is not
 *   initialized") outside a real Android process. Robolectric is the normal
 *   escape hatch, but every existing Robolectric test in this module is
 *   `@Ignore`d for an SDK 35 shadow mismatch (see e.g.
 *   [com.ssbmax.notifications.NotificationHelperTest]) — so this follows the
 *   same `mockkStatic` pattern for `android.util.Log`-adjacent Android
 *   statics that `com.ssbmax.testing.BaseViewModelTest` used before it (and
 *   the `app/ui` ViewModel tests that were its only callers) were deleted in
 *   KMP-convergence Phase 6a. Mocked at the `FirebaseAuth`/`FirebaseFirestore`/
 *   `FirebaseStorage`/`FirebaseMessaging` static `getInstance()` level, not
 *   the `Firebase.auth`-style KTX extension property level: GitLive's Android
 *   actuals (e.g. `GitLiveAuthRepository` → `dev.gitlive.firebase.auth.android
 *   .getAuth()`) call the Java static accessor directly, confirmed by running
 *   this test and reading the real `Caused by: IllegalStateException` it
 *   produced pointing at `FirebaseAuth.getInstance()`, not the KTX path.
 *
 * `Dispatchers.setMain` is likewise required: several ViewModels launch a
 * coroutine from `init {}` (e.g. `StudentHomeViewModel.observeUserProfile`),
 * and `checkModules` really constructs them, so `Dispatchers.Main` needs a
 * test dispatcher installed exactly as `kotlinx-coroutines-test` prescribes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlatformModuleCheckTest : KoinTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `appModules resolves every dependency`() {
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.getInstance() } returns mockk(relaxed = true)

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { FirebaseAuth.getInstance(any()) } returns mockk(relaxed = true)

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        every { FirebaseFirestore.getInstance(any<FirebaseApp>()) } returns mockk(relaxed = true)

        mockkStatic(FirebaseStorage::class)
        every { FirebaseStorage.getInstance() } returns mockk(relaxed = true)
        every { FirebaseStorage.getInstance(any<FirebaseApp>()) } returns mockk(relaxed = true)

        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)

        mockkStatic(FirebaseAnalytics::class)
        every { FirebaseAnalytics.getInstance(any()) } returns mockk(relaxed = true)

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)

        val fakeContext = mockk<Application>(relaxed = true)
        every { fakeContext.applicationContext } returns fakeContext

        // A bare relaxed mock's generic `get<T>(key)` returns a proxy that
        // fails to cast to the caller's expected type (e.g. TopicViewModel's
        // `savedStateHandle.get<String>("topicId")`, confirmed by running
        // this test and reading the real ClassCastException it produced) --
        // stub the one type shape every ViewModel here actually reads.
        val fakeSavedStateHandle = mockk<SavedStateHandle>(relaxed = true)
        every { fakeSavedStateHandle.get<String>(any()) } returns "OIR"

        val app = koinApplication {
            modules(appModules)
        }
        // PlayBillingClient's constructor reaches into Play Billing Library's
        // real GMS internals (BillingClientImpl.initialize()), which need a
        // genuine Android process -- confirmed by running this test and
        // reading the real NoClassDefFoundError (`libcore.io.Memory`) it
        // produced. `declare(allowOverride = true)` replaces the definition
        // outright before verification runs, the same way `checkModules`
        // itself checks singletons: by resolving the (now-overridden) type.
        app.koin.declare<BillingClient>(mockk(relaxed = true), allowOverride = true)

        // `checkModules` is deprecated upstream in favor of Koin's newer
        // `verify()` API; left as-is deliberately -- `verify()`'s static
        // mode can't handle this module's raw `single { ... }` lambda
        // declarations any better than `Module.verify()` does (see this
        // file's class doc), so migrating isn't a drop-in win, and this
        // works correctly today.
        app.checkModules {
            withInstance<Context>(fakeContext)
            // `koinViewModel()` normally supplies this via its own
            // SavedStateHandle-aware factory; checkModules constructs
            // ViewModels directly, bypassing that, so it needs a stand-in.
            withInstance<SavedStateHandle>(fakeSavedStateHandle)
        }
    }
}
