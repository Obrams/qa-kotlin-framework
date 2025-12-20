package com.automation.core.api

import com.automation.core.config.TestConfig
import com.automation.core.threading.TestContext
import com.automation.core.threading.ThreadSafeReportCollector
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.util.UUID

/**
 * HTTP API client with retry support and metrics collection.
 */
class ApiClient(
    @PublishedApi internal val baseUrl: String,
    @PublishedApi internal val reportCollector: ThreadSafeReportCollector = ThreadSafeReportCollector(),
    private val defaultHeaders: Map<String, String> = emptyMap(),
    @PublishedApi internal val timeoutSeconds: Long = TestConfig.API_TIMEOUT_SECONDS,
    @PublishedApi internal val maxRetries: Long = TestConfig.API_MAX_RETRIES,
) {
    @PublishedApi
    internal val webClient: WebClient =
        WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders { headers ->
                defaultHeaders.forEach { (key, value) -> headers.add(key, value) }
            }
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { configurer ->
                        // Настраиваем кодеки для корректной работы со строками
                        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                    }
                    .build(),
            )
            .build()

    @PublishedApi
    internal val json =
        Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @PublishedApi
    internal val logger = KotlinLogging.logger {}

    inline fun <reified T> get(
        path: String,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
    ): ApiResponse<T> {
        return execute(
            method = HttpMethod.GET,
            path = UrlBuilder.buildPath(path, queryParams),
            headers = headers,
            body = null,
        )
    }

    inline fun <reified T, reified R> post(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse<R> {
        // Сериализуем тело здесь, где у нас есть reified тип T
        val bodyJson = json.encodeToString(body)
        return executeWithBody(
            method = HttpMethod.POST,
            path = path,
            headers = headers,
            bodyJson = bodyJson,
        )
    }

    inline fun <reified T, reified R> put(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse<R> {
        // Сериализуем тело здесь, где у нас есть reified тип T
        val bodyJson = json.encodeToString(body)
        return executeWithBody(
            method = HttpMethod.PUT,
            path = path,
            headers = headers,
            bodyJson = bodyJson,
        )
    }

    inline fun <reified T> delete(
        path: String,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse<T> {
        return execute(
            method = HttpMethod.DELETE,
            path = path,
            headers = headers,
            body = null,
        )
    }

    inline fun <reified T> execute(config: ApiRequestConfig): ApiResponse<T> {
        return execute(
            method = config.method,
            path = config.getFullPath(),
            headers = config.headers,
            body = config.body,
        )
    }

    inline fun <reified T> execute(
        method: HttpMethod,
        path: String,
        headers: Map<String, String>,
        body: Any?,
    ): ApiResponse<T> {
        // Для обратной совместимости с методами без тела
        return executeWithBody(method, path, headers, null)
    }

    @PublishedApi
    internal inline fun <reified T> executeWithBody(
        method: HttpMethod,
        path: String,
        headers: Map<String, String>,
        bodyJson: String?,
    ): ApiResponse<T> {
        val requestId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        val fullUrl = "$baseUrl$path"

        if (TestContext.isInitialized()) {
            TestContext.trackRequestId(requestId)
        }

        logger.debug { "[$requestId] $method $fullUrl" }
        if (bodyJson != null) {
            logger.debug { "[$requestId] Request body: $bodyJson" }
        }

        return try {
            val response = executeRequest<T>(method, path, headers, bodyJson, startTime)
            val duration = System.currentTimeMillis() - startTime

            when (response) {
                is ApiResponse.Success -> {
                    reportCollector.recordRequest(
                        requestId = requestId,
                        method = method.name(),
                        path = path,
                        statusCode = response.statusCode,
                        duration = duration,
                        requestBody = bodyJson,
                        responseBody = null,
                    )
                    logger.debug { "[$requestId] Completed: ${response.statusCode} (${duration}ms)" }
                }
                is ApiResponse.HttpError -> {
                    reportCollector.recordRequest(
                        requestId = requestId,
                        method = method.name(),
                        path = path,
                        statusCode = response.statusCode,
                        duration = duration,
                        requestBody = bodyJson,
                        responseBody = response.body,
                    )
                    logger.warn { "[$requestId] HTTP Error: ${response.statusCode} - ${response.message}" }
                }
                is ApiResponse.Error -> {
                    reportCollector.recordError(requestId, response.exception, method.name(), path)
                    logger.error(response.exception) { "[$requestId] Error: ${response.message}" }
                }
            }

            response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            reportCollector.recordError(requestId, e, method.name(), path)
            logger.error(e) { "[$requestId] Failed after ${duration}ms: ${e.message}" }
            ApiResponse.Error(e)
        }
    }

    @PublishedApi
    internal inline fun <reified T> executeRequest(
        method: HttpMethod,
        path: String,
        headers: Map<String, String>,
        bodyJson: String?,
        startTime: Long,
    ): ApiResponse<T> {
        val spec =
            webClient.method(method)
                .uri(path)
                .headers { h ->
                    headers.forEach { (k, v) -> h.add(k, v) }
                }
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)

        // Используем кастомный BodyInserter чтобы отправить JSON-строку без повторной сериализации
        val requestSpec =
            if (bodyJson != null) {
                spec.body(rawJsonBodyInserter(bodyJson))
            } else {
                spec
            }

        return try {
            val result =
                requestSpec
                    .exchangeToMono { response -> handleResponse<T>(response, startTime) }
                    .retryWhen(
                        Retry.backoff(maxRetries, Duration.ofMillis(500))
                            .filter { it is WebClientResponseException && isRetryable(it.statusCode) }
                            .doBeforeRetry { logger.info { "Retrying request, attempt ${it.totalRetries() + 1}" } },
                    )
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block()

            result ?: ApiResponse.Error(IllegalStateException("Null response received"))
        } catch (e: WebClientResponseException) {
            ApiResponse.HttpError(
                statusCode = e.statusCode.value(),
                message = e.statusText,
                body = e.responseBodyAsString,
                headers = e.headers.toSingleValueMap(),
            )
        } catch (e: Exception) {
            ApiResponse.Error(e)
        }
    }

    @PublishedApi
    internal inline fun <reified T> handleResponse(
        response: ClientResponse,
        startTime: Long,
    ): Mono<ApiResponse<T>> {
        val statusCode = response.statusCode().value()
        val headers = response.headers().asHttpHeaders().toSingleValueMap()
        val requestDuration = System.currentTimeMillis() - startTime

        return if (response.statusCode().is2xxSuccessful) {
            response.bodyToMono(String::class.java)
                .defaultIfEmpty("")
                .map { body ->
                    val data: T =
                        if (body.isBlank() && T::class == Unit::class) {
                            @Suppress("UNCHECKED_CAST")
                            Unit as T
                        } else {
                            json.decodeFromString<T>(body)
                        }
                    ApiResponse.Success(data, statusCode, headers, requestDuration)
                }
        } else {
            response.bodyToMono(String::class.java)
                .defaultIfEmpty("")
                .map { body ->
                    ApiResponse.HttpError(
                        statusCode = statusCode,
                        message = response.statusCode().toString(),
                        body = body,
                        headers = headers,
                    )
                }
        }
    }

    @PublishedApi
    internal fun isRetryable(status: HttpStatusCode): Boolean {
        return status.value() in RETRYABLE_STATUS_CODES
    }

    /**
     * Создаёт BodyInserter для отправки сырой JSON строки без повторной сериализации Jackson.
     */
    @PublishedApi
    internal fun rawJsonBodyInserter(jsonString: String): BodyInserter<DataBuffer, ReactiveHttpOutputMessage> {
        return BodyInserter { outputMessage, _ ->
            val factory: DataBufferFactory = outputMessage.bufferFactory()
            val buffer = factory.wrap(jsonString.toByteArray(Charsets.UTF_8))
            outputMessage.writeWith(Mono.just(buffer))
        }
    }

    companion object {
        private val RETRYABLE_STATUS_CODES = setOf(408, 429, 500, 502, 503, 504)
    }
}
