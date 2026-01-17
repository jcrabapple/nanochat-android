package com.nanogpt.chat.data.repository

import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.VideoGenerationRequest
import com.nanogpt.chat.data.remote.dto.VideoGenerationStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoGenerationRepository @Inject constructor(
    private val api: NanoChatApi,
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val POLLING_INTERVAL_MS = 2000L // Poll every 2 seconds
        private const val MAX_POLL_ATTEMPTS = 150 // Max 5 minutes (150 * 2 seconds)
    }

    /**
     * Initiates a video generation request.
     * @return Result containing the runId on success, or Exception on failure
     */
    suspend fun generateVideo(model: String, prompt: String): Result<String> {
        return try {
            android.util.Log.d("VideoGenerationRepository", "Starting video generation with model: $model")

            val request = VideoGenerationRequest(
                model = model,
                prompt = prompt
            )

            val response = api.generateVideo(request)

            if (response.isSuccessful && response.body() != null) {
                val runId = response.body()!!.runId
                android.util.Log.d("VideoGenerationRepository", "Video generation started with runId: $runId")
                Result.success(runId)
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("VideoGenerationRepository", "Failed to generate video: $error")
                Result.failure(Exception("Failed to generate video: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoGenerationRepository", "Error generating video", e)
            Result.failure(e)
        }
    }

    /**
     * Polls the video status endpoint until completion or failure.
     * @return Flow emitting VideoGenerationStatus updates
     */
    suspend fun pollVideoStatus(runId: String, model: String): Flow<VideoGenerationResult> = flow {
        var attempts = 0

        while (attempts < MAX_POLL_ATTEMPTS) {
            try {
                android.util.Log.d("VideoGenerationRepository", "Polling video status: runId=$runId, attempt=$attempts")

                val response = api.getVideoStatus(runId, model)

                if (response.isSuccessful && response.body() != null) {
                    val statusData = response.body()!!.data
                    val status = statusData.status

                    android.util.Log.d("VideoGenerationRepository", "Video status: $status")

                    when (status) {
                        VideoGenerationStatus.IN_QUEUE -> {
                            emit(VideoGenerationResult.InQueue)
                        }
                        VideoGenerationStatus.IN_PROGRESS -> {
                            emit(VideoGenerationResult.InProgress)
                        }
                        VideoGenerationStatus.COMPLETED -> {
                            // Extract video URL from output
                            val videoUrl = statusData.output?.video?.url
                            if (videoUrl != null) {
                                android.util.Log.d("VideoGenerationRepository", "Video generation completed: $videoUrl")
                                emit(VideoGenerationResult.Completed(videoUrl))
                                return@flow // Stop polling
                            } else {
                                android.util.Log.e("VideoGenerationRepository", "Video completed but no URL found")
                                emit(VideoGenerationResult.Failed("Video completed but no URL returned"))
                                return@flow
                            }
                        }
                        VideoGenerationStatus.FAILED -> {
                            android.util.Log.e("VideoGenerationRepository", "Video generation failed")
                            emit(VideoGenerationResult.Failed("Video generation failed"))
                            return@flow
                        }
                    }
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    android.util.Log.e("VideoGenerationRepository", "Error polling status: $error")
                    // Don't fail immediately, might be a temporary network issue
                }

                attempts++
                if (attempts < MAX_POLL_ATTEMPTS) {
                    delay(POLLING_INTERVAL_MS)
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoGenerationRepository", "Exception polling video status", e)
                // Don't fail immediately on network errors, continue polling
                attempts++
                if (attempts < MAX_POLL_ATTEMPTS) {
                    delay(POLLING_INTERVAL_MS)
                }
            }
        }

        // Max attempts reached
        android.util.Log.e("VideoGenerationRepository", "Video generation polling timeout after $MAX_POLL_ATTEMPTS attempts")
        emit(VideoGenerationResult.Failed("Video generation timeout"))
    }

    /**
     * Sealed class representing video generation results
     */
    sealed class VideoGenerationResult {
        object InQueue : VideoGenerationResult()
        object InProgress : VideoGenerationResult()
        data class Completed(val videoUrl: String) : VideoGenerationResult()
        data class Failed(val error: String) : VideoGenerationResult()
    }
}
