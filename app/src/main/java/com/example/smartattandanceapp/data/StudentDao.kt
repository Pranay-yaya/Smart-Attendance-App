package com.example.smartattendanceapp.data

import androidx.room.*

@Dao
interface StudentDao {

    // ── Students ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    // ── FIX: filter by owner so teachers only see their own students ──────────
    @Query("SELECT * FROM students WHERE createdBy = :userId ORDER BY name ASC")
    suspend fun getStudentsByOwner(userId: String): List<StudentEntity>

    // Kept for legacy — not used in main flow anymore
    @Query("SELECT * FROM students WHERE className = :className AND createdBy = :userId")
    suspend fun getStudentsByClass(className: String, userId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE roll = :roll AND createdBy = :userId LIMIT 1")
    suspend fun getStudentByRoll(roll: String, userId: String): StudentEntity?

    // ── Attendance records ────────────────────────────────────────────────────

    @Insert
    suspend fun insertAttendance(record: AttendanceRecordEntity)

    // ── FIX: each user only sees records they created ─────────────────────────
    @Query("SELECT * FROM attendance_records WHERE createdBy = :userId ORDER BY timestamp DESC")
    suspend fun getAttendanceByOwner(userId: String): List<AttendanceRecordEntity>

    // ── FIX: clear only THIS user's records, not everyone's ──────────────────
    @Query("DELETE FROM attendance_records WHERE createdBy = :userId")
    suspend fun deleteAttendanceByOwner(userId: String)

    // ── Users ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId AND password = :password LIMIT 1")
    suspend fun loginUser(userId: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)
}