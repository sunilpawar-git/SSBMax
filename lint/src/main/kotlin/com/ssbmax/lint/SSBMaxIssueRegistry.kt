package com.ssbmax.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/**
 * SSBMax Custom Lint Issue Registry
 *
 * Registers all custom lint checks for the SSBMax project.
 * This registry is automatically discovered by the Android Lint framework
 * via the service loader mechanism.
 *
 * Current lint rules:
 * 1. HardcodedTextDetector - Enforces stringResource() usage in Compose Text
 * 2. SingletonMutableStateDetector - Prevents singleton anti-pattern (like OIRTestResultHolder)
 * 3. NavigationComplexObjectDetector - Enforces ID-based navigation (prevents passing complex objects)
 * 4. ViewModelLifecycleDetector - Ensures resources are cancelled in onCleared()
 * 5. PrintStackTraceDetector - Prevents printStackTrace() usage (use ErrorLogger instead)
 * 6. FirebaseInUILayerDetector - Prevents Firebase imports in UI layer (use domain abstractions)
 */
class SSBMaxIssueRegistry : IssueRegistry() {

    override val issues = listOf(
        HardcodedTextDetector.ISSUE,
        SingletonMutableStateDetector.ISSUE,
        NavigationComplexObjectDetector.ISSUE,
        ViewModelLifecycleDetector.ISSUE,
        PrintStackTraceDetector.ISSUE,
        FirebaseInUILayerDetector.ISSUE
    )

    override val api: Int = CURRENT_API

    override val minApi: Int = 14

    override val vendor: Vendor = Vendor(
        vendorName = "SSBMax",
        feedbackUrl = "https://github.com/ssbmax/ssbmax/issues",
        contact = "dev@ssbmax.com"
    )
}
