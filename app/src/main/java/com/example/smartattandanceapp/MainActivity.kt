package com.example.smartattendanceapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.example.smartattendanceapp.data.AttendanceRecordEntity
import com.example.smartattendanceapp.data.StudentEntity
import java.text.SimpleDateFormat
import java.util.*

private val PresentGreen = Color(0xFF00C853)

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val attendanceViewModel: AttendanceViewModel by viewModels()
    private lateinit var faceClassifier: FaceClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        faceClassifier = FaceClassifier(this)

        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#0A0E1A"))
        )

        val incomingUserId = intent.getStringExtra("userId") ?: ""
        if (incomingUserId.isNotBlank()) {
            authViewModel.restoreSession(incomingUserId)
        }

        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                faceClassifier.close()
            }
        })

        setContent {
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
            val isLoading  by authViewModel.isLoading.collectAsState()

            when {
                isLoading  -> SplashScreen()
                isLoggedIn -> MainScreen(authViewModel, attendanceViewModel, faceClassifier)
                else -> {
                    LaunchedEffect(Unit) {
                        startActivity(
                            Intent(this@MainActivity, LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                    SplashScreen()
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier.fillMaxSize().background(theme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(theme.accent2, theme.accent))),
                contentAlignment = Alignment.Center
            ) { Text("👁", fontSize = 32.sp) }
            Spacer(Modifier.height(16.dp))
            Text("AttendX AI", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = theme.accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun MainScreen(authVM: AuthViewModel, attendVM: AttendanceViewModel, faceClassifier: FaceClassifier) {
    val user       by authVM.currentUser.collectAsState()
    val isTeacher  = user?.isTeacher ?: false
    val isLightMode = user?.isDarkMode ?: false
    val theme       = if (isLightMode) LightColors else DarkColors

    // ── Tab list changes based on role ─────────────────────────────────────────
    // TEACHER → 4 tabs: Attendance, Enroll, Stats, Settings
    // STUDENT → 3 tabs: My Attendance, Stats, Settings  (no Enroll tab)
    val tabs = if (isTeacher) listOf(
        Triple(Icons.Outlined.FactCheck, Icons.Default.FactCheck, "Attendance"),
        Triple(Icons.Outlined.PersonAdd, Icons.Default.PersonAdd, "Enroll"),
        Triple(Icons.Outlined.Analytics, Icons.Default.Analytics, "Stats"),
        Triple(Icons.Outlined.Settings,  Icons.Default.Settings,  "Settings")
    ) else listOf(
        Triple(Icons.Outlined.FactCheck, Icons.Default.FactCheck, "My Attendance"),
        Triple(Icons.Outlined.Analytics, Icons.Default.Analytics, "Stats"),
        Triple(Icons.Outlined.Settings,  Icons.Default.Settings,  "Settings")
    )

    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(user?.userId) {
        user?.userId?.let { uid -> attendVM.setCurrentUser(uid) }
    }

    // ── Gradient backgrounds ───────────────────────────────────────────────────
    val bgGradient = if (isLightMode)
        Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFF5F7FA), Color(0xFFEEF2FF)))
    else
        Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0F172A), Color(0xFF0A0E1A)))

    val topBarGradient = if (isLightMode)
        Brush.horizontalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF0F4FF)))
    else
        Brush.horizontalGradient(listOf(Color(0xFF111827), Color(0xFF0F172A)))

    CompositionLocalProvider(LocalAppTheme provides theme) {
        Column(modifier = Modifier.fillMaxSize().background(bgGradient)) {

            // ── Top bar ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(topBarGradient)
                    .border(width = 1.dp, color = theme.cardBorder, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(theme.accent2, theme.accent))),
                        contentAlignment = Alignment.Center
                    ) { Text("👁", fontSize = 18.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AttendX AI", color = theme.textPrimary, fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
                        // Show role badge in top bar subtitle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isTeacher) theme.warning.copy(0.2f)
                                        else theme.success.copy(0.2f)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    if (isTeacher) "Teacher" else "Student",
                                    color = if (isTeacher) theme.warning else theme.success,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                            Text(
                                user?.className?.ifBlank { "" } ?: "",
                                color = theme.textMuted, fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // User avatar badge
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(theme.background)
                            .border(1.dp, theme.cardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(theme.accent2, theme.accent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user?.fullName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            user?.fullName?.split(" ")?.firstOrNull() ?: "User",
                            color = theme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "tab_content"
                ) { tab ->
                    if (isTeacher) {
                        when (tab) {
                            0 -> TeacherAttendanceTab(attendVM, faceClassifier)
                            1 -> RegisterTab(attendVM, user?.className ?: "", faceClassifier)
                            2 -> StatisticsScreen(attendVM, isLightMode)
                            3 -> SettingsScreen(authVM, attendVM, onLogout = {})
                        }
                    } else {
                        when (tab) {
                            0 -> StudentAttendanceTab(attendVM, user)
                            1 -> StatisticsScreen(attendVM, isLightMode)
                            2 -> SettingsScreen(authVM, attendVM, onLogout = {})
                        }
                    }
                }
            }

            // ── Bottom navigation ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        if (isLightMode) Brush.horizontalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF0F4FF)))
                        else Brush.horizontalGradient(listOf(Color(0xFF111827), Color(0xFF0F172A)))
                    )
                    .border(1.dp, theme.cardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, (outlineIcon, filledIcon, label) ->
                    val selected = selectedTab == index
                    val iconColor by animateColorAsState(
                        if (selected) theme.accent else theme.textMuted, label = "nav_$index"
                    )
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = index }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(theme.accent.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(filledIcon, null, tint = iconColor, modifier = Modifier.size(20.dp)) }
                        } else {
                            Icon(outlineIcon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = iconColor, fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TEACHER ATTENDANCE TAB
// Full flow: scan face → show student list → tap to mark
// ═══════════════════════════════════════════════════════════════════════════════
sealed class ScanState {
    object Idle     : ScanState()
    object Scanning : ScanState()
    object ShowAll  : ScanState()
    object NoFace   : ScanState()
}

@Composable
fun TeacherAttendanceTab(vm: AttendanceViewModel, faceClassifier: FaceClassifier) {
    val theme    = LocalAppTheme.current
    val records  by vm.attendanceRecords.collectAsState()
    val enrolled by vm.enrolledStudents.collectAsState()
    var scanState by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    val scope    = rememberCoroutineScope()
    val context  = LocalContext.current

    // ── Gradient background ────────────────────────────────────────────────────
    val bgGradient = if (theme.isLight)
        Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFF5F7FA)))
    else
        Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1321), Color(0xFF0A0E1A)))

    if (scanState == ScanState.Scanning) {
        CameraScreen(
            title = "Scan Face for Attendance",
            onImageCaptured = { bitmap ->
                scope.launch {
                    val faceFound = withContext(Dispatchers.IO) { faceClassifier.detectFace(bitmap) }
                    scanState = if (faceFound) ScanState.ShowAll else ScanState.NoFace
                }
            },
            onDismiss = { scanState = ScanState.Idle }
        )
        return
    }

    if (scanState == ScanState.NoFace) {
        Box(modifier = Modifier.fillMaxSize().background(bgGradient), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("😕", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text("No Face Detected", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("Make sure your face is clearly visible, well-lit, and facing the camera.",
                    color = theme.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                Spacer(Modifier.height(28.dp))
                Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(theme.accent2, theme.accent)))
                    .clickable { scanState = ScanState.Scanning }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                    .border(1.dp, theme.cardBorder, RoundedCornerShape(14.dp))
                    .clickable { scanState = ScanState.Idle }, contentAlignment = Alignment.Center) {
                    Text("Cancel", color = theme.textMuted, fontSize = 14.sp)
                }
            }
        }
        return
    }

    // ── Student list picker ────────────────────────────────────────────────────
    if (scanState == ScanState.ShowAll) {
        Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (enrolled.isEmpty()) "No Students Enrolled" else "Mark Attendance",
                            color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (enrolled.isEmpty()) "Go to Enroll tab first"
                            else "✓ Face detected — tap the student to mark present",
                            color = if (enrolled.isEmpty()) theme.textMuted else theme.success,
                            fontSize = 13.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(theme.cardBorder).clickable { scanState = ScanState.Idle },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Close, null, tint = theme.textMuted, modifier = Modifier.size(18.dp)) }
                }

                Spacer(Modifier.height(16.dp))

                if (enrolled.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)
                        .clip(RoundedCornerShape(16.dp)).background(theme.card)
                        .border(1.dp, theme.cardBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text("📋", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No students enrolled yet", color = theme.textMuted, fontSize = 14.sp)
                        }
                    }
                } else {
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    // ── DUPLICATE ATTENDANCE STATE for the list picker ─────────
                    var duplicateStudentName by remember { mutableStateOf<String?>(null) }

                    if (duplicateStudentName != null) {
                        AlertDialog(
                            onDismissRequest = { duplicateStudentName = null },
                            containerColor   = theme.card,
                            titleContentColor = theme.textPrimary,
                            textContentColor  = theme.textMuted,
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️", fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Already Marked", fontWeight = FontWeight.Bold)
                                }
                            },
                            text = { Text("${duplicateStudentName}'s attendance is already marked present for today.") },
                            confirmButton = {
                                Button(
                                    onClick = { duplicateStudentName = null },
                                    colors  = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                ) { Text("OK", color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                        )
                    }

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(enrolled) { student ->
                            val alreadyMarked = records.any { it.roll == student.roll && it.date == todayDate }
                            StudentPickerRow(
                                student       = student,
                                alreadyMarked = alreadyMarked,
                                theme         = theme,
                                onClick = {
                                    if (alreadyMarked) {
                                        duplicateStudentName = student.name
                                    } else {
                                        vm.markAttendance(student.name, student.roll, student.className)
                                        Toast.makeText(context, "✅ ${student.name} marked Present!", Toast.LENGTH_SHORT).show()
                                        scanState = ScanState.Idle
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(theme.accent2, theme.accent)))
                    .clickable { scanState = ScanState.Scanning }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Another Face", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        return
    }

    // ── Idle: scan card + recent logs ──────────────────────────────────────────
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(bgGradient).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item { ScanFaceCard { scanState = ScanState.Scanning } }

        item {
            val todayCount = records.count {
                it.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMiniCard("Today",   "$todayCount",       theme.success, Icons.Default.CheckCircle, Modifier.weight(1f))
                StatMiniCard("Total",   "${records.size}",   theme.accent,  Icons.Default.People,      Modifier.weight(1f))
                StatMiniCard("Classes", "${records.map { it.className }.distinct().size}", theme.accent2, Icons.Default.School, Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Logs", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (records.isNotEmpty()) Text("${records.size} entries", color = theme.textMuted, fontSize = 12.sp)
            }
        }

        if (records.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
                    .background(theme.card).border(1.dp, theme.cardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No attendance records yet", color = theme.textMuted, fontSize = 14.sp)
                        Text("Scan a face to get started", color = theme.textMuted.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(records) { record -> AttendanceLogItem(record) }
        }
    }
}

// ── Reusable student row ───────────────────────────────────────────────────────
@Composable
fun StudentPickerRow(
    student: StudentEntity,
    alreadyMarked: Boolean,
    theme: AppColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (alreadyMarked)
                    Brush.linearGradient(listOf(theme.success.copy(0.1f), theme.success.copy(0.05f)))
                else
                    Brush.linearGradient(listOf(theme.card, theme.card))
            )
            .border(1.dp, if (alreadyMarked) theme.success.copy(0.4f) else theme.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .background(
                    if (alreadyMarked)
                        Brush.linearGradient(listOf(theme.success.copy(0.6f), theme.success.copy(0.4f)))
                    else
                        Brush.linearGradient(listOf(theme.accent2.copy(0.7f), theme.accent.copy(0.7f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (alreadyMarked)
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
            else
                Text(student.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(student.name, color = if (alreadyMarked) theme.success else theme.textPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Roll: ${student.roll}  ·  ${student.className}", color = theme.textMuted, fontSize = 12.sp)
        }
        if (alreadyMarked)
            Text("Present ✓", color = theme.success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        else
            Icon(Icons.Default.TouchApp, null, tint = theme.textMuted.copy(0.5f), modifier = Modifier.size(20.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STUDENT ATTENDANCE TAB
// Students can ONLY mark their own attendance — no student list visible
// Duplicate attendance shows a popup dialog
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun StudentAttendanceTab(vm: AttendanceViewModel, user: com.example.smartattendanceapp.data.UserEntity?) {
    val theme   = LocalAppTheme.current
    val records by vm.attendanceRecords.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var showCamera        by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var isProcessing      by remember { mutableStateOf(false) }

    val today     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val markedToday = records.any { it.roll == user?.userId && it.date == today }

    // ── Gradient background ────────────────────────────────────────────────────
    val bgGradient = if (theme.isLight)
        Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFF5F7FA), Color(0xFFE8F5E9)))
    else
        Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1321), Color(0xFF061108)))

    // ── Duplicate attendance popup ─────────────────────────────────────────────
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest  = { showDuplicateDialog = false },
            containerColor    = theme.card,
            titleContentColor = theme.textPrimary,
            textContentColor  = theme.textMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Already Marked!", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Your attendance for today has already been marked present.",
                        fontSize = 14.sp, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(theme.success.copy(0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Present — $today", color = theme.success, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDuplicateDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = theme.accent)
                ) { Text("OK, Got It", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showCamera) {
        CameraScreen(
            title = "Verify Your Face",
            onImageCaptured = { bitmap ->
                isProcessing = true
                scope.launch {
                    val faceFound = withContext(Dispatchers.IO) {
                        com.example.smartattendanceapp.FaceClassifier(context).detectFace(bitmap)
                    }
                    isProcessing = false
                    showCamera   = false

                    if (!faceFound) {
                        Toast.makeText(context, "No face detected — please try again", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Check duplicate BEFORE marking
                    val alreadyMarked = records.any { it.roll == user?.userId && it.date == today }
                    if (alreadyMarked) {
                        showDuplicateDialog = true
                    } else {
                        vm.markAttendance(
                            studentName = user?.fullName ?: "Unknown",
                            roll        = user?.userId  ?: "",
                            className   = user?.className ?: ""
                        )
                        Toast.makeText(context, "✅ Attendance marked for today!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showCamera = false; isProcessing = false }
        )
        if (isProcessing) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = theme.accent)
                    Spacer(Modifier.height(12.dp))
                    Text("Verifying face...", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(bgGradient).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {

        // ── Student greeting card ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(theme.accent2.copy(0.8f), theme.accent.copy(0.8f))))
                    .padding(24.dp)
            ) {
                Column {
                    Text("Hello, ${user?.fullName?.split(" ")?.firstOrNull() ?: "Student"} 👋",
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date()),
                        color = Color.White.copy(0.8f), fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    // Status pill
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.2f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                            .background(if (markedToday) Color(0xFF00FF88) else Color.White.copy(0.5f)))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (markedToday) "Present today ✓" else "Not marked yet for today",
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── Mark attendance button ─────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .border(1.dp, theme.cardBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mark Your Attendance", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (markedToday) "You've already marked attendance today"
                        else "Tap below and look at the camera to verify",
                        color = theme.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (markedToday)
                                    Brush.linearGradient(listOf(theme.success.copy(0.6f), theme.success.copy(0.4f)))
                                else
                                    Brush.linearGradient(listOf(theme.accent2, theme.accent))
                            )
                            .clickable {
                                if (markedToday) {
                                    // Show duplicate dialog immediately — no need to open camera
                                    showDuplicateDialog = true
                                } else {
                                    showCamera = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (markedToday) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                null, tint = Color.White, modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (markedToday) "Already Marked Present ✓" else "Mark My Attendance",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Stats row ──────────────────────────────────────────────────────────
        item {
            val totalDays    = records.map { it.date }.distinct().size
            val thisMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val monthCount   = records.count { it.date.startsWith(thisMonthKey) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMiniCard("Total Days",    "$totalDays",  theme.accent,  Icons.Default.CalendarMonth, Modifier.weight(1f))
                StatMiniCard("This Month",    "$monthCount", theme.accent2, Icons.Default.DateRange,     Modifier.weight(1f))
                StatMiniCard("Today",         if (markedToday) "✓" else "–", theme.success, Icons.Default.CheckCircle, Modifier.weight(1f))
            }
        }

        // ── Attendance history ─────────────────────────────────────────────────
        item {
            Text("My Attendance History", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                        .clip(RoundedCornerShape(16.dp)).background(theme.card)
                        .border(1.dp, theme.cardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No attendance records yet", color = theme.textMuted, fontSize = 14.sp)
                }
            }
        } else {
            items(records) { record -> AttendanceLogItem(record) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// REGISTER TAB  (Teacher only)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterTab(vm: AttendanceViewModel, userClass: String, faceClassifier: FaceClassifier) {
    val theme = LocalAppTheme.current
    var name        by remember { mutableStateOf("") }
    var roll        by remember { mutableStateOf("") }
    var enrolled    by remember { mutableStateOf<String?>(null) }
    var showCamera  by remember { mutableStateOf(false) }
    val valid       = name.isNotBlank() && roll.isNotBlank()
    var pendingName by remember { mutableStateOf("") }
    var pendingRoll by remember { mutableStateOf("") }
    val scope       = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    val bgGradient = if (theme.isLight)
        Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFF5F7FA)))
    else
        Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1321), Color(0xFF0A0E1A)))

    if (showCamera) {
        CameraScreen(
            title = "Capture Face for Enrollment",
            onImageCaptured = { bmp ->
                if (pendingName.isNotBlank() && pendingRoll.isNotBlank()) {
                    isProcessing = true
                    scope.launch {
                        val realEmbedding = withContext(Dispatchers.IO) { faceClassifier.recognizeFace(bmp) }
                        vm.registerStudent(
                            name          = pendingName,
                            roll          = pendingRoll,
                            className     = userClass,
                            faceBitmap    = bmp,
                            faceRect      = android.graphics.Rect(0, 0, bmp.width, bmp.height),
                            faceEmbedding = realEmbedding
                        )
                        enrolled = pendingName; name = ""; roll = ""; isProcessing = false; showCamera = false
                    }
                } else { showCamera = false }
            },
            onDismiss = { showCamera = false }
        )
        if (isProcessing) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = theme.accent)
                    Spacer(Modifier.height(12.dp))
                    Text("Processing face...", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(bgGradient).padding(20.dp)) {
        Text("Enroll Student", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("Capture face data for AI recognition", color = theme.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        AnimatedVisibility(visible = enrolled != null) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(theme.success.copy(0.12f)).border(1.dp, theme.success.copy(0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✅", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Enrolled Successfully!", color = theme.success, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$enrolled has been registered", color = theme.success.copy(0.7f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(theme.card).border(1.dp, theme.cardBorder, RoundedCornerShape(20.dp)).padding(20.dp)) {
            Column {
                Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(theme.accent2.copy(0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, null, tint = theme.accent2, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(userClass.ifBlank { "No class assigned" }, color = theme.accent2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(18.dp))
                DarkTextField(Icons.Outlined.Person, name, "Student Full Name") { name = it; enrolled = null }
                Spacer(Modifier.height(14.dp))
                DarkTextField(Icons.Outlined.Tag, roll, "Roll Number / Student ID") { roll = it; enrolled = null }
                Spacer(Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (valid) Brush.linearGradient(listOf(theme.accent2, theme.accent)) else Brush.linearGradient(listOf(theme.cardBorder, theme.cardBorder)))
                    .clickable(enabled = valid) { pendingName = name.trim(); pendingRoll = roll.trim(); enrolled = null; showCamera = true },
                    contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Capture Face & Enroll", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(theme.card)
            .border(1.dp, theme.cardBorder, RoundedCornerShape(12.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = theme.warning, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text("Good lighting + frontal face position gives best accuracy.", color = theme.textMuted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

// ── Shared components ──────────────────────────────────────────────────────────
@Composable
fun ScanFaceCard(onScan: () -> Unit) {
    val theme = LocalAppTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(theme.card, theme.accent2.copy(0.05f))))
        .border(1.dp, theme.cardBorder, RoundedCornerShape(24.dp))) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(theme.accent.copy(0.07f), Color.Transparent), radius = 300f)))
        Row(modifier = Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Face Scanner", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Point camera at face\nfor instant recognition", color = theme.textMuted, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(theme.accent2, theme.accent)))
                    .clickable { onScan() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Now", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(90.dp).rotate(rotation).border(
                    2.dp, Brush.sweepGradient(listOf(theme.accent, theme.accent.copy(0f))), CircleShape))
                Icon(Icons.Default.Face, null, tint = theme.textPrimary.copy(0.4f), modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun StatMiniCard(label: String, value: String, tint: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp))
        .background(Brush.linearGradient(listOf(theme.card, tint.copy(0.05f))))
        .border(1.dp, theme.cardBorder, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Column {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label,  color = theme.textMuted,   fontSize = 11.sp)
        }
    }
}

@Composable
fun AttendanceLogItem(r: AttendanceRecordEntity) {
    val theme = LocalAppTheme.current
    val time = remember(r.timestamp) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(r.timestamp)) }
    val date = remember(r.timestamp) { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(r.timestamp)) }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(theme.card).border(1.dp, theme.cardBorder, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(theme.accent2.copy(0.6f), theme.accent.copy(0.6f)))),
            contentAlignment = Alignment.Center) {
            Text(r.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(r.name, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("ID: ${r.roll}${if (r.className.isNotBlank()) "  ·  ${r.className}" else ""}", color = theme.textMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(date, color = theme.textMuted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PresentGreen.copy(0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(PresentGreen))
                Spacer(Modifier.width(4.dp))
                Text("Present", color = PresentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DarkTextField(icon: ImageVector, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    val theme = LocalAppTheme.current
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (focused) theme.accent else theme.cardBorder, label = "border")
    Row(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp))
        .background(theme.background).border(1.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (focused) theme.accent else theme.textMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = theme.textMuted, fontSize = 14.sp)
            BasicTextField(value = value, onValueChange = onValueChange,
                textStyle = TextStyle(color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(theme.accent),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused })
        }
    }
}