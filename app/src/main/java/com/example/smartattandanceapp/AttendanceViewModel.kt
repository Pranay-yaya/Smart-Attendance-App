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

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = _attendanceRecords

    private val _enrolledStudents = MutableStateFlow<List<StudentEntity>>(emptyList())
    val enrolledStudents: StateFlow<List<StudentEntity>> = _enrolledStudents

    private val _isModelLoaded = MutableStateFlow(true)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded

    init { refreshData() }

    fun refreshData() {
        viewModelScope.launch {
            _attendanceRecords.value = dao.getAllAttendance()
            _enrolledStudents.value = dao.getAllStudents()
        }
    }

    fun registerStudent(name: String, roll: String, className: String, faceBitmap: Bitmap, faceRect: Rect) {
        viewModelScope.launch {
            try {
                val dummyEmbedding = FloatArray(128) { Random.nextFloat() }
                dao.insertStudent(StudentEntity(roll.trim(), name.trim(), className, dummyEmbedding))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun markAttendance(studentName: String, roll: String, className: String) {
        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dao.insertAttendance(
                    AttendanceRecordEntity(
                        roll = roll, name = studentName, className = className,
                        date = dateFormat.format(Date()), timestamp = System.currentTimeMillis(), confidence = 0.95f
                    )
                )
                refreshData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}