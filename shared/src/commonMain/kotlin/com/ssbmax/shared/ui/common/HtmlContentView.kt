package com.ssbmax.shared.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * KMP `expect`/`actual` shim for the Android original's
 * `app/.../ui/components/PIQFormWebView.kt` (Android `WebView`-backed HTML
 * renderer used to display the SSB PIQ form document, the only HTML-content
 * study material). No official Compose Multiplatform HTML-rendering
 * composable exists (JetBrains' resources API is text/binary only) -- a real
 * `expect`/`actual` shim per platform, same pattern already established by
 * `platform/{tts,audio,billing}` for this migration, rather than skipping
 * the feature or faking it with plain text.
 *
 * Android actual: unchanged `android.webkit.WebView` wrapped in `AndroidView`.
 * iOS actual: `WKWebView` wrapped in `UIKitView` (Compose Multiplatform's
 * iOS-side interop composable, the direct analogue of `AndroidView`).
 */
@Composable
expect fun HtmlContentView(
    htmlContent: String,
    modifier: Modifier = Modifier
)
