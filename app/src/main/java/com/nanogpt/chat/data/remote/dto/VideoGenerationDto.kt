package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoGenerationRequest(
    val model: String,
    val prompt: String,
    val duration: Int? = null,        // Optional: duration in seconds
    val aspect_ratio: String? = null  // Optional: "16:9", "9:16", "1:1"
)

@Serializable
data class VideoGenerationResponse(
    val runId: String
)

@Serializable
data class VideoStatusResponse(
    val data: VideoStatusData
)

@Serializable
data class VideoStatusData(
    val status: VideoGenerationStatus,
    val output: VideoOutput?
)

@Serializable
data class VideoOutput(
    val video: VideoUrl?
)

@Serializable
data class VideoUrl(
    val url: String
)

@Serializable
enum class VideoGenerationStatus {
    @SerialName("COMPLETED")
    COMPLETED,

    @SerialName("IN_QUEUE")
    IN_QUEUE,

    @SerialName("IN_PROGRESS")
    IN_PROGRESS,

    @SerialName("FAILED")
    FAILED
}
