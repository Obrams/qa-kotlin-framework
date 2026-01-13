package com.automation.integration

import com.automation.core.api.ApiClient
import com.automation.core.api.ApiResponse
import com.automation.core.config.TestConfig
import com.automation.core.threading.TestContext
import com.automation.core.threading.ThreadSafeReportCollector
import com.automation.data.factories.TestDataFactory
import com.automation.data.models.Pet
import com.automation.data.models.PetStatus
import io.qameta.allure.*
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

private val logger = KotlinLogging.logger {}

@Epic("End-to-End")
@Feature("Integration Tests")
@Tag("e2e")
@DisplayName("E2E Integration Tests")
class E2EFlowTests {
    private lateinit var apiClient: ApiClient
    private lateinit var reportCollector: ThreadSafeReportCollector

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        val testName = testInfo.displayName
        TestContext.initContext(testName, "e2e-test-user")

        reportCollector = ThreadSafeReportCollector()
        apiClient =
            ApiClient(
                baseUrl = TestConfig.PETSTORE_BASE_URL,
                reportCollector = reportCollector,
            )

        logger.info { "Starting E2E test: $testName" }
        Allure.parameter("Base URL", TestConfig.PETSTORE_BASE_URL)
    }

    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        try {
            reportCollector.generateAllureAttachments()
        } finally {
            TestContext.cleanup()
            logger.info { "E2E test completed: ${testInfo.displayName}" }
        }
    }

    @Test
    @Story("API Integration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E: Create and retrieve pet via API")
    fun testApiCreateAndRetrievePet() {
        val pet =
            TestDataFactory.createPet(
                name = "E2ETestPet_${System.currentTimeMillis()}",
                status = PetStatus.AVAILABLE,
            )

        val createResponse = apiClient.post<Pet, Pet>("/pet", pet)

        assertThat(createResponse).isInstanceOf(ApiResponse.Success::class.java)
        val createdPet = (createResponse as ApiResponse.Success).data
        assertThat(createdPet.id).isNotNull()

        TestContext.addMetadata("createdPetId", createdPet.id ?: 0L)
        TestContext.addMetadata("createdPetName", createdPet.name ?: "")

        Allure.parameter("Pet ID", createdPet.id.toString())
        Allure.parameter("Pet Name", createdPet.name ?: "")

        val petId = createdPet.id!!
        val getResponse = apiClient.get<Pet>("/pet/$petId")

        assertThat(getResponse).isInstanceOf(ApiResponse.Success::class.java)
        val retrievedPet = (getResponse as ApiResponse.Success).data
        assertThat(retrievedPet.id).isEqualTo(petId)
        assertThat(retrievedPet.name).isNotBlank()

        logger.info { "Successfully created and retrieved pet: ${retrievedPet.name}" }
    }

    @Test
    @Story("API Workflow")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E: Full pet lifecycle - create, update, delete")
    fun testFullPetLifecycle() {
        val pet =
            TestDataFactory.createPet(
                name = "LifecycleTestPet_${System.currentTimeMillis()}",
                status = PetStatus.AVAILABLE,
            )

        val createResponse = apiClient.post<Pet, Pet>("/pet", pet)
        assertThat(createResponse).isInstanceOf(ApiResponse.Success::class.java)
        val createdPet = (createResponse as ApiResponse.Success).data
        val petId = createdPet.id!!

        Allure.step("Created pet with ID: $petId")

        val updatedPet =
            createdPet.copy(
                name = "UpdatedLifecyclePet",
                status = PetStatus.SOLD,
            )

        val updateResponse = apiClient.put<Pet, Pet>("/pet", updatedPet)
        assertThat(updateResponse).isInstanceOf(ApiResponse.Success::class.java)
        val resultPet = (updateResponse as ApiResponse.Success).data
        assertThat(resultPet.name).isEqualTo("UpdatedLifecyclePet")
        assertThat(resultPet.status).isEqualTo(PetStatus.SOLD)

        Allure.step("Updated pet name and status")

        val getResponse = apiClient.get<Pet>("/pet/$petId")
        assertThat(getResponse).isInstanceOf(ApiResponse.Success::class.java)
        val verifiedPet = (getResponse as ApiResponse.Success).data
        assertThat(verifiedPet.id).isEqualTo(petId)

        Allure.step("Verified pet update via GET")

        val searchResponse =
            apiClient.get<List<Pet>>(
                path = "/pet/findByStatus",
                queryParams = mapOf("status" to "sold"),
            )
        assertThat(searchResponse).isInstanceOf(ApiResponse.Success::class.java)
        val soldPets = (searchResponse as ApiResponse.Success).data
        assertThat(soldPets).isNotEmpty()

        Allure.step("Found ${soldPets.size} sold pets")

        Allure.addAttachment(
            "E2E Test Summary",
            "text/plain",
            """
            Pet Lifecycle Test Results:
            - Created pet ID: $petId
            - Original name: ${pet.name}
            - Updated name: UpdatedLifecyclePet
            - Status changed: available -> sold
            - Total API Calls: ${reportCollector.getStatistics().totalRequests}
            """.trimIndent(),
        )

        logger.info { "Full pet lifecycle test completed successfully" }
    }
}
