package com.automation.core.ui

import com.automation.core.config.TestConfig
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import mu.KotlinLogging
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

object BrowserManager {
    @Volatile
    private var playwright: Playwright? = null

    @Volatile
    private var browser: Browser? = null

    private val lock = Any()

    fun init(headless: Boolean = TestConfig.BROWSER_HEADLESS): Browser {
        if (browser?.isConnected == true) {
            return browser!!
        }

        synchronized(lock) {
            if (browser?.isConnected == true) {
                return browser!!
            }

            if (playwright == null) {
                playwright = Playwright.create()
                logger.info { "Playwright instance created" }
            }

            browser =
                playwright!!.chromium().launch(
                    BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(50.0),
                )
            logger.info { "Browser initialized (headless: $headless)" }

            return browser!!
        }
    }

    fun createContext(
        viewport: Pair<Int, Int>? = null,
        recordVideo: Boolean = false,
    ): BrowserContext {
        val browser = init()
        val options = Browser.NewContextOptions()

        viewport?.let { (width, height) ->
            options.setViewportSize(width, height)
        } ?: options.setViewportSize(TestConfig.VIEWPORT_WIDTH, TestConfig.VIEWPORT_HEIGHT)

        if (recordVideo) {
            options.setRecordVideoDir(Paths.get("build/videos"))
        }

        return browser.newContext(options)
    }

    fun createPage(): Page {
        return createContext().newPage()
    }

    fun close() {
        synchronized(lock) {
            try {
                browser?.close()
                logger.debug { "Browser closed" }
            } catch (e: Exception) {
                logger.warn { "Error closing browser: ${e.message}" }
            }

            try {
                playwright?.close()
                logger.debug { "Playwright closed" }
            } catch (e: Exception) {
                logger.warn { "Error closing Playwright: ${e.message}" }
            }

            browser = null
            playwright = null
        }
    }

    fun isRunning(): Boolean = browser?.isConnected == true
}
