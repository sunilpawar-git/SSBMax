package com.ssbmax.shared.ui.common

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory

/**
 * First real use of the Coil3 dependencies added in commit `0dfe082`
 * ("navigation + image-loading deps for shared/commonMain") — nothing
 * consumed them until this session's OIR question-figure images
 * ([com.ssbmax.shared.ui.oir.components.OIRQuestionFigureCard]).
 *
 * Coil3's `coil-network-ktor3` Android artifact self-registers its
 * `NetworkFetcher.Factory` via a `META-INF/services` ServiceLoader entry
 * (confirmed by inspecting the artifact's own `.aar`), but Kotlin/Native has
 * no equivalent ServiceLoader mechanism — iOS needs the fetcher registered
 * explicitly. Rather than branch by platform, this registers it explicitly
 * on both, which is harmless on Android too (an explicit registration simply
 * wins over the auto-discovered one; same fetcher either way).
 *
 * [SingletonImageLoader.setSafe] is idempotent by design (logs a warning on
 * a second call rather than crashing) — [ensureInitialized] additionally
 * guards with `object`'s one-time `init {}` semantics so the registration
 * itself only happens once per process, not once per `AsyncImage` call.
 */
private object CoilNetworkFetcherRegistration {
    init {
        SingletonImageLoader.setSafe { context: PlatformContext ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }
}

/** Call once before the first [coil3.compose.AsyncImage] renders a network URL. */
fun ensureCoilNetworkFetcherRegistered() {
    CoilNetworkFetcherRegistration.hashCode() // touch the object to trigger its init {} block
}
