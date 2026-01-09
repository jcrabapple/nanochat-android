package com.nanogpt.chat.data.remote.api

import com.nanogpt.chat.data.remote.dto.WebSearchRequest
import com.nanogpt.chat.data.remote.dto.WebSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface WebSearchApi {
    @POST("api/web/search")
    suspend fun webSearch(@Body request: WebSearchRequest): Response<WebSearchResponse>
}
