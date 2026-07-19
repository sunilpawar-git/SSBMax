package com.ssbmax.shared.di

import org.koin.core.module.Module

/**
 * DatabaseDriverFactory's constructor differs per platform (Android needs a
 * Context, iOS doesn't take one), so common code can't call `DatabaseDriverFactory(...)`
 * directly — Koin's binding for it must live in a platform module instead.
 */
expect val platformModule: Module
