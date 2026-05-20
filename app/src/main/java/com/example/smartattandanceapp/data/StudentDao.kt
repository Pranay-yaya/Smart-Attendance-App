package com.example.smartattendanceapp.data

import androidx.room.*

@Dao
interface StudentDao {

    // ── Student operations ────────────────────────────────────────────────────

    /** Insert or replace a student (used during enrolment) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    /** Get all students in a specific class */
    @Query("SELECT * FROM students WHERE className = :className")
    suspend fun getStudentsByClass(className: String): List<StudentEntity>

    /** Get every enrolled student */
    @Query("SELECT * FROM students")
    suspend fun getAllStudents(): List<StudentEntity>

    // ── Attendance operations ─────────────────────────────────────────────────

    /** Insert a new attendance record */
    @Insert
    suspend fun insertAttendance(record: AttendanceRecordEntity)

    /** Get all attendance records, newest first */
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    suspend fun getAllAttendance(): List<AttendanceRecordEntity>

    /** Get attendance for a specific date */
    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getAttendanceByDate(date: String): List<AttendanceRecordEntity>

    /** Get attendance for a specific class */
    @Query("SELECT * FROM attendance_records WHERE className = :className ORDER BY timestamp DESC")
    suspend fun getAttendanceByClass(className: String): List<AttendanceRecordEntity>

    // ── User / auth operations ────────────────────────────────────────────────

    /** Insert or replace a user (used for signup and demo-account seeding) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /** Returns the user if userId + password match, null otherwise */
    @Query("SELECT * FROM users WHERE userId = :userId AND password = :password LIMIT 1")
    suspend fun loginUser(userId: String, password: String): UserEntity?

    /** Lookup a user by ID only (used to check for duplicates during signup) */
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    /** Update user preferences (dark mode, etc.) */
    @Update
    suspend fun updateUser(user: UserEntity)
}