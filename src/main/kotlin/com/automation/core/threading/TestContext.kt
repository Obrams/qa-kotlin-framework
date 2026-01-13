package com.automation.core.threading

import mu.KotlinLogging
import java.util.Collections
import java.util.UUID

private val logger = KotlinLogging.logger {}

object TestContext {
    private val threadLocalContext = ThreadLocal<TestData>()

    fun initContext(
        testName: String,
        userId: String = UUID.randomUUID().toString(),
    ) {
        val data =
            TestData(
                testName = testName,
                userId = userId,
                startTime = System.currentTimeMillis(),
                requestIds = Collections.synchronizedList(mutableListOf()),
                metadata = Collections.synchronizedMap(mutableMapOf()),
            )
        threadLocalContext.set(data)
        logger.debug { "Context initialized for test: $testName on thread: ${Thread.currentThread().name}" }
    }

    fun getContext(): TestData {
        return threadLocalContext.get()
            ?: throw IllegalStateException(
                "TestContext not initialized for thread: ${Thread.currentThread().name}. " +
                    "Call initContext() before using the context.",
            )
    }

    fun getContextOrNull(): TestData? = threadLocalContext.get()

    fun isInitialized(): Boolean = threadLocalContext.get() != null

    fun trackRequestId(requestId: String) {
        getContext().requestIds.add(requestId)
        logger.trace { "Tracked request: $requestId for test: ${getContext().testName}" }
    }

    fun addMetadata(
        key: String,
        value: Any,
    ) {
        getContext().metadata[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getMetadata(key: String): T? = getContext().metadata[key] as? T

    fun getTestDuration(): Long {
        val context = getContextOrNull() ?: return 0
        return System.currentTimeMillis() - context.startTime
    }

    fun cleanup() {
        val testName = getContextOrNull()?.testName ?: "unknown"
        val duration = getTestDuration()
        threadLocalContext.remove()
        logger.debug { "Context cleaned up for test: $testName (duration: ${duration}ms)" }
    }
}

data class TestData(
    val testName: String,
    val userId: String,
    val startTime: Long,
    val requestIds: MutableList<String>,
    val metadata: MutableMap<String, Any>,
) {
    fun getRequestIdSnapshot(): List<String> = requestIds.toList()

    fun getMetadataSnapshot(): Map<String, Any> = metadata.toMap()
}
