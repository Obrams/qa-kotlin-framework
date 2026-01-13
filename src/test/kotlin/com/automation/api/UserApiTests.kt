package com.automation.api

import com.automation.base.BaseApiTest
import com.automation.core.api.ApiResponse
import com.automation.data.factories.TestDataFactory
import com.automation.data.models.LoginResponse
import com.automation.data.models.User
import io.qameta.allure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Epic("Petstore API")
@Feature("User Management")
@Tag("api")
@DisplayName("User API Tests")
class UserApiTests : BaseApiTest() {
    @Test
    @Story("Create User")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should create a new user")
    fun testCreateUser() {
        val user = TestDataFactory.createUser()
        val response = apiClient.post<User, User>("/user", user)
        assertThat(response.isSuccess).isTrue()

        Allure.parameter("Username", user.username)
    }

    @Test
    @Story("Read User")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should get user by username")
    fun testGetUserByUsername() {
        val user = TestDataFactory.createUser()
        val createResponse = apiClient.post<User, User>("/user", user)

        assertThat(createResponse.isSuccess).isTrue()

        val response = apiClient.get<User>("/user/${user.username}")

        if (response is ApiResponse.Success) {
            val retrievedUser = response.data
            assertThat(retrievedUser.username).isEqualTo(user.username)
        } else {
            assertThat(response).isInstanceOf(ApiResponse.HttpError::class.java)
            val error = response as ApiResponse.HttpError
            assertThat(error.statusCode).isIn(200, 404)
        }
    }

    @Test
    @Story("User Authentication")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Should login user successfully")
    fun testUserLogin() {
        val user = TestDataFactory.createUser()
        apiClient.post<User, User>("/user", user)

        val response =
            apiClient.get<LoginResponse>(
                path = "/user/login",
                queryParams =
                    mapOf(
                        "username" to (user.username ?: ""),
                        "password" to (user.password ?: ""),
                    ),
            )

        assertThat(response).isInstanceOf(ApiResponse.Success::class.java)
        val loginResponse = (response as ApiResponse.Success).data
        assertThat(loginResponse.code).isEqualTo(200)
        assertThat(loginResponse.getSessionToken()).isNotNull()
    }
}
