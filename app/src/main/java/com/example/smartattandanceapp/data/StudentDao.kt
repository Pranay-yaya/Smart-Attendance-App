package com.example.smartattandanceapp.data


import androidx.room.*

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("SELECT * FROM students")
    suspend fun getAllStudents(): List<StudentEntity>

    @Insert
    suspend fun insertAttendance(record: AttendanceRecordEntity)

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    suspend fun getAllAttendance(): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE roll = :roll AND timestamp > :sinceMillis LIMIT 1")
    suspend fun getRecentAttendance(roll: String, sinceMillis: Long): AttendanceRecordEntity?
}