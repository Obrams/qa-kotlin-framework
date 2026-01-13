package com.automation.utils

import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 5000,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    repeat(maxRetries - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            logger.debug { "Attempt ${attempt + 1} failed: ${e.message}, retrying in ${currentDelay}ms" }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
    }
    return block()
}

fun <T> retryBlocking(
    maxRetries: Int = 3,
    delayMs: Long = 500,
    block: () -> T,
): T {
    repeat(maxRetries - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            logger.debug { "Attempt ${attempt + 1} failed: ${e.message}, retrying in ${delayMs}ms" }
            Thread.sleep(delayMs)
        }
    }
    return block()
}

inline fun <T> measureTimeMillis(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - start
    return result to duration
}

/**
 * Waits until a condition is met or timeout expires.
 *
 * @throws IllegalStateException if condition is not met within timeout
 */
fun waitUntil(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    message: String = "Condition not met",
    condition: () -> Boolean,
): Boolean {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
        if (condition()) return true
        Thread.sleep(pollIntervalMs)
    }
    throw IllegalStateException("$message after ${timeoutMs}ms")
}

fun <T> safeExecute(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        logger.warn { "Safe execution failed: ${e.message}" }
        null
    }
}

fun <T> Result<T>.getOrLog(
    default: T,
    logMessage: String = "Operation failed",
): T {
    return getOrElse { exception ->
        logger.warn(exception) { logMessage }
        default
    }
}
