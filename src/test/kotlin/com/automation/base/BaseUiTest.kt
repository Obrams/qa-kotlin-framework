package com.automation.base

import com.automation.core.config.TestConfig
import com.automation.core.threading.TestContext
import com.automation.core.ui.BrowserManager
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import io.qameta.allure.Allure
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

private val logger = KotlinLogging.logger {}

abstract class BaseUiTest {
    protected lateinit var page: Page
    protected lateinit var context: BrowserContext

    companion object {
        private lateinit var browser: Browser

        @JvmStatic
        @BeforeAll
        fun setUpBrowser() {
            browser = BrowserManager.init(TestConfig.BROWSER_HEADLESS)
            logger.info { "Browser initialized (headless: ${TestConfig.BROWSER_HEADLESS})" }
        }

        @JvmStatic
        @AfterAll
        fun tearDownBrowser() {
            BrowserManager.close()
            logger.info { "Browser closed" }
        }
    }

    @BeforeEach
    open fun setUp(testInfo: TestInfo) {
        val testName = testInfo.displayName
        TestContext.initContext(testName, "ui-test-user")
        context =
            browser.newContext(
                Browser.NewContextOptions()
                    .setViewportSize(TestConfig.VIEWPORT_WIDTH, TestConfig.VIEWPORT_HEIGHT),
            )
        page = context.newPage()

        logger.info { "Starting UI test: $testName" }
        Allure.parameter("Browser", "Chromium")
        Allure.parameter("Viewport", "${TestConfig.VIEWPORT_WIDTH}x${TestConfig.VIEWPORT_HEIGHT}")
    }

    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        if (::page.isInitialized) {
            page.close()
        }
        if (::context.isInitialized) {
            context.close()
        }
        TestContext.cleanup()

        logger.info { "UI test completed: ${testInfo.displayName}" }
    }

    protected fun takeScreenshot(name: String = "Screenshot") {
        try {
            val screenshot = page.screenshot(Page.ScreenshotOptions().setFullPage(true))
            Allure.addAttachment(name, "image/png", screenshot.inputStream(), ".png")
        } catch (e: Exception) {
            logger.warn { "Failed to take screenshot: ${e.message}" }
        }
    }

    protected inline fun <T> withScreenshotOnFailure(block: () -> T): T {
        return try {
            block()
        } catch (e: Throwable) {
            takeScreenshot("Failure Screenshot")
            throw e
        }
    }
}
