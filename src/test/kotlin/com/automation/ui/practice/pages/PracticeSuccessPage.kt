package com.automation.ui.practice.pages

import com.automation.core.ui.BaseScreen
import com.microsoft.playwright.Page
import io.qameta.allure.Step

class PracticeSuccessPage(page: Page) : BaseScreen(page) {
    override val screenName = "Practice Success Page"
    override val expectedUrlPattern = "logged-in-successfully"

    private val successMessage = css(".post-title", "Success message")
    // match by visible text: the site markup changes, the button label does not
    private val logoutButton = text("Log out", exact = true)
    private val contentText = css(".has-text-align-center", "Content text")

    fun getSuccessMessage(): String {
        return successMessage.waitForVisible().getText()
    }

    override fun isDisplayed(): Boolean {
        return getCurrentUrl().contains("logged-in-successfully") ||
            successMessage.isVisible()
    }

    fun getContentText(): String {
        return contentText.getText()
    }

    fun isLogoutButtonVisible(): Boolean {
        return runCatching { logoutButton.waitForVisible() }.isSuccess
    }

    @Step("Click logout")
    fun logout(): PracticeLoginPage {
        logoutButton.click()
        return PracticeLoginPage(page)
    }
}
