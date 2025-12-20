package com.automation.ui.practice.pages

import com.automation.core.ui.BaseScreen
import com.automation.data.factories.TestDataFactory.PracticeTestAutomation
import com.microsoft.playwright.Page
import io.qameta.allure.Step

class PracticeLoginPage(page: Page) : BaseScreen(page) {
    override val screenName = "Practice Login Page"
    override val expectedUrlPattern = "practice-test-login"

    private val usernameInput = css("#username", "Username input")
    private val passwordInput = css("#password", "Password input")
    private val submitButton = css("#submit", "Submit button")
    private val errorMessage = css("#error", "Error message")

    @Step("Open practice login page")
    fun open(): PracticeLoginPage {
        navigate(PracticeTestAutomation.LOGIN_URL)
        waitForLoad()
        return this
    }

    @Step("Enter username: {username}")
    fun enterUsername(username: String): PracticeLoginPage {
        usernameInput.clearAndType(username)
        return this
    }

    @Step("Enter password")
    fun enterPassword(password: String): PracticeLoginPage {
        passwordInput.clearAndType(password)
        return this
    }

    @Step("Click submit")
    fun clickSubmit(): PracticeLoginPage {
        submitButton.click()
        return this
    }

    @Step("Login with {username}")
    fun login(
        username: String,
        password: String,
    ): PracticeSuccessPage {
        enterUsername(username)
        enterPassword(password)
        clickSubmit()
        return PracticeSuccessPage(page)
    }

    @Step("Login expecting error")
    fun loginExpectingError(
        username: String,
        password: String,
    ): PracticeLoginPage {
        enterUsername(username)
        enterPassword(password)
        clickSubmit()
        return this
    }

    fun getErrorMessage(): String {
        return errorMessage.waitForVisible().getText()
    }

    fun isErrorDisplayed(): Boolean {
        return errorMessage.isVisible()
    }

    override fun isDisplayed(): Boolean {
        return usernameInput.isVisible() && submitButton.isVisible()
    }
}
