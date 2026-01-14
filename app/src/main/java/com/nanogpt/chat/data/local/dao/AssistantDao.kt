package com.nanogpt.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nanogpt.chat.data.local.entity.AssistantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {

    @Query("SELECT * FROM assistants ORDER BY name ASC")
    fun getAllAssistants(): Flow<List<AssistantEntity>>

    @Query("SELECT * FROM assistants WHERE id = :id")
    suspend fun getAssistantById(id: String): AssistantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssistant(assistant: AssistantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssistants(assistants: List<AssistantEntity>)

    @Update
    suspend fun updateAssistant(assistant: AssistantEntity)

    @Delete
    suspend fun deleteAssistant(assistant: AssistantEntity)

    @Query("DELETE FROM assistants WHERE id = :id")
    suspend fun deleteAssistantById(id: String)

    @Query("DELETE FROM assistants WHERE syncStatus = 'PENDING'")
    suspend fun deletePendingAssistants()

    @Query("SELECT * FROM assistants WHERE syncStatus = 'PENDING'")
    suspend fun getPendingAssistants(): List<AssistantEntity>
}
