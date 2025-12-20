package com.automation.data.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long? = null,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val password: String? = null,
    val phone: String? = null,
    val userStatus: Int? = null,
)

@Serializable
data class LoginResponse(
    val code: Int? = null,
    val type: String? = null,
    val message: String? = null,
) {
    fun getSessionToken(): String? {
        return message?.substringAfter("session:")?.trim()
    }
}
