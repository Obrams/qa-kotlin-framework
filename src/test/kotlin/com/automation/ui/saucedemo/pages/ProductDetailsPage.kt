package com.automation.ui.saucedemo.pages

import com.automation.core.ui.BaseScreen
import com.microsoft.playwright.Page
import io.qameta.allure.Step

class ProductDetailsPage(page: Page) : BaseScreen(page) {
    override val screenName = "Product Details Page"
    override val expectedUrlPattern = "inventory-item.html"

    private val productName = css(".inventory_details_name", "Product name")
    private val productDescription = css(".inventory_details_desc", "Product description")
    private val productPrice = css(".inventory_details_price", "Product price")
    private val addToCartButton = css("button[id^='add-to-cart']", "Add to cart button")
    private val removeButton = css("button[id^='remove']", "Remove button")
    private val backButton = css("#back-to-products", "Back button")

    fun getProductName(): String {
        return productName.waitForVisible().getText()
    }

    fun getProductDescription(): String {
        return productDescription.getText()
    }

    fun getProductPrice(): String {
        return productPrice.getText()
    }

    @Step("Add to cart")
    fun addToCart(): ProductDetailsPage {
        addToCartButton.click()
        return this
    }

    @Step("Remove from cart")
    fun removeFromCart(): ProductDetailsPage {
        removeButton.click()
        return this
    }

    fun isInCart(): Boolean {
        return removeButton.isVisible()
    }

    @Step("Back to products")
    fun backToProducts(): InventoryPage {
        backButton.click()
        return InventoryPage(page)
    }
}
