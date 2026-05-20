package com.example.smartattendanceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendanceapp.data.AppDatabase
import com.example.smartattendanceapp.data.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).studentDao()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Called by MainActivity immediately after it is created, using the userId
     * that LoginActivity passed via Intent.putExtra("userId", ...).
     *
     * WHY THIS EXISTS:
     * Each Activity gets its own ViewModel instance. When LoginActivity launches
     * MainActivity, MainActivity's AuthViewModel starts completely fresh with
     * isLoggedIn = false. Without restoreSession(), the LaunchedEffect in
     * MainActivity fires immediately and sends the user back to LoginActivity,
     * causing the white-flash navigation loop.
     *
     * restoreSession() loads the user from the DB and sets isLoggedIn = true
     * before Compose even renders the first frame (called before setContent).
     * isLoading = true during the DB call, so SplashScreen() is shown briefly.
     */
    fun restoreSession(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = dao.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
                _isLoggedIn.value = true
            }
            _isLoading.value = false
        }
    }

    fun login(userId: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val user = dao.loginUser(userId, password)
            if (user != null) {
                _currentUser.value = user
                _isLoggedIn.value = true
            } else {
                _errorMessage.value = "Invalid User ID or Password"
            }
            _isLoading.value = false
        }
    }

    fun signup(
        userId: String,
        password: String,
        fullName: String,
        className: String,
        isTeacher: Boolean,
        isDarkMode: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val existing = dao.getUserById(userId)
            if (existing != null) {
                _errorMessage.value = "User ID already exists"
            } else {
                val newUser = UserEntity(userId, password, fullName, className, isTeacher, isDarkMode)
                dao.insertUser(newUser)
                _currentUser.value = newUser
                _isLoggedIn.value = true
            }
            _isLoading.value = false
        }
    }

    fun updateUserTheme(isDarkMode: Boolean) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val updated = user.copy(isDarkMode = isDarkMode)
                dao.updateUser(updated)
                _currentUser.value = updated
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val updated = user.copy(password = newPassword)
                dao.updateUser(updated)
                _currentUser.value = updated
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }

    fun clearError() { _errorMessage.value = null }
}