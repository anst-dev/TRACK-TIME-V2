package com.example.core.network

import android.util.Log
import com.example.BuildConfig
import com.example.core.database.TaskEntity
import com.example.core.database.TimeLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getAiAnalysis(
        tasks: List<TaskEntity>,
        timeLogs: List<TimeLogEntity>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieval of GEMINI_API_KEY from BuildConfig: ${e.message}")
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext """
                {
                  "status": "warning",
                  "analysis": "Hệ thống chưa cấu hình Gemini API Key. Vui lòng cấu hình Key của bạn trong bảng điều khiển Secrets của AI Studio để mở khóa tính năng phân tích hiệu suất tối ưu bằng Trí tuệ nhân tạo.",
                  "trends": [
                     {"title": "Giờ làm việc tốt nhất", "value": "Chưa ghi nhận (Thiếu API Key)"},
                     {"title": "Giờ dễ bị phân tâm", "value": "Chưa ghi nhận (Thiếu API Key)"},
                     {"title": "Ngày làm việc tập trung", "value": "Chưa ghi nhận (Thiếu API Key)"}
                  ],
                  "proposals": [
                     "Thử thêm các công việc mới trong Ma trận Eisenhower.",
                     "Theo dõi thời gian bằng bộ đếm của từng Nhóm công việc quan trọng.",
                     "Đồng bộ với Google Sheets để lưu trữ dữ liệu lâu bền."
                  ]
                }
            """.trimIndent()
        }

        // Build a highly descriptive prompt in Vietnamese, requesting structured JSON back
        val totalWorkTime = timeLogs.sumOf { it.durationMinutes }
        val q1Time = tasks.filter { it.quadrant == 1 }.sumOf { it.actualMinutes }
        val q2Time = tasks.filter { it.quadrant == 2 }.sumOf { it.actualMinutes }
        val q3Time = tasks.filter { it.quadrant == 3 }.sumOf { it.actualMinutes }
        val q4Time = tasks.filter { it.quadrant == 4 }.sumOf { it.actualMinutes }

        val taskSummary = tasks.joinToString("\n") { task ->
            "- ${task.title} (Nhóm ${task.quadrant}, Ước tính: ${task.estimatedMinutes} phút, Thực tế: ${task.actualMinutes} phút, Trạng thái: ${task.status})"
        }

        val prompt = """
            Bạn là một AI phân tích hiệu suất làm việc chuyên sâu cho công cụ "Quick Note & Time Tracker" tích hợp Ma Trận Eisenhower.
            
            Dưới đây là dữ liệu ghi chép hôm nay:
            - Tổng thời gian ghi nhận (phút): $totalWorkTime phút.
            - Phân bổ theo Ma Trận:
               + Nhóm 1 (Quan trọng - Khẩn cấp): $q1Time phút
               + Nhóm 2 (Quan trọng - Không khẩn cấp): $q2Time phút
               + Nhóm 3 (Không quan trọng - Khẩn cấp): $q3Time phút
               + Nhóm 4 (Không quan trọng - Không khẩn cấp): $q4Time phút
            - Danh sách công việc đang thực hiện và ghi chú:
            $taskSummary
            
            Hãy phân tích dữ liệu trên và trả về một chuỗi JSON thuần túy bằng TIẾNG VIỆT, khớp chính xác định dạng cấu trúc dưới đây (KHÔNG THÊM CÁC THÔNG TIN NGOÀI JSON, KHÔNG BỌC TRONG BLOCK CODE ```json):
            {
              "status": "success",
              "analysis": "[Hãy nhận xét xúc tích từ 2-3 câu về hiệu suất làm việc hôm nay của người dùng. Gợi ý giảm tải nhóm 3, 4 và đầu tư sâu vào nhóm 2]",
              "trends": [
                {"title": "Giờ làm việc hiệu quả nhất", "value": "09:00 - 11:00 (hoặc thời gian tối ưu tương ứng)"},
                {"title": "Giờ dễ bị phân tâm", "value": "15:00 - 16:00 (hoặc tương ứng)"},
                {"title": "Đặc trưng tập trung", "value": "Bạn thường làm việc nhiều vào các ngày đầu tuần"}
              ],
              "proposals": [
                "[Đề xuất cụ thể 1 cho ngày mai, ví dụ: Ưu tiên hoàn thành 3 task quan trọng]",
                "[Đề xuất cụ thể 2 cho ngày mai, ví dụ: Hạn chế kiểm tra email sau 16:00]",
                "[Đề xuất cụ thể 3 cho ngày mai, ví dụ: Dành thời gian buổi sáng cho việc lập kế hoạch]"
              ]
            }
        """.trimIndent()

        val jsonRequest = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        jsonRequest.put("contents", contentsArray)

        // Request JSON back
        val configObj = JSONObject()
        val formatObj = JSONObject()
        formatObj.put("mimeType", "application/json")
        configObj.put("responseMimeType", "application/json")
        jsonRequest.put("generationConfig", configObj)

        val requestBodyText = jsonRequest.toString()
        val requestBody = requestBodyText.toRequestBody(JSON_MEDIA_TYPE)

        val url = "$BASE_URL/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API Request failed with code ${response.code}: $errorBody")
                    // Fallback to offline message
                    return@withContext fallbackResponse("Lỗi kết nối từ máy chủ AI. Mã lỗi: ${response.code}")
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val aiText = parts.getJSONObject(0).optString("text")
                        if (aiText.isNotEmpty()) {
                            return@withContext aiText.trim()
                        }
                    }
                }
                return@withContext fallbackResponse("Không nhận được dữ liệu phản hồi hợp lệ từ AI.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing API request: ${e.message}", e)
            return@withContext fallbackResponse("Vui lòng kiểm tra kết nối mạng của bạn để cập nhật đề xuất AI.")
        }
    }

    private fun fallbackResponse(msg: String): String {
        return """
            {
              "status": "offline",
              "analysis": "Chúng tôi không thể lấy phân tích trực tiếp ngay lúc này. $msg",
              "trends": [
                 {"title": "Giờ làm việc hiệu quả nhất", "value": "09:00 - 11:00 (Tải trước)"},
                 {"title": "Giờ dễ bị phân tâm", "value": "15:00 - 16:00 (Tải trước)"},
                 {"title": "Bạn thường làm việc tập trung vào", "value": "Thứ 3, Thứ 5"}
              ],
              "proposals": [
                 "Ưu tiên hoàn thành: 3 task quan trọng",
                 "Hạn chế kiểm tra email sau 16:00",
                 "Dành thời gian cho việc lập kế hoạch buổi sáng"
              ]
            }
        """.trimIndent()
    }
}
