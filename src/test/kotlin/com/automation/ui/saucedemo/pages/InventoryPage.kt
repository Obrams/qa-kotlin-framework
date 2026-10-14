package com.automation.ui.saucedemo.pages

import com.automation.core.ui.BaseScreen
import com.microsoft.playwright.Page
import io.qameta.allure.Step

class InventoryPage(page: Page) : BaseScreen(page) {
    override val screenName = "Inventory Page"
    override val expectedUrlPattern = "inventory.html"

    private val title = css(".title", "Page title")
    private val shoppingCartLink = css(".shopping_cart_link", "Shopping cart link")
    private val shoppingCartBadge = css(".shopping_cart_badge", "Cart badge")
    private val menuButton = css("#react-burger-menu-btn", "Menu button")
    private val logoutLink = css("#logout_sidebar_link", "Logout link")
    private val inventoryItems = css(".inventory_item", "Inventory items")
    private val sortDropdown = css(".product_sort_container", "Sort dropdown")

    fun getPageTitle(): String {
        return title.waitForVisible().getText()
    }

    override fun isDisplayed(): Boolean {
        // the page is a React app: the URL changes before the header renders, so wait for it
        return getCurrentUrl().contains("inventory") && runCatching { title.waitForVisible() }.isSuccess
    }

    fun getProductCount(): Int {
        return inventoryItems.count()
    }

    fun getProductNames(): List<String> {
        return inventoryItems.all().map { item ->
            item.getLocator().locator(".inventory_item_name").textContent() ?: ""
        }
    }

    @Step("Add product to cart: {productName}")
    fun addToCart(productName: String): InventoryPage {
        val addButton =
            css(
                ".inventory_item:has-text('$productName') button[id^='add-to-cart']",
                "Add to cart button for $productName",
            )
        addButton.click()
        return this
    }

    @Step("Remove product from cart: {productName}")
    fun removeFromCart(productName: String): InventoryPage {
        val removeButton =
            css(
                ".inventory_item:has-text('$productName') button[id^='remove']",
                "Remove button for $productName",
            )
        removeButton.click()
        return this
    }

    fun getCartItemCount(): Int {
        return if (shoppingCartBadge.isVisible()) {
            shoppingCartBadge.getText().toIntOrNull() ?: 0
        } else {
            0
        }
    }

    @Step("Open shopping cart")
    fun openCart(): CartPage {
        shoppingCartLink.click()
        return CartPage(page)
    }

    @Step("Open product details: {productName}")
    fun openProductDetails(productName: String): ProductDetailsPage {
        val productLink =
            css(
                ".inventory_item_name:has-text('$productName')",
                "Product link for $productName",
            )
        productLink.click()
        return ProductDetailsPage(page)
    }

    @Step("Sort products by: {option}")
    fun sortBy(option: String): InventoryPage {
        sortDropdown.selectByValue(option)
        return this
    }

    @Step("Open menu")
    fun openMenu(): InventoryPage {
        menuButton.click()
        return this
    }

    @Step("Logout")
    fun logout(): LoginPage {
        openMenu()
        logoutLink.waitForVisible().click()
        return LoginPage(page)
    }

    fun getProductPrice(productName: String): String {
        val priceElement =
            css(
                ".inventory_item:has-text('$productName') .inventory_item_price",
                "Price for $productName",
            )
        return priceElement.getText()
    }
}
