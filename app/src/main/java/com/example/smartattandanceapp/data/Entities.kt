package com.example.smartattandanceapp.data


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val roll: String,
    val name: String,
    val faceEmbedding: FloatArray
)

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roll: String,
    val name: String,
    val timestamp: Long,
    val confidence: Float = 0f
)