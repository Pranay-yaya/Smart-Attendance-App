package com.example.smartattandanceapp

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattandanceapp.data.AppDatabase
import com.example.smartattandanceapp.data.AttendanceRecordEntity
import com.example.smartattandanceapp.data.StudentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sqrt

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.studentDao()

    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val students: StateFlow<List<StudentEntity>> = _students

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = _attendanceRecords

    private var faceClassifier: FaceClassifier? = null
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadModelAsync()
        refreshData()
    }

    private fun loadModelAsync() {
        viewModelScope.launch {
            try {
                faceClassifier = FaceClassifier(getApplication(), "mobile_face_net.tflite")
                _isModelLoaded.value = true
                Log.d("AttendanceVM", "✅ Model loaded successfully")
            } catch (e: Exception) {
                Log.e("AttendanceVM", "❌ Model load failed", e)
                _isModelLoaded.value = false
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _students.value = dao.getAllStudents()
            _attendanceRecords.value = dao.getAllAttendance()
        }
    }

    fun registerStudent(name: String, roll: String, faceBitmap: Bitmap, faceRect: Rect, onSuccess: () -> Unit) {
        if (!_isModelLoaded.value) return
        if (name.isBlank() || roll.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cropped = cropAndScaleFace(faceBitmap, faceRect)
                val rawEmbedding = faceClassifier?.recognizeFace(cropped) ?: throw Exception("Recognition failed")

                // 🔑 CRITICAL: Normalize before saving
                val normalizedEmbedding = normalize(rawEmbedding)

                dao.insertStudent(StudentEntity(roll.trim(), name.trim(), normalizedEmbedding))
                refreshData()
                onSuccess()
            } catch (e: Exception) {
                Log.e("AttendanceVM", "Registration error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAttendance(faceBitmap: Bitmap, faceRect: Rect, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (!_isModelLoaded.value) {
            onError("Model not loaded")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cropped = cropAndScaleFace(faceBitmap, faceRect)
                val rawEmbedding = faceClassifier?.recognizeFace(cropped) ?: throw Exception("Recognition failed")
                val normalizedQuery = normalize(rawEmbedding)

                val allStudents = dao.getAllStudents()
                if (allStudents.isEmpty()) {
                    _isLoading.value = false
                    onError("No students registered yet")
                    return@launch
                }

                var bestMatch: StudentEntity? = null
                var maxSimilarity = 0f
                val THRESHOLD = 0.82f

                for (student in allStudents) {
                    val sim = cosineSimilarity(normalizedQuery, student.faceEmbedding)
                    if (sim > maxSimilarity) {
                        maxSimilarity = sim
                        bestMatch = student
                    }
                }

                _isLoading.value = false

                if (bestMatch != null && maxSimilarity >= THRESHOLD) {
                    // 🔑 Prevent duplicate marks within 5 minutes
                    val recent = dao.getRecentAttendance(bestMatch.roll, System.currentTimeMillis() - 300000)
                    if (recent != null) {
                        onError("Already marked for ${bestMatch.name} recently")
                        return@launch
                    }

                    val record = AttendanceRecordEntity(
                        roll = bestMatch.roll,
                        name = bestMatch.name,
                        timestamp = System.currentTimeMillis(),
                        confidence = maxSimilarity
                    )
                    dao.insertAttendance(record)
                    refreshData()
                    onSuccess("${bestMatch.name} (${(maxSimilarity * 100).toInt()}%)")
                } else {
                    onError("No match found (Best: ${bestMatch?.name ?: "None"} - ${(maxSimilarity * 100).toInt()}%)")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                onError("Error: ${e.message}")
            }
        }
    }

    private fun cropAndScaleFace(bitmap: Bitmap, rect: Rect): Bitmap {
        val pad = (rect.width() * 0.2).toInt()
        val l = (rect.left - pad).coerceAtLeast(0)
        val t = (rect.top - pad).coerceAtLeast(0)
        val r = (rect.right + pad).coerceAtMost(bitmap.width)
        val b = (rect.bottom + pad).coerceAtMost(bitmap.height)
        val w = r - l
        val h = b - t
        return try {
            val cropped = Bitmap.createBitmap(bitmap, l, t, w, h)
            Bitmap.createScaledBitmap(cropped, 112, 112, true)
        } catch (e: Exception) {
            Bitmap.createScaledBitmap(bitmap, 112, 112, true)
        }
    }

    private fun normalize(v: FloatArray): FloatArray {
        var norm = 0f
        for (x in v) norm += x * x
        norm = sqrt(norm)
        return if (norm == 0f) v else v.map { it / norm }.toFloatArray()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]
        }
        val den = sqrt(na) * sqrt(nb)
        return if (den == 0f) 0f else dot / den
    }
}