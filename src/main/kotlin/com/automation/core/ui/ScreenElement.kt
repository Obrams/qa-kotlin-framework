package com.automation.core.ui

import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.WaitForSelectorState
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ScreenElement(
    private val locator: Locator,
    private val name: String = "element",
) {
    private val defaultTimeout = 10000.0

    fun getLocator(): Locator = locator

    fun click(): ScreenElement {
        logger.debug { "Clicking on '$name'" }
        waitForVisible()
        locator.click()
        return this
    }

    fun doubleClick(): ScreenElement {
        logger.debug { "Double clicking on '$name'" }
        waitForVisible()
        locator.dblclick()
        return this
    }

    fun type(text: String): ScreenElement {
        logger.debug { "Typing into '$name': $text" }
        waitForVisible()
        locator.fill(text)
        return this
    }

    fun clearAndType(text: String): ScreenElement {
        logger.debug { "Clearing and typing into '$name': $text" }
        waitForVisible()
        locator.clear()
        locator.fill(text)
        return this
    }

    fun typeSlowly(
        text: String,
        delayMs: Double = 50.0,
    ): ScreenElement {
        logger.debug { "Typing slowly into '$name': $text" }
        waitForVisible()
        locator.pressSequentially(text, Locator.PressSequentiallyOptions().setDelay(delayMs))
        return this
    }

    fun pressKey(key: String): ScreenElement {
        logger.debug { "Pressing key '$key' on '$name'" }
        locator.press(key)
        return this
    }

    fun hover(): ScreenElement {
        logger.debug { "Hovering over '$name'" }
        waitForVisible()
        locator.hover()
        return this
    }

    fun getText(): String {
        waitForVisible()
        return locator.textContent() ?: ""
    }

    fun getInnerText(): String {
        waitForVisible()
        return locator.innerText()
    }

    fun getValue(): String {
        waitForVisible()
        return locator.inputValue()
    }

    fun getAttribute(name: String): String? {
        waitForVisible()
        return locator.getAttribute(name)
    }

    fun isVisible(): Boolean = locator.isVisible

    fun isEnabled(): Boolean = locator.isEnabled

    fun isChecked(): Boolean = locator.isChecked

    fun selectByValue(value: String): ScreenElement {
        logger.debug { "Selecting value '$value' in '$name'" }
        waitForVisible()
        locator.selectOption(value)
        return this
    }

    fun selectByText(text: String): ScreenElement {
        logger.debug { "Selecting text '$text' in '$name'" }
        waitForVisible()
        locator.selectOption(arrayOf(text))
        return this
    }

    fun check(): ScreenElement {
        logger.debug { "Checking '$name'" }
        waitForVisible()
        locator.check()
        return this
    }

    fun uncheck(): ScreenElement {
        logger.debug { "Unchecking '$name'" }
        waitForVisible()
        locator.uncheck()
        return this
    }

    fun scrollIntoView(): ScreenElement {
        logger.debug { "Scrolling '$name' into view" }
        locator.scrollIntoViewIfNeeded()
        return this
    }

    fun waitForVisible(timeoutMs: Double = defaultTimeout): ScreenElement {
        locator.waitFor(
            Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs),
        )
        return this
    }

    fun waitForHidden(timeoutMs: Double = defaultTimeout): ScreenElement {
        locator.waitFor(
            Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(timeoutMs),
        )
        return this
    }

    fun count(): Int = locator.count()

    fun nth(index: Int): ScreenElement {
        return ScreenElement(locator.nth(index), "$name[$index]")
    }

    fun first(): ScreenElement = nth(0)

    fun last(): ScreenElement = nth(count() - 1)

    fun all(): List<ScreenElement> {
        return locator.all().mapIndexed { index, loc ->
            ScreenElement(loc, "$name[$index]")
        }
    }

    fun filterByText(text: String): ScreenElement {
        return ScreenElement(
            locator.filter(Locator.FilterOptions().setHasText(text)),
            "$name[text='$text']",
        )
    }

    fun screenshot(): ByteArray {
        return locator.screenshot()
    }

    fun highlight(): ScreenElement {
        locator.highlight()
        return this
    }
}
