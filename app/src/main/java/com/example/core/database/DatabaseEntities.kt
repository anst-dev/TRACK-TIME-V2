package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val quadrant: Int, // 1 to 4 (1: Red, 2: Green, 3: Orange, 4: Grey)
    val estimatedMinutes: Int,
    val actualMinutes: Int,
    val status: String, // "DOING", "WAITING", "COMPLETED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val date: String, // YYYY-MM-DD
    val isSynced: Boolean = false,
    val isNote: Boolean = false
)

@Entity(tableName = "time_logs")
data class TimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val startTime: Long,
    val endTime: Long?,
    val durationMinutes: Int,
    val isSynced: Boolean = false
)

@Entity(tableName = "sync_history")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "Thành công" or "Thất bại"
    val message: String
)
