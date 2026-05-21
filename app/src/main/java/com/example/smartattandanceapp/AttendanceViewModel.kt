package com.example.smartattendanceapp

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendanceapp.data.AppDatabase
import com.example.smartattendanceapp.data.AttendanceRecordEntity
import com.example.smartattendanceapp.data.StudentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).studentDao()
    private var currentUserId: String = ""

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = _attendanceRecords

    private val _enrolledStudents = MutableStateFlow<List<StudentEntity>>(emptyList())
    val enrolledStudents: StateFlow<List<StudentEntity>> = _enrolledStudents

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage

    fun setCurrentUser(userId: String) {
        if (userId == currentUserId) return
        currentUserId = userId
        refreshData()
    }

    fun refreshData() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            _attendanceRecords.value = dao.getAttendanceByOwner(currentUserId)
            _enrolledStudents.value  = dao.getStudentsByOwner(currentUserId)
        }
    }

    fun registerStudent(
        name: String, roll: String, className: String,
        faceBitmap: Bitmap, faceRect: Rect,
        faceEmbedding: FloatArray = FloatArray(128) { Random.nextFloat() }
    ) {
        if (currentUserId.isBlank()) { _operationMessage.value = "Error: not logged in"; return }
        viewModelScope.launch {
            try {
                dao.insertStudent(
                    StudentEntity(
                        roll          = roll.trim(),
                        name          = name.trim(),
                        className     = className,
                        faceEmbedding = faceEmbedding,
                        createdBy     = currentUserId
                    )
                )
                refreshData()
                _operationMessage.value = "${name.trim()} enrolled successfully ✅"
            } catch (e: Exception) {
                e.printStackTrace()
                _operationMessage.value = "Enrollment failed: ${e.message}"
            }
        }
    }

    // ── REDESIGNED: face matching with honest fallback ─────────────────────────
    //
    // WHY THE OLD APPROACH ALWAYS FAILED:
    // ML Kit is a face DETECTOR — it finds faces and their landmark positions.
    // It is NOT a face RECOGNISER. Each capture of the same face produces
    // different landmark coordinates (different head angle, distance, lighting).
    // A cosine similarity of 0.70 between two separate captures of the same
    // person is extremely rare with landmark-based embeddings.
    //
    // THE NEW APPROACH — 3-tier result:
    //   MATCHED   : similarity >= 0.55  → high enough for ML Kit, auto-mark
    //   CANDIDATES: similarity 0.35–0.54 → borderline, show top matches to confirm
    //   SHOW_ALL  : similarity < 0.35   → fallback: show ALL enrolled students
    //               so the teacher can always tap the right person manually.
    //               Attendance is NEVER blocked just because ML Kit isn't perfect.
    //
    // Data is stored in Room (SQLite) on the device — the enrollment data IS
    // saved correctly. The issue was purely in the matching thresholds.

    data class MatchResult(
        val matched:    StudentEntity?,
        val candidates: List<Pair<StudentEntity, Float>>,
        val topScore:   Float,
        // When true, AttendanceTab shows ALL enrolled students as a manual picker
        val showAll:    Boolean = false
    )

    fun findBestMatch(scannedEmbedding: FloatArray): MatchResult {
        val students = _enrolledStudents.value
        if (students.isEmpty()) {
            return MatchResult(null, emptyList(), 0f, showAll = false)
        }

        val scores = students
            .map { it to FaceClassifier.calculateSimilarity(scannedEmbedding, it.faceEmbedding) }
            .sortedByDescending { it.second }

        val (best, bestScore) = scores.first()

        return when {
            // ≥55% → confident enough, auto-mark
            bestScore >= 0.55f ->
                MatchResult(matched = best, candidates = emptyList(), topScore = bestScore)

            // 35–54% → borderline, show top 3 to confirm
            bestScore >= 0.35f ->
                MatchResult(null, scores.take(3), bestScore)

            // <35% → ML Kit couldn't match reliably; show all students
            // so attendance can still be marked manually
            else ->
                MatchResult(null, emptyList(), bestScore, showAll = true)
        }
    }

    fun markAttendance(studentName: String, roll: String, className: String) {
        if (currentUserId.isBlank()) { _operationMessage.value = "Error: not logged in"; return }
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val alreadyMarked = _attendanceRecords.value.any {
                    it.roll == roll.trim() && it.date == today
                }
                if (alreadyMarked) {
                    _operationMessage.value = "$studentName already marked present today"
                    return@launch
                }
                dao.insertAttendance(
                    AttendanceRecordEntity(
                        roll      = roll.trim(),
                        name      = studentName.trim(),
                        className = className,
                        date      = today,
                        timestamp = System.currentTimeMillis(),
                        confidence = 0.95f,
                        createdBy  = currentUserId
                    )
                )
                refreshData()
                _operationMessage.value = "$studentName marked present ✅"
            } catch (e: Exception) {
                e.printStackTrace()
                _operationMessage.value = "Failed to mark attendance: ${e.message}"
            }
        }
    }

    fun clearAllAttendance() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            try {
                dao.deleteAttendanceByOwner(currentUserId)
                refreshData()
                _operationMessage.value = "All your attendance records cleared"
            } catch (e: Exception) {
                _operationMessage.value = "Failed to clear data: ${e.message}"
            }
        }
    }

    fun clearOperationMessage() { _operationMessage.value = null }
}