package com.ssbmax.shared.platform

import com.ssbmax.shared.BuildConfig

actual fun isDebugBuild(): Boolean = BuildConfig.DEBUG
