package com.automation.core.config

/**
 * Centralized configuration for test framework.
 * Values can be overridden via environment variables.
 */
object TestConfig {
    // API Configuration
    val PETSTORE_BASE_URL: String =
        System.getenv("PETSTORE_URL")
            ?: "https://petstore.swagger.io/v2"

    // Timeouts
    val DEFAULT_TIMEOUT_MS: Long =
        System.getenv("DEFAULT_TIMEOUT_MS")?.toLongOrNull()
            ?: 10000L

    val API_TIMEOUT_SECONDS: Long =
        System.getenv("API_TIMEOUT_SECONDS")?.toLongOrNull()
            ?: 30L

    val API_MAX_RETRIES: Long =
        System.getenv("API_MAX_RETRIES")?.toLongOrNull()
            ?: 3L

    // Browser Configuration
    val BROWSER_HEADLESS: Boolean =
        System.getenv("CI")?.toBoolean()
            ?: System.getenv("HEADLESS")?.toBoolean()
            ?: true

    val VIEWPORT_WIDTH: Int =
        System.getenv("VIEWPORT_WIDTH")?.toIntOrNull()
            ?: 1920

    val VIEWPORT_HEIGHT: Int =
        System.getenv("VIEWPORT_HEIGHT")?.toIntOrNull()
            ?: 1080

    // Parallel Execution
    val PARALLEL_THREADS: Int =
        System.getenv("PARALLEL_THREADS")?.toIntOrNull()
            ?: Runtime.getRuntime().availableProcessors()
}
