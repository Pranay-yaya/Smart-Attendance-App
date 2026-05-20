package com.example.smartattendanceapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// Student — stores enrolled student info + their face embedding
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey
    @ColumnInfo(name = "roll") val roll: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "className") val className: String,
    // 128-D face embedding stored as JSON string via Converters.kt
    @ColumnInfo(name = "faceEmbedding") val faceEmbedding: FloatArray
) {
    // equals/hashCode only use roll so Room deduplication works correctly
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return roll == (other as StudentEntity).roll
    }
    override fun hashCode(): Int = roll.hashCode()
}

// ─────────────────────────────────────────────────────────────────────────────
// AttendanceRecord — one row per attendance event
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roll: String,
    val name: String,
    val className: String = "",
    val date: String = "",           // "yyyy-MM-dd"
    val timestamp: Long,             // epoch millis
    val confidence: Float = 0f       // face-match confidence [0, 1]
)

// ─────────────────────────────────────────────────────────────────────────────
// User — authentication + preferences
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val password: String,
    val fullName: String,
    val className: String,
    val isTeacher: Boolean = false,
    val isDarkMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)