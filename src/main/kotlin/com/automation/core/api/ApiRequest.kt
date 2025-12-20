package com.automation.core.api

import org.springframework.http.HttpMethod

/**
 * DSL builder for API requests.
 */
class ApiRequestBuilder {
    var method: HttpMethod = HttpMethod.GET
    var path: String = ""
    var body: Any? = null

    private val headers = mutableMapOf<String, String>()
    private val queryParams = mutableMapOf<String, String>()

    fun header(
        name: String,
        value: String,
    ) {
        headers[name] = value
    }

    fun headers(vararg pairs: Pair<String, String>) {
        headers.putAll(pairs)
    }

    fun queryParam(
        name: String,
        value: String,
    ) {
        queryParams[name] = value
    }

    fun queryParams(vararg pairs: Pair<String, String>) {
        queryParams.putAll(pairs)
    }

    fun jsonBody(content: Any) {
        body = content
        header("Content-Type", "application/json")
    }

    fun build(): ApiRequestConfig =
        ApiRequestConfig(
            method = method,
            path = path,
            body = body,
            headers = headers.toMap(),
            queryParams = queryParams.toMap(),
        )
}

data class ApiRequestConfig(
    val method: HttpMethod,
    val path: String,
    val body: Any?,
    val headers: Map<String, String>,
    val queryParams: Map<String, String>,
) {
    /**
     * Returns full path with query parameters.
     */
    fun getFullPath(): String = UrlBuilder.buildPath(path, queryParams)
}

fun apiRequest(block: ApiRequestBuilder.() -> Unit): ApiRequestConfig {
    return ApiRequestBuilder().apply(block).build()
}

fun getRequest(
    path: String,
    block: ApiRequestBuilder.() -> Unit = {},
): ApiRequestConfig {
    return apiRequest {
        this.method = HttpMethod.GET
        this.path = path
        block()
    }
}

fun postRequest(
    path: String,
    body: Any? = null,
    block: ApiRequestBuilder.() -> Unit = {},
): ApiRequestConfig {
    return apiRequest {
        this.method = HttpMethod.POST
        this.path = path
        this.body = body
        block()
    }
}

fun putRequest(
    path: String,
    body: Any? = null,
    block: ApiRequestBuilder.() -> Unit = {},
): ApiRequestConfig {
    return apiRequest {
        this.method = HttpMethod.PUT
        this.path = path
        this.body = body
        block()
    }
}

fun deleteRequest(
    path: String,
    block: ApiRequestBuilder.() -> Unit = {},
): ApiRequestConfig {
    return apiRequest {
        this.method = HttpMethod.DELETE
        this.path = path
        block()
    }
}
