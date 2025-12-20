package com.automation.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pet(
    val id: Long? = null,
    val name: String? = null,
    val category: Category? = null,
    val photoUrls: List<String> = emptyList(),
    val tags: List<Tag>? = null,
    val status: PetStatus? = null,
)

@Serializable
data class Category(
    val id: Long? = null,
    val name: String? = null,
)

@Serializable
data class Tag(
    val id: Long? = null,
    val name: String? = null,
)

@Serializable
enum class PetStatus {
    @SerialName("available")
    AVAILABLE,

    @SerialName("pending")
    PENDING,

    @SerialName("sold")
    SOLD,
}

@Serializable
data class ApiMessageResponse(
    val code: Int? = null,
    val type: String? = null,
    val message: String? = null,
)
