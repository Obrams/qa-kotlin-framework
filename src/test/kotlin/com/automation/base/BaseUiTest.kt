package com.automation.base

import com.automation.core.config.TestConfig
import com.automation.core.threading.TestContext
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.qameta.allure.Allure
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

private val logger = KotlinLogging.logger {}

/**
 * Base class for UI tests.
 *
 * Playwright for Java is not thread-safe, and JUnit runs test methods concurrently,
 * so every test gets its own Playwright instance, browser, context and page.
 * Nothing Playwright-related is shared between threads.
 */
abstract class BaseUiTest {
    protected lateinit var page: Page
    protected lateinit var context: BrowserContext
    private lateinit var playwright: Playwright
    private lateinit var browser: Browser

    @BeforeEach
    open fun setUp(testInfo: TestInfo) {
        val testName = testInfo.displayName
        TestContext.initContext(testName, "ui-test-user")

        playwright = Playwright.create()
        browser =
            playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(TestConfig.BROWSER_HEADLESS),
            )
        context =
            browser.newContext(
                Browser.NewContextOptions()
                    .setViewportSize(TestConfig.VIEWPORT_WIDTH, TestConfig.VIEWPORT_HEIGHT),
            )
        page = context.newPage()

        logger.info { "Starting UI test: $testName (headless: ${TestConfig.BROWSER_HEADLESS})" }
        Allure.parameter("Browser", "Chromium")
        Allure.parameter("Viewport", "${TestConfig.VIEWPORT_WIDTH}x${TestConfig.VIEWPORT_HEIGHT}")
    }

    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        try {
            if (::page.isInitialized) {
                page.close()
            }
            if (::context.isInitialized) {
                context.close()
            }
        } finally {
            if (::browser.isInitialized) {
                browser.close()
            }
            if (::playwright.isInitialized) {
                playwright.close()
            }
            TestContext.cleanup()
            logger.info { "UI test completed: ${testInfo.displayName}" }
        }
    }

    protected fun takeScreenshot(name: String = "Screenshot") {
        try {
            val screenshot = page.screenshot(Page.ScreenshotOptions().setFullPage(true))
            Allure.addAttachment(name, "image/png", screenshot.inputStream(), ".png")
        } catch (e: Exception) {
            logger.warn { "Failed to take screenshot: ${e.message}" }
        }
    }

    /**
     * Runs a UI test body and attaches a screenshot on failure.
     * Returns Unit on purpose: JUnit 5 silently ignores @Test methods that return a value,
     * so `fun test() = withScreenshotOnFailure { ... }` must resolve to Unit.
     */
    protected inline fun withScreenshotOnFailure(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            takeScreenshot("Failure Screenshot")
            throw e
        }
    }
}
