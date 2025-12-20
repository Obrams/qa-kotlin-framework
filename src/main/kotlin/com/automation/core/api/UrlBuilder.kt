package com.automation.core.api

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Utility object for building URLs with query parameters.
 */
object UrlBuilder {
    /**
     * Builds a path with query parameters.
     *
     * @param path the base path
     * @param queryParams map of query parameters
     * @param encode whether to URL-encode parameter values
     * @return the full path with query string
     */
    fun buildPath(
        path: String,
        queryParams: Map<String, String>,
        encode: Boolean = false,
    ): String {
        if (queryParams.isEmpty()) return path

        val query =
            queryParams.entries.joinToString("&") { (key, value) ->
                if (encode) {
                    "${URLEncoder.encode(
                        key,
                        StandardCharsets.UTF_8,
                    )}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
                } else {
                    "$key=$value"
                }
            }

        val separator = if (path.contains("?")) "&" else "?"
        return "$path$separator$query"
    }
}
