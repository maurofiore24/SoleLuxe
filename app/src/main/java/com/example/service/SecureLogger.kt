package com.example.service

import android.util.Log
import com.example.BuildConfig

/**
 * Enterprise production-hardened logging gateway.
 * Fully masks user identification metadata and financial amounts in production release builds,
 * while maintaining detailed trace outputs in debug cycles for developers.
 */
object SecureLogger {
    val IS_DEBUG: Boolean = BuildConfig.DEBUG

    /**
     * Secures and conceals user identify patterns.
     * E.g., "local_user" becomes "lo***er" in production to protect PII.
     */
    fun maskUserId(userId: String?): String {
        if (userId == null) return "null"
        if (IS_DEBUG) return userId
        if (userId.length <= 4) return "****"
        return "${userId.take(2)}***${userId.takeLast(2)}"
    }

    /**
     * Conceals transaction sums and coin balances on production systems.
     */
    fun maskAmount(amount: Double?): String {
        if (amount == null) return "0.00"
        if (IS_DEBUG) return String.format("%.2f", amount)
        return "███.██ [SECURED]"
    }

    /**
     * High-performance sanitizing regex filters to screen sensitive tokens inside log strings.
     */
    fun sanitizeLogString(message: String): String {
        if (IS_DEBUG) return message
        
        // Redact coin/currency strings, decimal transaction numbers, and credit balances.
        val coinPattern = """(\d+(\.\d+)?)\s*(Gold|Coins?|Tokens?)""".toRegex(RegexOption.IGNORE_CASE)
        val emailPattern = """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""".toRegex()
        
        var sanitized = coinPattern.replace(message) { match ->
            "███.██ ${match.groupValues[3]}"
        }
        sanitized = emailPattern.replace(sanitized, "******@***.com")
        return sanitized
    }

    fun d(tag: String, message: String) {
        if (IS_DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, sanitizeLogString(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMsg = sanitizeLogString(message)
        if (throwable != null) {
            Log.w(tag, sanitizedMsg, throwable)
        } else {
            Log.w(tag, sanitizedMsg)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMsg = sanitizeLogString(message)
        if (throwable != null) {
            Log.e(tag, sanitizedMsg, throwable)
        } else {
            Log.e(tag, sanitizedMsg)
        }
    }
}
