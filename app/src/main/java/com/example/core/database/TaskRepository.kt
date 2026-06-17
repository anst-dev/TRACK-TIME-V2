package com.example.core.database

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allTimeLogs: Flow<List<TimeLogEntity>> = taskDao.getAllTimeLogs()
    val syncLogs: Flow<List<SyncLogEntity>> = taskDao.getSyncLogs()

    fun getTasksByDate(date: String): Flow<List<TaskEntity>> = taskDao.getTasksByDate(date)

    suspend fun getTaskById(id: Int): TaskEntity? = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Int) = taskDao.deleteTaskById(id)

    // Time Logs
    fun getTimeLogsForTask(taskId: Int): Flow<List<TimeLogEntity>> = taskDao.getTimeLogsForTask(taskId)

    fun getTimeLogsByDate(date: String): Flow<List<TimeLogEntity>> = taskDao.getTimeLogsByDate(date)

    suspend fun getActiveTimeLogForTask(taskId: Int): TimeLogEntity? = taskDao.getActiveTimeLogForTask(taskId)

    suspend fun insertTimeLog(log: TimeLogEntity): Long = taskDao.insertTimeLog(log)

    suspend fun updateTimeLog(log: TimeLogEntity) = taskDao.updateTimeLog(log)

    suspend fun deleteTimeLog(id: Int) = taskDao.deleteTimeLog(id)

    // Sync History
    suspend fun insertSyncLog(log: SyncLogEntity): Long = taskDao.insertSyncLog(log)

    suspend fun clearSyncLogs() = taskDao.clearSyncLogs()
}
