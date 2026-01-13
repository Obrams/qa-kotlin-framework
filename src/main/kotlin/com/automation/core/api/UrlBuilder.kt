package com.automation.core.api

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UrlBuilder {
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

