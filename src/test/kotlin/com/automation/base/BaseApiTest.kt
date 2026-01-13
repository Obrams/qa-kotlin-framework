package com.automation.base

import com.automation.core.api.ApiClient
import com.automation.core.config.TestConfig
import com.automation.core.threading.TestContext
import com.automation.core.threading.ThreadSafeReportCollector
import io.qameta.allure.Allure
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

private val logger = KotlinLogging.logger {}

abstract class BaseApiTest {
    protected lateinit var apiClient: ApiClient
    protected lateinit var reportCollector: ThreadSafeReportCollector

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        val testName = testInfo.displayName

        TestContext.initContext(testName, "api-test-user")
        reportCollector = ThreadSafeReportCollector()
        apiClient =
            ApiClient(
                baseUrl = TestConfig.PETSTORE_BASE_URL,
                reportCollector = reportCollector,
                defaultHeaders =
                    mapOf(
                        "Accept" to "application/json",
                        "Content-Type" to "application/json",
                    ),
            )

        logger.info { "Starting test: $testName" }
        Allure.parameter("Base URL", TestConfig.PETSTORE_BASE_URL)
    }

    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        try {
            reportCollector.generateAllureAttachments()
            val stats = reportCollector.getStatistics()
            logger.info {
                "Test completed: ${testInfo.displayName} - " +
                    "Requests: ${stats.totalRequests}, Errors: ${stats.totalErrors}, " +
                    "Avg Duration: ${stats.averageDuration}ms"
            }
        } finally {
            TestContext.cleanup()
        }
    }
}
