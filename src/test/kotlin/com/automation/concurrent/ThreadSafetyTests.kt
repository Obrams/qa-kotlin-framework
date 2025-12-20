package com.automation.concurrent

import com.automation.core.threading.TestContext
import com.automation.core.threading.ThreadSafeReportCollector
import io.qameta.allure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Epic("Concurrency")
@Feature("Thread Safety")
@Tag("concurrent")
@DisplayName("Thread Safety Tests")
class ThreadSafetyTests {
    @BeforeEach
    fun setUp() {
        if (TestContext.isInitialized()) {
            TestContext.cleanup()
        }
    }

    @AfterEach
    fun tearDown() {
        if (TestContext.isInitialized()) {
            TestContext.cleanup()
        }
    }

    @Test
    @Story("ThreadLocal Isolation")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("ThreadLocal context should be isolated between threads")
    fun testThreadLocalIsolation() {
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val errors = Collections.synchronizedList(mutableListOf<String>())
        val contextValues = ConcurrentHashMap<String, String>()
        val executor = Executors.newFixedThreadPool(threadCount)

        (1..threadCount).forEach { id ->
            executor.submit {
                try {
                    TestContext.initContext("test_$id", "user_$id")
                    Thread.sleep((50..150).random().toLong())
                    val context = TestContext.getContext()
                    contextValues[Thread.currentThread().name] = context.testName
                    if (context.testName != "test_$id") {
                        errors.add("Thread $id got wrong context: ${context.testName}")
                    }
                    if (context.userId != "user_$id") {
                        errors.add("Thread $id got wrong userId: ${context.userId}")
                    }
                } catch (e: Exception) {
                    errors.add("Thread $id exception: ${e.message}")
                } finally {
                    TestContext.cleanup()
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(errors)
            .withFailMessage("Thread isolation failures: $errors")
            .isEmpty()
        assertThat(contextValues).hasSize(threadCount)

        Allure.addAttachment(
            "Thread Context Values",
            "application/json",
            contextValues.entries.joinToString("\n") { "${it.key}: ${it.value}" },
        )
    }

    @Test
    @Story("ThreadSafeReportCollector")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ReportCollector should handle concurrent writes without data loss")
    fun testConcurrentMetricCollection() {
        val threadCount = 20
        val recordsPerThread = 100
        val expectedTotal = threadCount * recordsPerThread
        val collector = ThreadSafeReportCollector()
        val completedRecords = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        (1..threadCount).forEach { threadId ->
            executor.submit {
                try {
                    repeat(recordsPerThread) { recordId ->
                        collector.recordRequest(
                            requestId = "req_${threadId}_$recordId",
                            method = if (recordId % 2 == 0) "GET" else "POST",
                            path = "/api/test/$threadId/$recordId",
                            statusCode = listOf(200, 201, 204).random(),
                            duration = (10L..500L).random(),
                            threadName = Thread.currentThread().name,
                        )
                        completedRecords.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(60, TimeUnit.SECONDS)
        executor.shutdown()

        val metrics = collector.getMetrics()
        val stats = collector.getStatistics()

        assertThat(metrics).hasSize(expectedTotal)
        assertThat(stats.totalRequests).isEqualTo(expectedTotal.toLong())
        assertThat(stats.uniqueThreads).isGreaterThan(1)
        assertThat(stats.methodDistribution).containsKeys("GET", "POST")
        val totalMethods = stats.methodDistribution.values.sum()
        assertThat(totalMethods).isEqualTo(expectedTotal.toLong())

        collector.generateAllureAttachments()

        Allure.addAttachment(
            "Statistics Summary",
            "text/plain",
            """
            Total Requests: ${stats.totalRequests}
            Unique Threads: ${stats.uniqueThreads}
            Avg Duration: ${stats.averageDuration}ms
            Min Duration: ${stats.minDuration}ms
            Max Duration: ${stats.maxDuration}ms
            Status Codes: ${stats.statusCodeDistribution}
            Methods: ${stats.methodDistribution}
            """.trimIndent(),
        )
    }
}
