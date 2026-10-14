package com.automation.ui.saucedemo.pages

import com.automation.core.ui.BaseScreen
import com.microsoft.playwright.Page
import io.qameta.allure.Step

class CartPage(page: Page) : BaseScreen(page) {
    override val screenName = "Cart Page"
    override val expectedUrlPattern = "cart.html"

    private val title = css(".title", "Page title")
    private val cartList = css(".cart_list", "Cart list")
    private val cartItems = css(".cart_item", "Cart items")
    private val continueShoppingButton = css("#continue-shopping", "Continue shopping button")
    private val checkoutButton = css("#checkout", "Checkout button")
    private val removeButtons = css("button[id^='remove']", "Remove buttons")

    fun getPageTitle(): String {
        return title.waitForVisible().getText()
    }

    override fun isDisplayed(): Boolean {
        return title.isVisible() && getCurrentUrl().contains("cart")
    }

    fun getCartItemCount(): Int {
        waitUntilLoaded()
        return cartItems.count()
    }

    fun getCartItemNames(): List<String> {
        waitUntilLoaded()
        return cartItems.all().map { item ->
            item.getLocator().locator(".inventory_item_name").textContent() ?: ""
        }
    }

    @Step("Remove item from cart: {itemName}")
    fun removeItem(itemName: String): CartPage {
        val removeButton =
            css(
                ".cart_item:has-text('$itemName') button[id^='remove']",
                "Remove button for $itemName",
            )
        removeButton.click()
        return this
    }

    @Step("Continue shopping")
    fun continueShopping(): InventoryPage {
        continueShoppingButton.click()
        return InventoryPage(page)
    }

    @Step("Proceed to checkout")
    fun checkout(): CheckoutPage {
        checkoutButton.click()
        return CheckoutPage(page)
    }

    fun isCartEmpty(): Boolean {
        waitUntilLoaded()
        return cartItems.count() == 0
    }

    // the cart is client-side rendered and count() does not wait; .cart_list exists even for an empty cart
    private fun waitUntilLoaded() {
        cartList.waitForVisible()
    }

    fun getItemPrice(itemName: String): String {
        val priceElement =
            css(
                ".cart_item:has-text('$itemName') .inventory_item_price",
                "Price for $itemName",
            )
        return priceElement.getText()
    }
}
