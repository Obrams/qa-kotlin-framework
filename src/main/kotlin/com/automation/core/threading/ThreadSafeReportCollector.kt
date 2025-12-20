package com.automation.core.threading

import io.qameta.allure.Allure
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * Thread-safe collector for API request metrics.
 */
class ThreadSafeReportCollector {
    private val metrics = mutableListOf<RequestMetric>()
    private val errors = mutableListOf<ErrorRecord>()
    private val lock = ReentrantReadWriteLock()

    private val statusCodeCounts = ConcurrentHashMap<Int, AtomicLong>()
    private val methodCounts = ConcurrentHashMap<String, AtomicLong>()
    private val totalRequests = AtomicLong(0)
    private val totalErrors = AtomicLong(0)
    private val totalDuration = AtomicLong(0)

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    fun recordRequest(
        requestId: String,
        method: String,
        path: String,
        statusCode: Int,
        duration: Long,
        threadName: String = Thread.currentThread().name,
        requestBody: String? = null,
        responseBody: String? = null,
    ) {
        val metric =
            RequestMetric(
                requestId = requestId,
                method = method,
                path = path,
                statusCode = statusCode,
                duration = duration,
                threadName = threadName,
                timestamp = System.currentTimeMillis(),
                requestBody = requestBody,
                responseBody = responseBody?.take(1000),
            )

        lock.write {
            metrics.add(metric)
        }

        totalRequests.incrementAndGet()
        totalDuration.addAndGet(duration)
        statusCodeCounts.computeIfAbsent(statusCode) { AtomicLong(0) }.incrementAndGet()
        methodCounts.computeIfAbsent(method) { AtomicLong(0) }.incrementAndGet()

        logger.trace { "Recorded request: $method $path -> $statusCode (${duration}ms) on $threadName" }
    }

    fun recordError(
        requestId: String,
        exception: Throwable,
        method: String? = null,
        path: String? = null,
    ) {
        val errorRecord =
            ErrorRecord(
                requestId = requestId,
                errorType = exception::class.simpleName ?: "Unknown",
                message = exception.message ?: "No message",
                stackTrace = exception.stackTraceToString().take(2000),
                method = method,
                path = path,
                threadName = Thread.currentThread().name,
                timestamp = System.currentTimeMillis(),
            )

        lock.write {
            errors.add(errorRecord)
        }

        totalErrors.incrementAndGet()
        logger.warn { "Recorded error for request $requestId: ${exception.message}" }
    }

    fun getMetrics(): List<RequestMetric> {
        return lock.read { metrics.toList() }
    }

    fun getErrors(): List<ErrorRecord> {
        return lock.read { errors.toList() }
    }

    fun getStatistics(): MetricStatistics {
        val metricsCopy = getMetrics()
        val total = totalRequests.get()
        val avgDuration = if (total > 0) totalDuration.get() / total else 0

        return MetricStatistics(
            totalRequests = total,
            totalErrors = totalErrors.get(),
            averageDuration = avgDuration,
            minDuration = metricsCopy.minOfOrNull { it.duration } ?: 0,
            maxDuration = metricsCopy.maxOfOrNull { it.duration } ?: 0,
            statusCodeDistribution = statusCodeCounts.mapValues { it.value.get() },
            methodDistribution = methodCounts.mapValues { it.value.get() },
            uniqueThreads = metricsCopy.map { it.threadName }.toSet().size,
        )
    }

    fun generateAllureAttachments() {
        val metrics = getMetrics()
        val errors = getErrors()
        val stats = getStatistics()

        if (metrics.isNotEmpty()) {
            Allure.addAttachment(
                "API Metrics",
                "application/json",
                json.encodeToString(metrics),
                ".json",
            )
        }

        if (errors.isNotEmpty()) {
            Allure.addAttachment(
                "API Errors",
                "application/json",
                json.encodeToString(errors),
                ".json",
            )
        }

        Allure.addAttachment(
            "Metrics Summary",
            "application/json",
            json.encodeToString(stats),
            ".json",
        )

        logger.info { "Generated Allure attachments: ${metrics.size} requests, ${errors.size} errors" }
    }

    fun clear() {
        lock.write {
            metrics.clear()
            errors.clear()
        }
        statusCodeCounts.clear()
        methodCounts.clear()
        totalRequests.set(0)
        totalErrors.set(0)
        totalDuration.set(0)
        logger.debug { "Report collector cleared" }
    }
}

@Serializable
data class RequestMetric(
    val requestId: String,
    val method: String,
    val path: String,
    val statusCode: Int,
    val duration: Long,
    val threadName: String,
    val timestamp: Long,
    val requestBody: String? = null,
    val responseBody: String? = null,
)

@Serializable
data class ErrorRecord(
    val requestId: String,
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val method: String?,
    val path: String?,
    val threadName: String,
    val timestamp: Long,
)

@Serializable
data class MetricStatistics(
    val totalRequests: Long,
    val totalErrors: Long,
    val averageDuration: Long,
    val minDuration: Long,
    val maxDuration: Long,
    val statusCodeDistribution: Map<Int, Long>,
    val methodDistribution: Map<String, Long>,
    val uniqueThreads: Int,
)
