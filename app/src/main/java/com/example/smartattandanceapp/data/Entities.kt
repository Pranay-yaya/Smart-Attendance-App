package com.example.smartattendanceapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey @ColumnInfo(name = "roll") val roll: String,
    @ColumnInfo(name = "name")             val name: String,
    @ColumnInfo(name = "className")        val className: String,
    @ColumnInfo(name = "faceEmbedding")    val faceEmbedding: FloatArray,

    // ── FIX: data isolation ───────────────────────────────────────────────────
    // Each enrolled student belongs to the teacher who registered them.
    // Without this column every teacher saw every other teacher's students.
    @ColumnInfo(name = "createdBy", defaultValue = "")
    val createdBy: String = ""

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StudentEntity
        // Two students are the same if same roll AND same owner
        return roll == other.roll && createdBy == other.createdBy
    }
    override fun hashCode(): Int = 31 * roll.hashCode() + createdBy.hashCode()
}

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roll:      String,
    val name:      String,
    val className: String = "",
    val date:      String = "",
    val timestamp: Long,
    val confidence: Float = 0f,

    // ── FIX: data isolation ───────────────────────────────────────────────────
    // Records are tagged with the userId of the teacher who marked them.
    // Without this column all users shared the same attendance list.
    @ColumnInfo(name = "createdBy", defaultValue = "")
    val createdBy: String = ""
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId:    String,
    val password:   String,
    val fullName:   String,
    val className:  String,
    val isTeacher:  Boolean = false,
    val isDarkMode: Boolean = false,
    val createdAt:  Long = System.currentTimeMillis()
)