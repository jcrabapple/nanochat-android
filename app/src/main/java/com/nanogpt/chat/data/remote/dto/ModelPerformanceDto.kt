package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ModelPerformanceResponseDto(
    val success: Boolean,
    val stats: List<ModelPerformanceStatsDto>
)

@Serializable
data class ModelPerformanceStatsDto(
    val id: String? = null,
    val userId: String? = null,
    val modelId: String,
    val provider: String? = null,
    val totalMessages: Int,
    val avgRating: Double?,
    val thumbsUpCount: Int,
    val thumbsDownCount: Int,
    val regenerateCount: Int,
    val avgResponseTime: Double,
    val avgTokens: Double,
    val totalCost: Double,
    val errorCount: Int,
    val accurateCount: Int,
    val helpfulCount: Int,
    val creativeCount: Int,
    val fastCount: Int,
    val costEffectiveCount: Int,
    @SerialName("lastUpdated")
    val lastUpdated: String? = null
)

@Serializable
data class OverallStatsDto(
    val totalMessages: Int,
    val totalCost: Double,
    val avgRating: Double?,
    @SerialName("mostUsedModel")
    val mostUsedModel: String?,  // Will be a model ID
    @SerialName("bestRatedModel")
    val bestRatedModel: String?,  // Will be a model ID
    @SerialName("mostCostEffective")
    val mostCostEffective: String?,  // Will be a model ID
    @SerialName("fastestModel")
    val fastestModel: String?  // Will be a model ID
)
