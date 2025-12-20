package com.automation.core.ui

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import io.qameta.allure.Allure
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Base class for Page Objects.
 */
abstract class BaseScreen(
    protected val page: Page,
) {
    abstract val screenName: String
    open val expectedUrlPattern: String? = null

    protected fun css(
        selector: String,
        name: String = selector,
    ): ScreenElement {
        return ScreenElement(page.locator(selector), name)
    }

    protected fun testId(
        id: String,
        name: String = id,
    ): ScreenElement {
        return ScreenElement(page.getByTestId(id), name)
    }

    protected fun text(
        text: String,
        exact: Boolean = false,
    ): ScreenElement {
        val locator =
            if (exact) {
                page.getByText(text, Page.GetByTextOptions().setExact(true))
            } else {
                page.getByText(text)
            }
        return ScreenElement(locator, "text='$text'")
    }

    protected fun role(
        role: com.microsoft.playwright.options.AriaRole,
        name: String? = null,
    ): ScreenElement {
        val options = name?.let { Page.GetByRoleOptions().setName(it) }
        val locator = page.getByRole(role, options)
        return ScreenElement(locator, "role=$role${name?.let { "[$it]" } ?: ""}")
    }

    protected fun button(name: String): ScreenElement {
        return role(com.microsoft.playwright.options.AriaRole.BUTTON, name)
    }

    protected fun link(name: String): ScreenElement {
        return role(com.microsoft.playwright.options.AriaRole.LINK, name)
    }

    protected fun inputByLabel(label: String): ScreenElement {
        return ScreenElement(page.getByLabel(label), "label='$label'")
    }

    protected fun inputByPlaceholder(placeholder: String): ScreenElement {
        return ScreenElement(page.getByPlaceholder(placeholder), "placeholder='$placeholder'")
    }

    protected fun xpath(
        expression: String,
        name: String = expression,
    ): ScreenElement {
        return ScreenElement(page.locator("xpath=$expression"), name)
    }

    fun getCurrentUrl(): String = page.url()

    fun getTitle(): String = page.title()

    fun navigate(url: String) {
        logger.info { "Navigating to: $url" }
        page.navigate(url)
    }

    fun refresh() {
        logger.debug { "Refreshing page" }
        page.reload()
    }

    fun goBack() {
        logger.debug { "Going back" }
        page.goBack()
    }

    fun goForward() {
        logger.debug { "Going forward" }
        page.goForward()
    }

    fun waitForLoad() {
        page.waitForLoadState(LoadState.NETWORKIDLE)
    }

    fun waitForNavigation(action: () -> Unit) {
        page.waitForNavigation { action() }
    }

    fun verifyUrl(urlPattern: String = expectedUrlPattern ?: "") {
        WaitConditions.waitForUrlContains(page, urlPattern)
    }

    fun takeScreenshot(name: String = screenName): ByteArray {
        val screenshot = page.screenshot(Page.ScreenshotOptions().setFullPage(true))
        Allure.addAttachment(name, "image/png", screenshot.inputStream(), ".png")
        logger.debug { "Screenshot taken: $name" }
        return screenshot
    }

    fun takeScreenshotOnFailure(testName: String) {
        try {
            val screenshot = page.screenshot(Page.ScreenshotOptions().setFullPage(true))
            Allure.addAttachment("Failure - $testName", "image/png", screenshot.inputStream(), ".png")
        } catch (e: Exception) {
            logger.warn { "Failed to take screenshot: ${e.message}" }
        }
    }

    fun executeScript(script: String): Any? {
        return page.evaluate(script)
    }

    fun scrollToTop() {
        executeScript("window.scrollTo(0, 0)")
    }

    fun scrollToBottom() {
        executeScript("window.scrollTo(0, document.body.scrollHeight)")
    }

    /**
     * Handles alert dialogs.
     * Must be called BEFORE the action that triggers the alert.
     *
     * @param accept true to accept the alert, false to dismiss
     * @param action the action that triggers the alert
     * @return the alert message text
     */
    fun handleAlert(
        accept: Boolean = true,
        action: () -> Unit,
    ): String {
        var alertText = ""
        page.onceDialog { dialog ->
            alertText = dialog.message()
            if (accept) {
                dialog.accept()
            } else {
                dialog.dismiss()
            }
        }
        action()
        return alertText
    }

    /**
     * Accepts an alert dialog.
     * Must be called BEFORE the action that triggers the alert.
     */
    fun acceptAlert(action: () -> Unit): String {
        return handleAlert(accept = true, action = action)
    }

    /**
     * Dismisses an alert dialog.
     * Must be called BEFORE the action that triggers the alert.
     */
    fun dismissAlert(action: () -> Unit): String {
        return handleAlert(accept = false, action = action)
    }

    /**
     * Switches to a frame and executes actions within it.
     *
     * @param frameSelector CSS selector for the frame
     * @param action actions to perform within the frame
     */
    fun <T> withinFrame(
        frameSelector: String,
        action: (frameLocator: com.microsoft.playwright.FrameLocator) -> T,
    ): T {
        val frameLocator = page.frameLocator(frameSelector)
        logger.debug { "Switching to frame: $frameSelector" }
        return action(frameLocator)
    }

    /**
     * Gets a ScreenElement within a specific frame.
     */
    fun frameElement(
        frameSelector: String,
        elementSelector: String,
        name: String = elementSelector,
    ): ScreenElement {
        val locator = page.frameLocator(frameSelector).locator(elementSelector)
        return ScreenElement(locator, "$name (in frame)")
    }

    open fun isDisplayed(): Boolean {
        return expectedUrlPattern?.let { getCurrentUrl().contains(it) } ?: true
    }
}
