package com.example.core.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "quick_note_time_tracker_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_GOOGLE_SHEET_CONNECTED = "google_sheet_connected"
        private const val KEY_GOOGLE_SHEET_ID = "google_sheet_id"
        private const val KEY_GOOGLE_SHEET_NAME = "google_sheet_name"
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val KEY_TIMER_MODE = "timer_mode" // "THAO_TAC_NHANH" / "TIENTRINH"
    }

    var isPinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_ENABLED, value).apply()

    var pinCode: String
        get() = prefs.getString(KEY_PIN_CODE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    var isGoogleSheetConnected: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_SHEET_CONNECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_GOOGLE_SHEET_CONNECTED, value).apply()

    var googleSheetId: String
        get() = prefs.getString(KEY_GOOGLE_SHEET_ID, "1AbcD2eFgHijKlMnOpQrStUvwXxZ") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_SHEET_ID, value).apply()

    var googleSheetName: String
        get() = prefs.getString(KEY_GOOGLE_SHEET_NAME, "My Time Tracker") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_SHEET_NAME, value).apply()

    var syncIntervalMinutes: Int
        get() = prefs.getInt(KEY_SYNC_INTERVAL_MINUTES, 15)
        set(value) = prefs.edit().putInt(KEY_SYNC_INTERVAL_MINUTES, value).apply()

    var timerMode: String
        get() = prefs.getString(KEY_TIMER_MODE, "THAO_TAC_NHANH") ?: "THAO_TAC_NHANH"
        set(value) = prefs.edit().putString(KEY_TIMER_MODE, value).apply()
}
