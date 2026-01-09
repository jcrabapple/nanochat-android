package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class UserModelDto(
    val modelId: String,
    val provider: String,
    val enabled: Boolean,
    val pinned: Boolean,
    val icon_url: String? = null
)

@Serializable
data class UpdateUserModelRequest(
    val action: String,  // "set" | "togglePinned" | "enableInitial"
    val provider: String,
    val modelId: String,
    val enabled: Boolean? = null
)

@Serializable
data class ModelProvidersResponse(
    val canonicalId: String,
    val displayName: String,
    val supportsProviderSelection: Boolean,
    val providers: List<ProviderInfo>
)

@Serializable
data class ProviderInfo(
    val id: String,
    val name: String
)

// Helper to parse the user models response which comes as an object with model IDs as keys
fun parseUserModelsResponse(jsonElement: JsonElement): List<UserModelDto> {
    if (jsonElement !is JsonObject) return emptyList()

    return jsonElement.jsonObject.entries.map { (key, value) ->
        if (value is JsonObject) {
            val modelId = value["modelId"]?.jsonPrimitive?.content ?: key
            val provider = value["provider"]?.jsonPrimitive?.content ?: "nanogpt"
            val pinned = value["pinned"]?.jsonPrimitive?.content == "true"
            val iconUrl = value["icon_url"]?.jsonPrimitive?.content

            // All models returned from the API are enabled (they're in the user's model list)
            UserModelDto(
                modelId = modelId,
                provider = provider,
                enabled = true,
                pinned = pinned,
                icon_url = iconUrl
            )
        } else {
            UserModelDto(
                modelId = key,
                provider = "nanogpt",
                enabled = true,
                pinned = true,
                icon_url = null
            )
        }
    }
}
