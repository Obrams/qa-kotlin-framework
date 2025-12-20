package com.automation.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Long? = null,
    val petId: Long? = null,
    val quantity: Int = 1,
    val shipDate: String? = null,
    val status: OrderStatus? = null,
    val complete: Boolean = false,
)

@Serializable
enum class OrderStatus {
    @SerialName("placed")
    PLACED,

    @SerialName("approved")
    APPROVED,

    @SerialName("delivered")
    DELIVERED,
}

@Serializable
data class Inventory(
    val available: Int? = null,
    val pending: Int? = null,
    val sold: Int? = null,
)
