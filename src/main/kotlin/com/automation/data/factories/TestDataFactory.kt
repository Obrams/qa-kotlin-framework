package com.automation.data.factories

import com.automation.data.models.Category
import com.automation.data.models.Order
import com.automation.data.models.OrderStatus
import com.automation.data.models.Pet
import com.automation.data.models.PetStatus
import com.automation.data.models.Tag
import com.automation.data.models.User
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

/**
 * Factory for generating test data.
 */
object TestDataFactory {
    private val random = Random(System.currentTimeMillis())

    private val petNames =
        listOf(
            "Buddy", "Max", "Charlie", "Cooper", "Rocky",
            "Bear", "Duke", "Tucker", "Jack", "Toby",
            "Luna", "Bella", "Daisy", "Lucy", "Sadie",
        )

    private val categoryNames =
        listOf(
            "Dogs",
            "Cats",
            "Birds",
            "Fish",
            "Reptiles",
            "Hamsters",
        )

    private val tagNames =
        listOf(
            "friendly", "trained", "vaccinated", "playful", "calm",
            "active", "indoor", "outdoor", "young", "adult",
        )

    private val firstNames =
        listOf(
            "John", "Jane", "Michael", "Sarah", "David",
            "Emily", "Robert", "Emma", "William", "Olivia",
        )

    private val lastNames =
        listOf(
            "Smith", "Johnson", "Williams", "Brown", "Jones",
            "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        )

    fun generateId(): Long = System.currentTimeMillis() + random.nextLong(1000, 9999)

    fun generateUsername(): String = "user_${UUID.randomUUID().toString().take(8)}"

    fun generateEmail(): String = "${generateUsername()}@test.com"

    fun generatePhone(): String = "+1${random.nextLong(1000000000, 9999999999)}"

    fun generatePassword(): String = "Pass${random.nextInt(1000, 9999)}!"

    fun createPet(
        id: Long? = null,
        name: String? = null,
        status: PetStatus = PetStatus.AVAILABLE,
        categoryName: String? = null,
        tags: List<String>? = null,
    ): Pet {
        return Pet(
            id = id,
            name = name ?: petNames.random(),
            category =
                Category(
                    id = random.nextLong(1, 100),
                    name = categoryName ?: categoryNames.random(),
                ),
            photoUrls = listOf("https://example.com/pet${random.nextInt(1, 100)}.jpg"),
            tags =
                (tags ?: listOf(tagNames.random(), tagNames.random())).map {
                    Tag(id = random.nextLong(1, 100), name = it)
                },
            status = status,
        )
    }

    fun createUser(
        id: Long? = null,
        username: String? = null,
        email: String? = null,
        password: String? = null,
    ): User {
        val generatedUsername = username ?: generateUsername()
        return User(
            id = id,
            username = generatedUsername,
            firstName = firstNames.random(),
            lastName = lastNames.random(),
            email = email ?: generateEmail(),
            password = password ?: generatePassword(),
            phone = generatePhone(),
            userStatus = 1,
        )
    }

    fun createOrder(
        id: Long? = null,
        petId: Long? = null,
        quantity: Int = 1,
        status: OrderStatus = OrderStatus.PLACED,
    ): Order {
        return Order(
            id = id,
            petId = petId ?: random.nextLong(1, 1000),
            quantity = quantity,
            shipDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            status = status,
            complete = false,
        )
    }

    fun createPets(
        count: Int,
        status: PetStatus = PetStatus.AVAILABLE,
    ): List<Pet> {
        return (1..count).map { createPet(status = status) }
    }

    fun createUsers(count: Int): List<User> {
        return (1..count).map { createUser() }
    }

    object SauceDemo {
        const val BASE_URL = "https://www.saucedemo.com"

        const val STANDARD_USER = "standard_user"
        const val LOCKED_OUT_USER = "locked_out_user"
        const val PROBLEM_USER = "problem_user"
        const val PERFORMANCE_GLITCH_USER = "performance_glitch_user"
        const val ERROR_USER = "error_user"
        const val VISUAL_USER = "visual_user"
        const val PASSWORD = "secret_sauce"
        const val INVALID_USER = "invalid_user"
        const val INVALID_PASSWORD = "wrong_password"

        val PRODUCTS =
            listOf(
                "Sauce Labs Backpack",
                "Sauce Labs Bike Light",
                "Sauce Labs Bolt T-Shirt",
                "Sauce Labs Fleece Jacket",
                "Sauce Labs Onesie",
                "Test.allTheThings() T-Shirt (Red)",
            )
    }

    object PracticeTestAutomation {
        const val BASE_URL = "https://practicetestautomation.com"
        const val LOGIN_URL = "$BASE_URL/practice-test-login/"
        const val VALID_USERNAME = "student"
        const val VALID_PASSWORD = "Password123"
        const val INVALID_USERNAME = "incorrectUser"
        const val INVALID_PASSWORD = "incorrectPassword"
    }
}
