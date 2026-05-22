package com.example.smartattendanceapp

import android.content.Context
import androidx.core.content.edit

// ─────────────────────────────────────────────────────────────────────────────
// AppPreferences — stores lightweight non-DB settings using SharedPreferences.
// Notifications and theme are per-device, not per-account, so SharedPrefs
// is the right tool (Room is for structured data like students/attendance).
// ─────────────────────────────────────────────────────────────────────────────

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("attendx_prefs", Context.MODE_PRIVATE)

    // ── Notification settings ─────────────────────────────────────────────────
    var pushNotificationsEnabled: Boolean
        get()      = prefs.getBoolean("push_notifications", true)
        set(value) = prefs.edit { putBoolean("push_notifications", value) }

    var absenceAlertsEnabled: Boolean
        get()      = prefs.getBoolean("absence_alerts", true)
        set(value) = prefs.edit { putBoolean("absence_alerts", value) }

    // ── Theme (mirrors UserEntity.isDarkMode, kept in sync) ───────────────────
    // Stored here too so the theme is available before the DB user is loaded
    var isLightMode: Boolean
        get()      = prefs.getBoolean("light_mode", false)   // default: dark
        set(value) = prefs.edit { putBoolean("light_mode", value) }

    companion object {
        @Volatile private var INSTANCE: AppPreferences? = null
        fun get(context: Context): AppPreferences =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
    }
}