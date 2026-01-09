package com.nanogpt.chat.data.repository

import com.nanogpt.chat.data.remote.api.WebSearchApi
import com.nanogpt.chat.data.remote.dto.WebSearchRequest
import com.nanogpt.chat.data.remote.dto.WebSearchResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSearchRepository @Inject constructor(
    private val webSearchApi: WebSearchApi
) {
    suspend fun performSearch(
        query: String,
        provider: String,
        deepSearch: Boolean = false
    ): Result<WebSearchResponse> {
        return try {
            val response = webSearchApi.webSearch(
                WebSearchRequest(
                    query = query,
                    provider = provider,
                    deepSearch = deepSearch
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val PROVIDER_LINKUP = "linkup"
        const val PROVIDER_TAVILY = "tavily"
        const val PROVIDER_EXA = "exa"
        const val PROVIDER_KAGI = "kagi"

        val ALL_PROVIDERS = listOf(
            PROVIDER_LINKUP,
            PROVIDER_TAVILY,
            PROVIDER_EXA,
            PROVIDER_KAGI
        )

        val PROVIDER_NAMES = mapOf(
            PROVIDER_LINKUP to "Linkup",
            PROVIDER_TAVILY to "Tavily",
            PROVIDER_EXA to "Exa",
            PROVIDER_KAGI to "Kagi"
        )
    }
}
