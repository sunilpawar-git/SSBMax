package com.ssbmax.shared.testing

import android.content.ContentProvider
import org.robolectric.Robolectric

/**
 * Compose Multiplatform's `stringResource`/`Res.string.*` on Android reads
 * assets through a static `Context` captured by a manifest-declared
 * `ContentProvider` (`org.jetbrains.compose.resources.AndroidContextProvider`,
 * internal to `components-resources-android` -- not visible to this module's
 * source, hence the reflection below). Real Android and instrumented tests
 * get this for free (the OS instantiates every manifest provider at process
 * start); Robolectric's JVM unit-test environment does not reliably do the
 * same for this specific provider (confirmed empirically -- a Phase 6a UI
 * test calling `stringResource` failed with "Android context is not
 * initialized" even though the provider IS present in the merged test
 * manifest, `AndroidManifest.xml:68-73` under
 * `build/intermediates/packaged_manifests/debugUnitTest`).
 *
 * [Robolectric.setupContentProvider] is Robolectric's own supported hook for
 * exactly this situation (a provider that needs its `attachInfo`/`onCreate`
 * lifecycle driven manually in a test). Call once per test class, in
 * `@Before`, before any Composable that calls `stringResource` -- every
 * ported Phase 6a screen test that renders real UI text needs this, not just
 * one vertical, hence its own file rather than being duplicated per test.
 */
@Suppress("UNCHECKED_CAST")
fun ensureComposeResourcesContextInitialized() {
    val providerClass = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
        as Class<out ContentProvider>
    Robolectric.setupContentProvider(providerClass)
}
