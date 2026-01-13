package com.automation.core.api

sealed class ApiResponse<out T> {
    data class Success<T>(
        val data: T,
        val statusCode: Int = 200,
        val headers: Map<String, String> = emptyMap(),
        val requestDuration: Long = 0,
    ) : ApiResponse<T>()

    data class HttpError(
        val statusCode: Int,
        val message: String,
        val body: String? = null,
        val headers: Map<String, String> = emptyMap(),
    ) : ApiResponse<Nothing>()

    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error",
    ) : ApiResponse<Nothing>()

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T =
        when (this) {
            is Success -> data
            is HttpError -> throw ApiException("HTTP Error $statusCode: $message", statusCode)
            is Error -> throw exception
        }

    inline fun <R> map(transform: (T) -> R): ApiResponse<R> =
        when (this) {
            is Success -> Success(transform(data), statusCode, headers, requestDuration)
            is HttpError -> this
            is Error -> this
        }

    inline fun onSuccess(action: (T) -> Unit): ApiResponse<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String) -> Unit): ApiResponse<T> {
        when (this) {
            is HttpError -> action("HTTP $statusCode: $message")
            is Error -> action(message)
            is Success -> { /* no-op */ }
        }
        return this
    }
}

class ApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
