package com.automation.core.ui

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.WaitForSelectorState
import com.microsoft.playwright.options.WaitUntilState
import mu.KotlinLogging
import java.util.function.Predicate

private val logger = KotlinLogging.logger {}

/**
 * Wait conditions for Playwright.
 */
object WaitConditions {
    private const val DEFAULT_TIMEOUT_MS = 10000.0

    fun waitForVisible(
        locator: Locator,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ): Locator {
        logger.trace { "Waiting for element to be visible" }
        locator.waitFor(
            Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs),
        )
        return locator
    }

    fun waitForHidden(
        locator: Locator,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ): Locator {
        logger.trace { "Waiting for element to be hidden" }
        locator.waitFor(
            Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(timeoutMs),
        )
        return locator
    }

    fun waitForAttached(
        locator: Locator,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ): Locator {
        logger.trace { "Waiting for element to be attached" }
        locator.waitFor(
            Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED)
                .setTimeout(timeoutMs),
        )
        return locator
    }

    fun waitForDetached(
        locator: Locator,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ): Locator {
        logger.trace { "Waiting for element to be detached" }
        locator.waitFor(
            Locator.WaitForOptions()
                .setState(WaitForSelectorState.DETACHED)
                .setTimeout(timeoutMs),
        )
        return locator
    }

    fun waitForEnabled(
        locator: Locator,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ): Locator {
        logger.trace { "Waiting for element to be enabled" }
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (locator.isEnabled) return locator
            Thread.sleep(100)
        }
        throw IllegalStateException("Element not enabled after ${timeoutMs}ms")
    }

    fun waitForUrl(
        page: Page,
        urlPattern: String,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ) {
        logger.trace { "Waiting for URL to match: $urlPattern" }
        page.waitForURL(urlPattern, Page.WaitForURLOptions().setTimeout(timeoutMs))
    }

    fun waitForUrlContains(
        page: Page,
        substring: String,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ) {
        logger.trace { "Waiting for URL to contain: $substring" }
        // A glob like "**/*foo*" never matches URLs with a trailing slash ("*" does not cross "/"),
        // so match on the plain string. DOMCONTENTLOADED: do not block on ads/analytics finishing to load.
        page.waitForURL(
            Predicate<String> { it.contains(substring) },
            Page.WaitForURLOptions()
                .setTimeout(timeoutMs)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED),
        )
    }

    fun waitForLoadState(
        page: Page,
        state: LoadState = LoadState.NETWORKIDLE,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ) {
        logger.trace { "Waiting for load state: $state" }
        page.waitForLoadState(state, Page.WaitForLoadStateOptions().setTimeout(timeoutMs))
    }

    fun waitForText(
        locator: Locator,
        expectedText: String,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ): Locator {
        logger.trace { "Waiting for text: $expectedText" }
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val actualText = locator.textContent() ?: ""
            if (actualText.contains(expectedText)) return locator
            Thread.sleep(100)
        }
        throw IllegalStateException("Text '$expectedText' not found after ${timeoutMs}ms")
    }

    fun waitForCount(
        locator: Locator,
        expectedCount: Int,
        timeoutMs: Double = DEFAULT_TIMEOUT_MS,
    ): Locator {
        logger.trace { "Waiting for element count: $expectedCount" }
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (locator.count() == expectedCount) return locator
            Thread.sleep(100)
        }
        throw IllegalStateException("Expected $expectedCount elements, found ${locator.count()} after ${timeoutMs}ms")
    }
}
