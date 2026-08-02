package com.ssbmax.shared.platform.util

import com.ssbmax.shared.domain.util.DomainLogger
import platform.Foundation.NSLog

/**
 * iOS actual. Uses `NSLog` rather than `os_log`: `os_log` is a variadic C
 * function, which Kotlin/Native cannot call directly (no cinterop binding
 * for C varargs); `NSLog` is the one variadic Apple logging function
 * Kotlin/Native's Objective-C interop special-cases as callable from Kotlin.
 * Same destination developers actually look at (Xcode console / Console.app
 * via the OS log stream) — a substitution of mechanism, not of outcome.
 */
class IosDomainLogger : DomainLogger {
    override fun d(tag: String, message: String) = log("D", tag, message)
    override fun e(tag: String, message: String, throwable: Throwable?) =
        log("E", tag, message + (throwable?.let { " — ${it.stackTraceToString()}" } ?: ""))
    override fun w(tag: String, message: String) = log("W", tag, message)
    override fun i(tag: String, message: String) = log("I", tag, message)
    override fun v(tag: String, message: String) = log("V", tag, message)

    private fun log(level: String, tag: String, message: String) {
        NSLog("[$level/$tag] $message")
    }
}
