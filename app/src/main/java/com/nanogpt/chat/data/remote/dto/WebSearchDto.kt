package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WebSearchRequest(
    val query: String,
    val provider: String, // "linkup" | "tavily" | "exa" | "kagi"
    val deepSearch: Boolean = false
)

@Serializable
data class WebSearchResponse(
    val query: String,
    val provider: String,
    val deepSearch: Boolean,
    val sources: List<WebSearchResultDto>
)

@Serializable
data class WebSearchResultDto(
    val title: String,
    val url: String,
    val snippet: String,
    val score: Float? = null,
    val publishedDate: String? = null
)
