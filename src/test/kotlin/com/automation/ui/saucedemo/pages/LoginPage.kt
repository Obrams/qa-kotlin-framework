package com.automation.ui.saucedemo.pages

import com.automation.core.ui.BaseScreen
import com.automation.data.factories.TestDataFactory.SauceDemo
import com.microsoft.playwright.Page
import io.qameta.allure.Step

class LoginPage(page: Page) : BaseScreen(page) {
    override val screenName = "Login Page"
    override val expectedUrlPattern = "saucedemo.com"

    private val usernameInput = css("#user-name", "Username input")
    private val passwordInput = css("#password", "Password input")
    private val loginButton = css("#login-button", "Login button")
    private val errorMessage = css("[data-test='error']", "Error message")
    private val logo = css(".login_logo", "Logo")

    @Step("Open login page")
    fun open(): LoginPage {
        navigate(SauceDemo.BASE_URL)
        waitForLoad()
        return this
    }

    @Step("Enter username: {username}")
    fun enterUsername(username: String): LoginPage {
        usernameInput.clearAndType(username)
        return this
    }

    @Step("Enter password")
    fun enterPassword(password: String): LoginPage {
        passwordInput.clearAndType(password)
        return this
    }

    @Step("Click login button")
    fun clickLogin(): LoginPage {
        loginButton.click()
        return this
    }

    @Step("Login with {username}")
    fun login(
        username: String,
        password: String,
    ): InventoryPage {
        enterUsername(username)
        enterPassword(password)
        clickLogin()
        return InventoryPage(page)
    }

    @Step("Login with invalid credentials expecting error")
    fun loginExpectingError(
        username: String,
        password: String,
    ): LoginPage {
        enterUsername(username)
        enterPassword(password)
        clickLogin()
        return this
    }

    fun getErrorMessage(): String {
        return errorMessage.waitForVisible().getText()
    }

    fun isErrorDisplayed(): Boolean {
        return errorMessage.isVisible()
    }

    override fun isDisplayed(): Boolean {
        return logo.isVisible() && loginButton.isVisible()
    }
}
