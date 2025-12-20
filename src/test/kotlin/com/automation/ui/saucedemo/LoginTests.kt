package com.automation.ui.saucedemo

import com.automation.base.BaseUiTest
import com.automation.data.factories.TestDataFactory.SauceDemo
import com.automation.ui.saucedemo.pages.LoginPage
import io.qameta.allure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Epic("SauceDemo")
@Feature("Authentication")
@Tag("ui")
@Tag("saucedemo")
@DisplayName("SauceDemo Login Tests")
class LoginTests : BaseUiTest() {
    @Test
    @Story("Standard User Login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Should login successfully with standard user")
    fun testStandardUserLogin() =
        withScreenshotOnFailure {
            val loginPage = LoginPage(page).open()

            val inventoryPage =
                loginPage.login(
                    username = SauceDemo.STANDARD_USER,
                    password = SauceDemo.PASSWORD,
                )

            assertThat(inventoryPage.isDisplayed()).isTrue()
            assertThat(inventoryPage.getPageTitle()).isEqualTo("Products")
            assertThat(inventoryPage.getProductCount()).isGreaterThan(0)
        }

    @Test
    @Story("Locked Out User")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should show error for locked out user")
    fun testLockedOutUserLogin() =
        withScreenshotOnFailure {
            val loginPage = LoginPage(page).open()

            loginPage.loginExpectingError(
                username = SauceDemo.LOCKED_OUT_USER,
                password = SauceDemo.PASSWORD,
            )

            assertThat(loginPage.isErrorDisplayed()).isTrue()
            assertThat(loginPage.getErrorMessage())
                .contains("locked out")
        }

    @Test
    @Story("Invalid Credentials")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should show error for invalid username")
    fun testInvalidUsername() =
        withScreenshotOnFailure {
            val loginPage = LoginPage(page).open()

            loginPage.loginExpectingError(
                username = SauceDemo.INVALID_USER,
                password = SauceDemo.PASSWORD,
            )

            assertThat(loginPage.isErrorDisplayed()).isTrue()
            assertThat(loginPage.getErrorMessage())
                .contains("Username and password do not match")
        }

    @Test
    @Story("Invalid Credentials")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should show error for invalid password")
    fun testInvalidPassword() =
        withScreenshotOnFailure {
            val loginPage = LoginPage(page).open()

            loginPage.loginExpectingError(
                username = SauceDemo.STANDARD_USER,
                password = SauceDemo.INVALID_PASSWORD,
            )

            assertThat(loginPage.isErrorDisplayed()).isTrue()
            assertThat(loginPage.getErrorMessage())
                .contains("Username and password do not match")
        }
}
