package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val enabled: Boolean,
    val pinned: Boolean,
    val capabilities: ModelCapabilities,
    val pricing: ModelPricing? = null,
    val subscription: ModelSubscription? = null
)

@Serializable
data class ModelCapabilities(
    val vision: Boolean = false,
    val reasoning: Boolean = false,
    val images: Boolean = false,
    val video: Boolean = false
)

@Serializable
data class ModelPricing(
    val prompt: String? = null,
    val completion: String? = null,
    val image: String? = null,
    val request: String? = null
)

@Serializable
data class ModelSubscription(
    val included: Boolean = false,
    val note: String? = null
)
