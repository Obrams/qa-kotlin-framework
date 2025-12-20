package com.automation.core.reporting

import io.qameta.allure.Allure
import io.qameta.allure.model.Status
import io.qameta.allure.model.StepResult
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Allure reporting extensions and utilities.
 */
object AllureExtensions {
    /**
     * Creates an Allure step with automatic status handling.
     */
    fun <T> step(
        name: String,
        action: () -> T,
    ): T {
        val uuid = UUID.randomUUID().toString()
        Allure.getLifecycle().startStep(uuid, StepResult().setName(name))

        return try {
            val result = action()
            Allure.getLifecycle().updateStep(uuid) { it.status = Status.PASSED }
            result
        } catch (e: AssertionError) {
            Allure.getLifecycle().updateStep(uuid) { it.status = Status.FAILED }
            throw e
        } catch (e: Exception) {
            Allure.getLifecycle().updateStep(uuid) { it.status = Status.BROKEN }
            throw e
        } finally {
            Allure.getLifecycle().stopStep(uuid)
        }
    }

    /**
     * Adds text attachment to Allure report.
     */
    fun attachText(
        name: String,
        content: String,
    ) {
        Allure.addAttachment(name, "text/plain", content)
        logger.trace { "Attached text: $name" }
    }

    /**
     * Adds JSON attachment to Allure report.
     */
    fun attachJson(
        name: String,
        content: String,
    ) {
        Allure.addAttachment(name, "application/json", content, ".json")
        logger.trace { "Attached JSON: $name" }
    }

    /**
     * Adds HTML attachment to Allure report.
     */
    fun attachHtml(
        name: String,
        content: String,
    ) {
        Allure.addAttachment(name, "text/html", content, ".html")
        logger.trace { "Attached HTML: $name" }
    }

    /**
     * Adds screenshot attachment.
     */
    fun attachScreenshot(
        name: String,
        screenshot: ByteArray,
    ) {
        Allure.addAttachment(name, "image/png", screenshot.inputStream(), ".png")
        logger.trace { "Attached screenshot: $name" }
    }

    /**
     * Adds video attachment.
     */
    fun attachVideo(
        name: String,
        video: ByteArray,
    ) {
        Allure.addAttachment(name, "video/webm", video.inputStream(), ".webm")
        logger.trace { "Attached video: $name" }
    }

    /**
     * Adds a link to the report.
     */
    fun addLink(
        name: String,
        url: String,
        type: String = "custom",
    ) {
        Allure.link(name, type, url)
    }

    /**
     * Adds issue link.
     */
    fun addIssue(issueId: String) {
        Allure.issue(issueId, issueId)
    }

    /**
     * Adds TMS link.
     */
    fun addTestCase(testCaseId: String) {
        Allure.tms(testCaseId, testCaseId)
    }

    /**
     * Sets test description.
     */
    fun setDescription(description: String) {
        Allure.description(description)
    }

    /**
     * Sets test description in HTML format.
     */
    fun setDescriptionHtml(html: String) {
        Allure.descriptionHtml(html)
    }

    /**
     * Adds environment info to report.
     */
    fun addParameter(
        name: String,
        value: String,
    ) {
        Allure.parameter(name, value)
    }

    /**
     * Adds multiple parameters.
     */
    fun addParameters(vararg params: Pair<String, String>) {
        params.forEach { (name, value) -> Allure.parameter(name, value) }
    }
}

/**
 * Extension function for executing block as Allure step.
 */
fun <T> allureStep(
    name: String,
    block: () -> T,
): T {
    return AllureExtensions.step(name, block)
}

/**
 * Extension function for attaching data on failure.
 */
fun <T> withScreenshotOnFailure(
    screenshotProvider: () -> ByteArray,
    block: () -> T,
): T {
    return try {
        block()
    } catch (e: Throwable) {
        try {
            AllureExtensions.attachScreenshot("Failure Screenshot", screenshotProvider())
        } catch (screenshotError: Exception) {
            logger.warn { "Failed to capture screenshot: ${screenshotError.message}" }
        }
        throw e
    }
}
