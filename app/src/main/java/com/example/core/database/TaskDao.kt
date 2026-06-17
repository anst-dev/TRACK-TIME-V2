package com.example.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Task Operations
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt DESC")
    fun getTasksByDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    // TimeLog Operations
    @Query("SELECT * FROM time_logs WHERE taskId = :taskId")
    fun getTimeLogsForTask(taskId: Int): Flow<List<TimeLogEntity>>

    @Query("SELECT * FROM time_logs ORDER BY startTime DESC")
    fun getAllTimeLogs(): Flow<List<TimeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeLog(log: TimeLogEntity): Long

    @Update
    suspend fun updateTimeLog(log: TimeLogEntity)

    @Query("DELETE FROM time_logs WHERE id = :id")
    suspend fun deleteTimeLog(id: Int)

    @Query("SELECT * FROM time_logs WHERE taskId = :taskId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveTimeLogForTask(taskId: Int): TimeLogEntity?

    @Query("SELECT tl.* FROM time_logs tl INNER JOIN tasks t ON tl.taskId = t.id WHERE t.date = :date")
    fun getTimeLogsByDate(date: String): Flow<List<TimeLogEntity>>

    // SyncLog Operations
    @Query("SELECT * FROM sync_history ORDER BY timestamp DESC")
    fun getSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity): Long

    @Query("DELETE FROM sync_history")
    suspend fun clearSyncLogs()
}
