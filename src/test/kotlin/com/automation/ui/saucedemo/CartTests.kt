package com.automation.ui.saucedemo

import com.automation.base.BaseUiTest
import com.automation.data.factories.TestDataFactory.SauceDemo
import com.automation.ui.saucedemo.pages.InventoryPage
import com.automation.ui.saucedemo.pages.LoginPage
import io.qameta.allure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@Epic("SauceDemo")
@Feature("Shopping Cart")
@Tag("ui")
@Tag("saucedemo")
@DisplayName("SauceDemo Cart Tests")
class CartTests : BaseUiTest() {
    @BeforeEach
    override fun setUp(testInfo: TestInfo) {
        super.setUp(testInfo)
        LoginPage(page)
            .open()
            .login(SauceDemo.STANDARD_USER, SauceDemo.PASSWORD)
    }

    @Test
    @Story("Add to Cart")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should add product to cart")
    fun testAddProductToCart() =
        withScreenshotOnFailure {
            val inventoryPage = InventoryPage(page)
            val productName = SauceDemo.PRODUCTS.first()

            inventoryPage.addToCart(productName)

            assertThat(inventoryPage.getCartItemCount()).isEqualTo(1)

            val cartPage = inventoryPage.openCart()
            assertThat(cartPage.getCartItemNames()).contains(productName)
        }

    @Test
    @Story("Remove from Cart")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should remove product from cart")
    fun testRemoveProductFromCart() =
        withScreenshotOnFailure {
            val inventoryPage = InventoryPage(page)
            val productName = SauceDemo.PRODUCTS.first()
            inventoryPage.addToCart(productName)

            inventoryPage.removeFromCart(productName)

            assertThat(inventoryPage.getCartItemCount()).isEqualTo(0)
        }

    @Test
    @Story("Checkout Flow")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Should complete checkout successfully")
    fun testCompleteCheckout() =
        withScreenshotOnFailure {
            val inventoryPage = InventoryPage(page)
            inventoryPage.addToCart(SauceDemo.PRODUCTS[0])
            inventoryPage.addToCart(SauceDemo.PRODUCTS[1])

            val cartPage = inventoryPage.openCart()
            val checkoutPage = cartPage.checkout()

            checkoutPage
                .fillInfo("John", "Doe", "12345")
                .continueCheckout()

            assertThat(checkoutPage.getTotalPrice()).isNotEmpty()

            checkoutPage.finish()

            assertThat(checkoutPage.isCheckoutComplete()).isTrue()
            assertThat(checkoutPage.getCompletionMessage())
                .containsIgnoringCase("thank you")
        }
}
