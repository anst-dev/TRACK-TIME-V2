package com.example.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.database.AppDatabase
import com.example.core.database.TaskRepository
import com.example.core.database.TimeLogEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

object TimerManager {
    val activeTaskId = MutableStateFlow<Int?>(null)
    val runningSeconds = MutableStateFlow(0L)
    val activeTaskTitle = MutableStateFlow<String>("")
    val isRunning = MutableStateFlow<Boolean>(false)
}

class TimerService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_TIMER"
        const val ACTION_STOP = "com.example.action.STOP_TIMER"
        const val EXTRA_TASK_ID = "com.example.extra.TASK_ID"
        const val NOTIFICATION_ID = 8888
        const val CHANNEL_ID = "chrono_timer_channel"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                if (taskId != -1) {
                    handleStartTimer(taskId)
                }
            }
            ACTION_STOP -> {
                handleStopTimer()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun handleStartTimer(taskId: Int) {
        if (TimerManager.activeTaskId.value == taskId) {
            return
        }

        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = TaskRepository(db.taskDao())

            // If there's an already active task running, stop it first!
            val currentActiveId = TimerManager.activeTaskId.value
            if (currentActiveId != null && currentActiveId != taskId) {
                saveActiveTimeLogState()
                val oldTask = repository.getTaskById(currentActiveId)
                if (oldTask != null && oldTask.status == "DOING") {
                    repository.updateTask(oldTask.copy(status = "WAITING", updatedAt = System.currentTimeMillis()))
                }
            }

            val task = repository.getTaskById(taskId) ?: return@launch
            repository.updateTask(task.copy(status = "DOING", updatedAt = System.currentTimeMillis()))

            withContext(Dispatchers.Main) {
                TimerManager.activeTaskId.value = taskId
                TimerManager.runningSeconds.value = 0L
                TimerManager.activeTaskTitle.value = task.title
                TimerManager.isRunning.value = true
            }

            // Insert TimeLog entity (start)
            repository.insertTimeLog(
                TimeLogEntity(
                    taskId = taskId,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    durationSeconds = 0
                )
            )

            startTicker()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()

        // Fast-path build first notification
        val title = TimerManager.activeTaskTitle.value
        val initialNotification = buildNotification("Đang chuẩn bị...", title)
        startForeground(NOTIFICATION_ID, initialNotification)

        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    TimerManager.runningSeconds.value += 1
                }

                val seconds = TimerManager.runningSeconds.value
                val taskTitle = TimerManager.activeTaskTitle.value
                val formattedTime = formatTimeClock(seconds)

                updateNotification("Đang chạy: $taskTitle", formattedTime)

                if (seconds % 30 == 0L) {
                    saveActiveTimeLogState()
                }
            }
        }
    }

    private fun handleStopTimer() {
        serviceScope.launch {
            tickerJob?.cancel()
            tickerJob = null

            saveActiveTimeLogState()

            val taskId = TimerManager.activeTaskId.value
            if (taskId != null) {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = TaskRepository(db.taskDao())
                val task = repository.getTaskById(taskId)
                if (task != null && task.status == "DOING") {
                    repository.updateTask(task.copy(status = "WAITING", updatedAt = System.currentTimeMillis()))
                }
            }

            withContext(Dispatchers.Main) {
                TimerManager.activeTaskId.value = null
                TimerManager.runningSeconds.value = 0L
                TimerManager.activeTaskTitle.value = ""
                TimerManager.isRunning.value = false
            }

            stopForeground(true)
            stopSelf()
        }
    }

    private suspend fun saveActiveTimeLogState() {
        val taskId = TimerManager.activeTaskId.value ?: return
        
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(db.taskDao())

        val dbLogs = repository.allTimeLogs.first()
        val activeLog = dbLogs.firstOrNull { it.taskId == taskId && it.endTime == null }
        if (activeLog != null) {
            val endTime = System.currentTimeMillis()
            val durationSec = ((endTime - activeLog.startTime) / 1000).toInt()
            repository.updateTimeLog(activeLog.copy(endTime = endTime, durationSeconds = durationSec))

            val task = repository.getTaskById(taskId)
            if (task != null) {
                val newActual = task.actualSeconds + durationSec
                repository.updateTask(task.copy(actualSeconds = newActual, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bộ Đếm Thời Gian",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị bộ đếm thời gian đang hoạt động trong nền"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                com.example.R.mipmap.ic_launcher,
                "Tạm dừng",
                stopPendingIntent
            )
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatTimeClock(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
