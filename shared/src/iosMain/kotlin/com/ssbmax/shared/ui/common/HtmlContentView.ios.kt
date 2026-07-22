package com.ssbmax.shared.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.WebKit.WKWebView

/**
 * iOS actual -- `WKWebView` wrapped in Compose Multiplatform's `UIKitView`
 * interop composable, the direct iOS-side analogue of Android's
 * `AndroidView`. Loads the same raw HTML string via `loadHTMLString`, no
 * base URL (matches the Android actual's `loadDataWithBaseURL(null, ...)`).
 */
@Composable
actual fun HtmlContentView(
    htmlContent: String,
    modifier: Modifier
) {
    UIKitView(
        factory = {
            WKWebView().apply {
                loadHTMLString(htmlContent, baseURL = null)
            }
        },
        modifier = modifier
    )
}
