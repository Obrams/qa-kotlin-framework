package com.automation.api

import com.automation.base.BaseApiTest
import com.automation.core.api.ApiResponse
import com.automation.data.factories.TestDataFactory
import com.automation.data.models.Pet
import com.automation.data.models.PetStatus
import io.qameta.allure.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Epic("Petstore API")
@Feature("Pet Management")
@Tag("api")
@DisplayName("Pet API Tests")
class PetApiTests : BaseApiTest() {
    @Test
    @Story("Create Pet")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should create a new pet successfully")
    fun testCreatePet() {
        val pet =
            TestDataFactory.createPet(
                name = "Fluffy",
                status = PetStatus.AVAILABLE,
            )

        val response = apiClient.post<Pet, Pet>("/pet", pet)

        assertThat(response).isInstanceOf(ApiResponse.Success::class.java)
        val createdPet = (response as ApiResponse.Success).data
        assertThat(createdPet.id).isNotNull()
        assertThat(createdPet.name).isEqualTo("Fluffy")
        assertThat(createdPet.status).isEqualTo(PetStatus.AVAILABLE)
    }

    @Test
    @Story("Read Pet")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should get pet by ID")
    fun testGetPetById() {
        val pet = TestDataFactory.createPet(name = "TestPet_${System.currentTimeMillis()}")
        val createResponse = apiClient.post<Pet, Pet>("/pet", pet)
        assertThat(createResponse).isInstanceOf(ApiResponse.Success::class.java)
        val createdPet = (createResponse as ApiResponse.Success).data
        val petId = createdPet.id

        val response = apiClient.get<Pet>("/pet/$petId")

        assertThat(response).isInstanceOf(ApiResponse.Success::class.java)
        val retrievedPet = (response as ApiResponse.Success).data
        assertThat(retrievedPet.id).isEqualTo(petId)
        // Имя может измениться при параллельном выполнении тестов на публичном API
        assertThat(retrievedPet.name).isNotBlank()
    }

    @Test
    @Story("Read Pet")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Should return 404 for non-existent pet")
    fun testGetNonExistentPet() {
        val response = apiClient.get<Pet>("/pet/999999999999")

        assertThat(response).isInstanceOf(ApiResponse.HttpError::class.java)
        val error = response as ApiResponse.HttpError
        assertThat(error.statusCode).isEqualTo(404)
    }

    @Test
    @Story("Update Pet")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Should update existing pet")
    fun testUpdatePet() {
        val originalPet = TestDataFactory.createPet(name = "Original")
        val createResponse = apiClient.post<Pet, Pet>("/pet", originalPet)
        val createdPet = (createResponse as ApiResponse.Success).data
        val updatedPet =
            createdPet.copy(
                name = "Updated",
                status = PetStatus.SOLD,
            )

        val updateResponse = apiClient.put<Pet, Pet>("/pet", updatedPet)

        assertThat(updateResponse).isInstanceOf(ApiResponse.Success::class.java)
        val result = (updateResponse as ApiResponse.Success).data
        assertThat(result.name).isEqualTo("Updated")
        assertThat(result.status).isEqualTo(PetStatus.SOLD)
    }

    @Test
    @Story("Search Pets")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Should find pets by status")
    fun testFindPetsByStatus() {
        val response =
            apiClient.get<List<Pet>>(
                path = "/pet/findByStatus",
                queryParams = mapOf("status" to "available"),
            )

        assertThat(response).isInstanceOf(ApiResponse.Success::class.java)
        val pets = (response as ApiResponse.Success).data
        assertThat(pets).isNotEmpty()
        assertThat(pets).allMatch { it.status == PetStatus.AVAILABLE }
    }
}
