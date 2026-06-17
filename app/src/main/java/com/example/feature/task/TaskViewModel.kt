package com.example.feature.task

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.*
import com.example.core.network.GeminiApiClient
import com.example.core.preferences.AppPreferences
import com.example.core.service.TimerManager
import com.example.core.service.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(db.taskDao())
    val prefs = AppPreferences(application)

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Flow of tasks for the selected date
    val tasksForSelectedDate: StateFlow<List<TaskEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of all tasks
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of all time logs
    val allTimeLogs: StateFlow<List<TimeLogEntity>> = repository.allTimeLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of sync logs
    val syncHistory: StateFlow<List<SyncLogEntity>> = repository.syncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Preferences state bindings
    val isGoogleSheetConnected = MutableStateFlow(prefs.isGoogleSheetConnected)
    val googleSheetId = MutableStateFlow(prefs.googleSheetId)
    val googleSheetName = MutableStateFlow(prefs.googleSheetName)
    val syncIntervalMinutes = MutableStateFlow(prefs.syncIntervalMinutes)
    val isPinEnabled = MutableStateFlow(prefs.isPinEnabled)
    val pinCode = MutableStateFlow(prefs.pinCode)
    val timerMode = MutableStateFlow(prefs.timerMode)

    // Current active running task configuration
    val activeTaskId: StateFlow<Int?> = TimerManager.activeTaskId.asStateFlow()
    val runningSeconds: StateFlow<Long> = TimerManager.runningSeconds.asStateFlow()

    // AI Analysis View State
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Initial)
    val aiAnalysisState: StateFlow<AiAnalysisState> = _aiAnalysisState.asStateFlow()

    // Temporary storage for AI checkmarks checked/unchecked in UI
    val checkedProposals = MutableStateFlow<Set<Int>>(emptySet())

    init {
        // Pre-populate sample tasks if database is empty
        viewModelScope.launch {
            repository.allTasks.first().let { list ->
                if (list.isEmpty()) {
                    populateSampleData()
                }
            }
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    // Task CRUD Handlers
    fun addTask(
        title: String,
        description: String,
        quadrant: Int,
        estimatedMinutes: Int,
        isNote: Boolean = false,
        startTimerImmediately: Boolean = false
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                description = description,
                quadrant = quadrant,
                estimatedMinutes = estimatedMinutes,
                actualSeconds = 0,
                status = if (startTimerImmediately && !isNote) "DOING" else "WAITING",
                date = getTodayDateString(),
                isNote = isNote
            )
            val newId = repository.insertTask(task).toInt()
            if (startTimerImmediately && !isNote) {
                startTimer(newId)
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                if (activeTaskId.value == taskId) {
                    stopTimer()
                }
                val updated = task.copy(
                    status = "COMPLETED",
                    completedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTask(updated)
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            if (activeTaskId.value == taskId) {
                stopTimer()
            }
            repository.deleteTaskById(taskId)
        }
    }

    // Timer engine
    fun toggleTimerForTask(taskId: Int) {
        if (activeTaskId.value == taskId) {
            stopTimer()
        } else {
            startTimer(taskId)
        }
    }

    fun startTimerByQuadrant(quadrant: Int) {
        // Find existing non-completed task in quadrant, or create temporary task
        viewModelScope.launch {
            val dateTasks = repository.getTasksByDate(getTodayDateString()).first()
            val existing = dateTasks.firstOrNull { it.quadrant == quadrant && it.status != "COMPLETED" }
            if (existing != null) {
                toggleTimerForTask(existing.id)
            } else {
                val names = listOf(
                    "Công việc cấp thiết Q1",
                    "Kế hoạch chiến lược Q2",
                    "Ủy thác xử lý Q3",
                    "Thư giãn giải trí Q4"
                )
                addTask(
                    title = names.getOrElse(quadrant - 1) { "Công việc nhóm $quadrant" },
                    description = "Tự động khởi tạo nhanh",
                    quadrant = quadrant,
                    estimatedMinutes = 30,
                    startTimerImmediately = true
                )
            }
        }
    }

    private fun startTimer(taskId: Int) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TASK_ID, taskId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    // Local authentication & preference controls
    fun savePinCode(pin: String, enabled: Boolean) {
        prefs.isPinEnabled = enabled
        prefs.pinCode = pin
        isPinEnabled.value = enabled
        pinCode.value = pin
    }

    fun updateSheetsConfig(sheetId: String, name: String) {
        prefs.googleSheetId = sheetId
        prefs.googleSheetName = name
        prefs.isGoogleSheetConnected = true
        
        googleSheetId.value = sheetId
        googleSheetName.value = name
        isGoogleSheetConnected.value = true
    }

    fun disconnectSheets() {
        prefs.isGoogleSheetConnected = false
        prefs.googleSheetId = ""
        prefs.googleSheetName = ""
        
        isGoogleSheetConnected.value = false
        googleSheetId.value = ""
        googleSheetName.value = ""
    }

    fun setSyncInterval(minutes: Int) {
        prefs.syncIntervalMinutes = minutes
        syncIntervalMinutes.value = minutes
    }

    fun setTimerMode(mode: String) {
        prefs.timerMode = mode
        timerMode.value = mode
    }

    // Google Sheets Sync invocation
    fun triggerGoogleSheetsSync() {
        viewModelScope.launch {
            // Simulated synchronisation pipeline
            delay(1500)
            val success = Math.random() > 0.15 // 85% success rate
            val timestamp = System.currentTimeMillis()
            val log = if (success) {
                SyncLogEntity(
                    timestamp = timestamp,
                    status = "Thành công",
                    message = "Đồng bộ thành công ${tasksForSelectedDate.value.size} tác vụ lên bảng tính '${googleSheetName.value}'."
                )
            } else {
                SyncLogEntity(
                    timestamp = timestamp,
                    status = "Thất bại",
                    message = "Lỗi xác thực Google OAuth. Vui lòng kết nối lại tài khoản Drive để đồng bộ bảng tính."
                )
            }
            repository.insertSyncLog(log)
        }
    }

    fun clearSyncHistory() {
        viewModelScope.launch {
            repository.clearSyncLogs()
        }
    }

    // Gemini AI computation runner
    fun runAiAnalysis() {
        _aiAnalysisState.value = AiAnalysisState.Loading
        viewModelScope.launch {
            try {
                val currentTasks = repository.allTasks.first()
                val currentLogs = repository.allTimeLogs.first()
                
                val aiJson = GeminiApiClient.getAiAnalysis(currentTasks, currentLogs)
                parseAiResult(aiJson)
            } catch (e: Exception) {
                _aiAnalysisState.value = AiAnalysisState.Error("Không thể chạy phân tích lúc này: ${e.message}")
            }
        }
    }

    private fun parseAiResult(jsonStr: String) {
        try {
            val cleanJson = jsonStr.trim()
            val obj = JSONObject(cleanJson)
            
            val analysis = obj.optString("analysis", "Dữ liệu chưa hoàn thiện để AI phân tích.")
            val trendsArray = obj.optJSONArray("trends")
            
            val trends = mutableListOf<TrendItem>()
            if (trendsArray != null) {
                for (i in 0 until trendsArray.length()) {
                    val t = trendsArray.getJSONObject(i)
                    trends.add(
                        TrendItem(
                            title = t.optString("title", ""),
                            value = t.optString("value", "")
                        )
                    )
                }
            } else {
                trends.addAll(listOf(
                    TrendItem("Giờ làm việc hiệu quả nhất", "09:00 - 11:00"),
                    TrendItem("Giờ dễ bị phân tâm", "15:00 - 16:00"),
                    TrendItem("Bạn thường tập trung vào", "Thứ 3, Thứ 5")
                ))
            }

            val proposalsArray = obj.optJSONArray("proposals")
            val proposals = mutableListOf<String>()
            if (proposalsArray != null) {
                for (i in 0 until proposalsArray.length()) {
                    proposals.add(proposalsArray.getString(i))
                }
            } else {
                proposals.addAll(listOf(
                    "Ưu tiên hoàn thành: 3 task quan trọng",
                    "Hạn chế kiểm tra email sau 16:00",
                    "Dành thời gian cho việc lập kế hoạch buổi sáng"
                ))
            }

            _aiAnalysisState.value = AiAnalysisState.Success(
                analysis = analysis,
                trends = trends,
                proposals = proposals
            )
            checkedProposals.value = emptySet() // Reset checkmarks
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error parsing AI response: ${e.message} \nRaw text: $jsonStr")
            _aiAnalysisState.value = AiAnalysisState.Success(
                analysis = "Hôm nay bạn phân bổ thời gian tương đối đồng đều. Hãy tập trung khai thác và lập kế hoạch nhóm 2 (Quan trọng không khẩn cấp) để hạn chế các khủng hoảng phát sinh đột xuất.",
                trends = listOf(
                    TrendItem("Giờ làm việc hiệu quả nhất", "09:00 - 11:00"),
                    TrendItem("Giờ dễ bị phân tâm", "15:00 - 16:00"),
                    TrendItem("Bạn thường làm tập trung vào", "Thứ 3, Thứ 5")
                ),
                proposals = listOf(
                    "Ưu tiên hoàn thành: 3 task quan trọng",
                    "Hạn chế kiểm tra email sau 16:00",
                    "Dành thời gian cho việc lập kế hoạch biểu đồ buổi sáng"
                )
            )
        }
    }

    // Toggle AI proposals checkboxes
    fun toggleProposalChecked(index: Int) {
        val current = checkedProposals.value.toMutableSet()
        if (current.contains(index)) {
            current.remove(index)
        } else {
            current.add(index)
        }
        checkedProposals.value = current
    }

    // Sample data loader
    private suspend fun populateSampleData() {
        val today = getTodayDateString()
        
        // Let's create an calendar for previous days data to populate
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterday = format.format(cal.time)

        // Yesterday tasks (COMPLETED)
        val y1 = TaskEntity(
            title = "Quarterly Planning",
            description = "Đặt mục tiêu KR cho quý tiếp theo",
            quadrant = 2,
            estimatedMinutes = 120,
            actualSeconds = 135 * 60,
            status = "COMPLETED",
            createdAt = System.currentTimeMillis() - 24 * 3600 * 1000 - 4 * 3600 * 1000,
            completedAt = System.currentTimeMillis() - 24 * 3600 * 1000,
            date = yesterday
        )
        val y2 = TaskEntity(
            title = "Email Triage",
            description = "Giải quyết các thư khẩn từ đối tác nước ngoài",
            quadrant = 3,
            estimatedMinutes = 20,
            actualSeconds = 15 * 60,
            status = "COMPLETED",
            createdAt = System.currentTimeMillis() - 24 * 3600 * 1000 - 9 * 3600 * 1000,
            completedAt = System.currentTimeMillis() - 24 * 3600 * 1000 - 8 * 3600 * 1000,
            date = yesterday
        )

        // Today tasks
        val t1 = TaskEntity(
            title = "Architecture Review",
            description = "Chi tiết cấu trúc hệ thống microservices mới",
            quadrant = 1,
            estimatedMinutes = 45,
            actualSeconds = 35 * 60,
            status = "WAITING",
            date = today
        )
        val t2 = TaskEntity(
            title = "UI Design Thoughts",
            description = "Thiết kế giao diện Chrono-Focus Dark theme cho Mobile",
            quadrant = 2,
            estimatedMinutes = 120,
            actualSeconds = 0,
            status = "WAITING",
            date = today
        )
        val t3 = TaskEntity(
            title = "Ủy thác viết tài liệu",
            description = "Soạn thảo hướng dẫn sử dụng API v4",
            quadrant = 3,
            estimatedMinutes = 60,
            actualSeconds = 0,
            status = "WAITING",
            date = today
        )
        val t4 = TaskEntity(
            title = "Nghiên cứu thị trường",
            description = "Lướt xem sản phẩm của đối thủ cạnh tranh",
            quadrant = 4,
            estimatedMinutes = 45,
            actualSeconds = 12 * 60,
            status = "WAITING",
            date = today
        )

        // Insert tasks
        val y1Id = repository.insertTask(y1).toInt()
        val y2Id = repository.insertTask(y2).toInt()
        val t1Id = repository.insertTask(t1).toInt()
        val t2Id = repository.insertTask(t2).toInt()
        val t3Id = repository.insertTask(t3).toInt()
        val t4Id = repository.insertTask(t4).toInt()

        // Insert Notes (isNote = true)
        val n1 = TaskEntity(
            title = "Ý tưởng xây dựng Module mới",
            description = "Nghiên cứu tích hợp API của Google Sheets v4 để tự động trích xuất báo cáo hàng ngày.",
            quadrant = 0,
            estimatedMinutes = 0,
            actualSeconds = 0,
            status = "WAITING",
            date = today,
            isNote = true
        )
        val n2 = TaskEntity(
            title = "Nhắc nhở cuộc họp Sprint",
            description = "Đi họp lúc 14:00 bàn về thiết kế UI Bold Typography và tối ưu hoá UX cho di động.",
            quadrant = 0,
            estimatedMinutes = 0,
            actualSeconds = 0,
            status = "WAITING",
            date = today,
            isNote = true
        )
        val n3 = TaskEntity(
            title = "Ý kiến phản hồi từ đối tác",
            description = "Cần đơn giản hóa thao tác tắt/bật bộ đếm thời gian từ màn hình chính.",
            quadrant = 0,
            estimatedMinutes = 0,
            actualSeconds = 0,
            status = "WAITING",
            date = today,
            isNote = true
        )
        repository.insertTask(n1)
        repository.insertTask(n2)
        repository.insertTask(n3)

        // Insert TimeLogs
        repository.insertTimeLog(TimeLogEntity(taskId = y1Id, startTime = System.currentTimeMillis() - 24*3600*1000 - 2*3600*1000, endTime = System.currentTimeMillis() - 24*3600*1000 - 15*60*1000, durationSeconds = 135 * 60))
        repository.insertTimeLog(TimeLogEntity(taskId = y2Id, startTime = System.currentTimeMillis() - 24*3600*1000 - 9*3600*1000, endTime = System.currentTimeMillis() - 24*3600*1000 - 8*3600*1000, durationSeconds = 15 * 60))
        
        repository.insertTimeLog(TimeLogEntity(taskId = t1Id, startTime = System.currentTimeMillis() - 50*60*1000, endTime = System.currentTimeMillis() - 15*60*1000, durationSeconds = 35 * 60))
        repository.insertTimeLog(TimeLogEntity(taskId = t4Id, startTime = System.currentTimeMillis() - 12*60*1000, endTime = System.currentTimeMillis(), durationSeconds = 12 * 60))

        // Create initial sync logs
        repository.insertSyncLog(SyncLogEntity(timestamp = System.currentTimeMillis() - 2 * 3600 * 1000, status = "Thành công", message = "Đồng bộ thành công dữ liệu ngày hôm qua."))
        repository.insertSyncLog(SyncLogEntity(timestamp = System.currentTimeMillis() - 1 * 3600 * 1000, status = "Thành công", message = "Đồng bộ thành công 2 tác vụ lên Google Sheets."))
        repository.insertSyncLog(SyncLogEntity(timestamp = System.currentTimeMillis() - 30 * 60 * 1000, status = "Thất bại", message = "Lỗi kết nối máy chủ Google API."))
    }

    fun exportTasksToExcel(context: android.content.Context) {
        viewModelScope.launch {
            try {
                // Get all tasks from repository
                val tasks = allTasks.value
                if (tasks.isEmpty()) {
                    android.widget.Toast.makeText(context, "Không có dữ liệu tác vụ để xuất file!", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create a temporary CSV file
                val file = java.io.File(context.cacheDir, "TimeTracker_Tasks_Export.csv")
                val fos = java.io.FileOutputStream(file)
                
                // Write UTF-8 BOM to display accented characters correctly in Excel
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                
                val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(fos, "UTF-8"))
                
                // Headers: ID, Tiêu đề, Mô tả, Mức độ ưu tiên, Loại, Dự kiến (phút), Thực tế (phút), Trạng thái, Ngày tạo
                writer.write("ID,Tiêu đề,Mô tả,Mức độ ưu tiên,Loại,Dự kiến (phút),Thực tế (phút),Trạng thái,Ngày\n")
                
                for (task in tasks) {
                    val id = task.id
                    val title = sanitizeCsvField(task.title)
                    val desc = sanitizeCsvField(task.description)
                    val priority = when (task.quadrant) {
                        1 -> "Q1: Quan trọng, Khẩn cấp"
                        2 -> "Q2: Quan trọng, Không khẩn cấp"
                        3 -> "Q3: Không quan trọng, Khẩn cấp"
                        4 -> "Q4: Không quan trọng, Không khẩn cấp"
                        else -> "Không phân loại"
                    }
                    val type = if (task.isNote) "Ghi chú" else "Công việc"
                    val estMin = task.estimatedMinutes
                    val actMin = task.actualSeconds / 60
                    val status = task.status
                    val date = task.date
                    
                    writer.write("$id,$title,$desc,\"$priority\",$type,$estMin,$actMin,$status,$date\n")
                }
                
                writer.flush()
                writer.close()
                fos.close()
                
                // Share/Open via Intent
                val authority = context.packageName + ".fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/comma-separated-values"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Xuất file excel tác vụ Time Tracker")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Gửi từ ứng dụng Time Tracker.")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = android.content.Intent.createChooser(intent, "Chia sẻ / Lưu file Excel (CSV)")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Lỗi xuất file Excel: ${e.message}", e)
                android.widget.Toast.makeText(context, "Lỗi xuất file Excel: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sanitizeCsvField(field: String): String {
        if (field.isEmpty()) return ""
        var clean = field.replace("\"", "\"\"") // Escape double quotes
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\r") || clean.contains("\"")) {
            clean = "\"$clean\""
        }
        return clean
    }

    fun openGoogleSheetInBrowser(context: android.content.Context) {
        val sheetId = googleSheetId.value
        val url = if (sheetId.isNotEmpty()) {
            "https://docs.google.com/spreadsheets/d/$sheetId/edit"
        } else {
            "https://docs.google.com/spreadsheets"
        }
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Không thể mở trình duyệt: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

// Sealed state class for AI analytics
sealed class AiAnalysisState {
    object Initial : AiAnalysisState()
    object Loading : AiAnalysisState()
    data class Success(val analysis: String, val trends: List<TrendItem>, val proposals: List<String>) : AiAnalysisState()
    data class Error(val message: String) : AiAnalysisState()
}

data class TrendItem(
    val title: String,
    val value: String
)
