package com.automation.ui.practice

import com.automation.base.BaseUiTest
import com.automation.data.factories.TestDataFactory.PracticeTestAutomation
import com.automation.ui.practice.pages.PracticeLoginPage
import io.qameta.allure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Epic("Practice Test Automation")
@Feature("Authentication")
@Tag("ui")
@Tag("practice")
@DisplayName("Practice Test Automation Login Tests")
class LoginTests : BaseUiTest() {
    @Test
    @Story("Successful Login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Should login successfully with valid credentials")
    fun testSuccessfulLogin() =
        withScreenshotOnFailure {
            val loginPage = PracticeLoginPage(page).open()

            val successPage =
                loginPage.login(
                    username = PracticeTestAutomation.VALID_USERNAME,
                    password = PracticeTestAutomation.VALID_PASSWORD,
                )

            assertThat(successPage.isDisplayed()).isTrue()
            assertThat(successPage.getSuccessMessage())
                .containsIgnoringCase("logged in")
            assertThat(successPage.isLogoutButtonVisible()).isTrue()
        }

    @Test
    @Story("Invalid Credentials")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should show error for invalid username")
    fun testInvalidUsername() =
        withScreenshotOnFailure {
            val loginPage = PracticeLoginPage(page).open()

            loginPage.loginExpectingError(
                username = PracticeTestAutomation.INVALID_USERNAME,
                password = PracticeTestAutomation.VALID_PASSWORD,
            )

            assertThat(loginPage.isErrorDisplayed()).isTrue()
            assertThat(loginPage.getErrorMessage())
                .containsIgnoringCase("invalid")
        }

    @Test
    @Story("Invalid Credentials")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should show error for invalid password")
    fun testInvalidPassword() =
        withScreenshotOnFailure {
            val loginPage = PracticeLoginPage(page).open()

            loginPage.loginExpectingError(
                username = PracticeTestAutomation.VALID_USERNAME,
                password = PracticeTestAutomation.INVALID_PASSWORD,
            )

            assertThat(loginPage.isErrorDisplayed()).isTrue()
            assertThat(loginPage.getErrorMessage())
                .containsIgnoringCase("invalid")
        }
}
