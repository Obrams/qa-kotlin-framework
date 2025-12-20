package com.automation.api

import com.automation.base.BaseApiTest
import com.automation.core.api.ApiResponse
import com.automation.data.factories.TestDataFactory
import com.automation.data.models.Order
import com.automation.data.models.OrderStatus
import io.qameta.allure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Epic("Petstore API")
@Feature("Store Management")
@Tag("api")
@DisplayName("Store API Tests")
class StoreApiTests : BaseApiTest() {
    @Test
    @Story("Get Inventory")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Should get store inventory")
    fun testGetInventory() {
        val response = apiClient.get<Map<String, Int>>("/store/inventory")

        assertThat(response).isInstanceOf(ApiResponse.Success::class.java)
        val inventory = (response as ApiResponse.Success).data
        assertThat(inventory).isNotEmpty()
        Allure.parameter("Inventory Size", inventory.size.toString())
    }

    @Test
    @Story("Create Order")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should create a new order")
    fun testCreateOrder() {
        val order =
            TestDataFactory.createOrder(
                petId = 1,
                quantity = 2,
                status = OrderStatus.PLACED,
            )

        val response = apiClient.post<Order, Order>("/store/order", order)

        assertThat(response).isInstanceOf(ApiResponse.Success::class.java)
        val createdOrder = (response as ApiResponse.Success).data
        assertThat(createdOrder.id).isNotNull()
        assertThat(createdOrder.petId).isEqualTo(1)
        assertThat(createdOrder.quantity).isEqualTo(2)
        assertThat(createdOrder.status).isEqualTo(OrderStatus.PLACED)
    }

    @Test
    @Story("Read Order")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Should get order by ID")
    fun testGetOrderById() {
        val order = TestDataFactory.createOrder(petId = 1)
        val createResponse = apiClient.post<Order, Order>("/store/order", order)
        val orderId = (createResponse as ApiResponse.Success).data.id

        val response = apiClient.get<Order>("/store/order/$orderId")

        assertThat(response).isInstanceOf(ApiResponse.Success::class.java)
        val retrievedOrder = (response as ApiResponse.Success).data
        assertThat(retrievedOrder.id).isEqualTo(orderId)
    }
}
