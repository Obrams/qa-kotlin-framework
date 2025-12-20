package com.automation.ui.saucedemo.pages

import com.automation.core.ui.BaseScreen
import com.microsoft.playwright.Page
import io.qameta.allure.Step

class CheckoutPage(page: Page) : BaseScreen(page) {
    override val screenName = "Checkout Page"
    override val expectedUrlPattern = "checkout"

    private val firstNameInput = css("#first-name", "First name input")
    private val lastNameInput = css("#last-name", "Last name input")
    private val postalCodeInput = css("#postal-code", "Postal code input")
    private val continueButton = css("#continue", "Continue button")
    private val cancelButton = css("#cancel", "Cancel button")
    private val errorMessage = css("[data-test='error']", "Error message")
    private val finishButton = css("#finish", "Finish button")
    private val summaryTotal = css(".summary_total_label", "Total price")
    private val summarySubtotal = css(".summary_subtotal_label", "Subtotal")
    private val summaryTax = css(".summary_tax_label", "Tax")
    private val completeHeader = css(".complete-header", "Complete header")
    private val backHomeButton = css("#back-to-products", "Back home button")

    @Step("Fill checkout info: {firstName} {lastName}, {postalCode}")
    fun fillInfo(
        firstName: String,
        lastName: String,
        postalCode: String,
    ): CheckoutPage {
        firstNameInput.clearAndType(firstName)
        lastNameInput.clearAndType(lastName)
        postalCodeInput.clearAndType(postalCode)
        return this
    }

    @Step("Continue to next step")
    fun continueCheckout(): CheckoutPage {
        continueButton.click()
        return this
    }

    @Step("Finish checkout")
    fun finish(): CheckoutPage {
        finishButton.click()
        return this
    }

    fun getErrorMessage(): String {
        return errorMessage.waitForVisible().getText()
    }

    fun isErrorDisplayed(): Boolean {
        return errorMessage.isVisible()
    }

    fun getTotalPrice(): String {
        return summaryTotal.getText()
    }

    fun getSubtotal(): String {
        return summarySubtotal.getText()
    }

    fun getTax(): String {
        return summaryTax.getText()
    }

    fun isCheckoutComplete(): Boolean {
        return completeHeader.isVisible()
    }

    fun getCompletionMessage(): String {
        return completeHeader.getText()
    }

    @Step("Back to products")
    fun backToProducts(): InventoryPage {
        backHomeButton.click()
        return InventoryPage(page)
    }

    @Step("Cancel checkout")
    fun cancel(): CartPage {
        cancelButton.click()
        return CartPage(page)
    }
}
