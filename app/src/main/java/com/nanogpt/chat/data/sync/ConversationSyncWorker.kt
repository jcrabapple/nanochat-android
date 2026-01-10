package com.nanogpt.chat.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nanogpt.chat.data.repository.ConversationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically syncs conversations from the server.
 *
 * Runs every 15 minutes when network is available and battery is not low.
 * Ensures that conversations created on other devices appear in the Android app.
 */
@HiltWorker
class ConversationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val conversationRepository: ConversationRepository,
    private val conversationSyncManager: ConversationSyncManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "conversation_sync_work"

        /**
         * Schedule the periodic conversation sync worker.
         *
         * @param context Application context
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<ConversationSyncWorker>(
                15, TimeUnit.MINUTES  // Minimum interval for PeriodicWorkRequest
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    10,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,  // Keep existing if already scheduled
                    syncRequest
                )

            Log.d("ConversationSyncWorker", "Conversation sync worker scheduled (every 15 minutes)")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d("ConversationSyncWorker", "Starting conversation sync...")

            val result = conversationRepository.fetchConversationsFromApi()

            if (result.isSuccess) {
                // Notify listeners that conversations have been refreshed
                conversationSyncManager.notifyConversationsRefreshed()

                Log.d("ConversationSyncWorker", "Conversation sync successful")
                Result.success()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("ConversationSyncWorker", "Conversation sync failed: $error")

                // Retry on failure
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("ConversationSyncWorker", "Sync failed with exception: ${e.message}", e)
            Result.failure()
        }
    }
}
