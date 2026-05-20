package com.example.smartattendanceapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartattendanceapp.data.AttendanceRecordEntity
import com.example.smartattendanceapp.data.StudentEntity
import java.text.SimpleDateFormat
import java.util.*

private val ElectricBlue = Color(0xFF00D4FF)
private val NeonPurple   = Color(0xFF7B5EFF)
private val DeepNavy     = Color(0xFF0A0E1A)
private val CardDark     = Color(0xFF111827)
private val CardBorder   = Color(0xFF1E2D40)
private val TextWhite    = Color(0xFFE8EAED)
private val TextMuted    = Color(0xFF6B7A99)
private val SuccessGreen = Color(0xFF00E676)
private val WarnAmber    = Color(0xFFFFAB40)
private val PresentGreen = Color(0xFF00C853)
private val ErrorRed     = Color(0xFFFF5252)

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val attendanceViewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── FIX 1: DARK WINDOW BACKGROUND ─────────────────────────────────────
        // The app theme has a white windowBackground. Android paints that colour
        // before Compose draws its first frame, producing a white blink every time
        // this activity starts. Setting the window background to the app's deep
        // navy here (before setContent) means the window is already dark when it
        // first becomes visible.
        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#0A0E1A"))
        )

        // ── FIX 2: RESTORE SESSION FROM INTENT ────────────────────────────────
        // Each Activity gets its own ViewModel instance. When LoginActivity
        // launches MainActivity, MainActivity's AuthViewModel starts fresh with
        // isLoggedIn = false.
        //
        // The OLD buggy flow:
        //   LoginActivity login ──► starts MainActivity ──► finish()
        //   MainActivity fresh AuthViewModel: isLoggedIn = false
        //   LaunchedEffect fires immediately ──► starts LoginActivity ──► finish()
        //   White flash ──► LoginActivity shown ──► user confused
        //
        // THE FIX:
        //   LoginActivity puts the userId in the Intent (see LoginActivity.kt).
        //   Here we read it and call authViewModel.restoreSession(userId).
        //   restoreSession() loads the user from Room DB, sets isLoggedIn = true,
        //   and sets isLoading = true while it's working.
        //   The setContent block shows SplashScreen() while isLoading=true,
        //   then MainScreen() once isLoggedIn=true — no redirect happens.
        val incomingUserId = intent.getStringExtra("userId") ?: ""
        if (incomingUserId.isNotBlank()) {
            authViewModel.restoreSession(incomingUserId)
        }

        setContent {
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
            val isLoading  by authViewModel.isLoading.collectAsState()

            when {
                // Still loading the session → show splash (dark background already set)
                isLoading -> SplashScreen()

                // Session confirmed → show main UI
                isLoggedIn -> MainScreen(authViewModel, attendanceViewModel)

                // Not loading and not logged in → user explicitly logged out
                // (this path is ONLY reached after isLoading finishes as false,
                //  i.e. session restore failed or user called logout())
                else -> {
                    LaunchedEffect(Unit) {
                        startActivity(
                            Intent(this@MainActivity, LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                    // Keep showing splash while the Intent fires to avoid white flash
                    SplashScreen()
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(NeonPurple, ElectricBlue))),
                contentAlignment = Alignment.Center
            ) { Text("👁", fontSize = 32.sp) }
            Spacer(Modifier.height(16.dp))
            Text("AttendX AI", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = ElectricBlue, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun MainScreen(authVM: AuthViewModel, attendVM: AttendanceViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val user by authVM.currentUser.collectAsState()

    val tabs = listOf(
        Triple(Icons.Outlined.FactCheck, Icons.Default.FactCheck, "Attendance"),
        Triple(Icons.Outlined.PersonAdd, Icons.Default.PersonAdd, "Enroll"),
        Triple(Icons.Outlined.Analytics, Icons.Default.Analytics, "Stats"),
        Triple(Icons.Outlined.Settings, Icons.Default.Settings, "Settings")
    )

    Column(modifier = Modifier.fillMaxSize().background(DeepNavy)) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark)
                .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(NeonPurple, ElectricBlue))),
                    contentAlignment = Alignment.Center
                ) { Text("👁", fontSize = 18.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "AttendX AI", color = TextWhite, fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp
                    )
                    Text(
                        user?.className?.ifBlank { "Dashboard" } ?: "Dashboard",
                        color = TextMuted, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepNavy)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonPurple, ElectricBlue))),
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
                        color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> AttendanceTab(attendVM)
                    1 -> RegisterTab(attendVM, user?.className ?: "")
                    2 -> StatisticsScreen(attendVM)
                    3 -> SettingsScreen(authVM, onLogout = {})
                }
            }
        }

        // ── Bottom navigation ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, (outlineIcon, filledIcon, label) ->
                val selected = selectedTab == index
                val iconColor by animateColorAsState(
                    if (selected) ElectricBlue else TextMuted, label = "nav_$index"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedTab = index }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(ElectricBlue.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(filledIcon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Icon(outlineIcon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label, color = iconColor, fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ATTENDANCE TAB
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun AttendanceTab(vm: AttendanceViewModel) {
    val records by vm.attendanceRecords.collectAsState()
    var showCamera    by remember { mutableStateOf(false) }
    // ── FIXED: after capture, show a "Mark Present" form ─────────────────────
    // Bug: old code called Toast only — markAttendance() was NEVER called.
    // Now: capture sets showMarkForm = true, form calls vm.markAttendance().
    var showMarkForm  by remember { mutableStateOf(false) }
    var markName      by remember { mutableStateOf("") }
    var markRoll      by remember { mutableStateOf("") }
    var markClass     by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showCamera) {
        CameraScreen(
            onImageCaptured = { _ ->
                // Face was captured — open the "Mark Present" form
                showCamera   = false
                showMarkForm = true
            },
            onDismiss = { showCamera = false }
        )
        return
    }

    // ── Student picker after face scan ────────────────────────────────────────
    if (showMarkForm) {
        val enrolled by vm.enrolledStudents.collectAsState()

        Box(
            modifier = Modifier.fillMaxSize().background(DeepNavy)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Who is this?", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Tap a student to mark present", color = TextMuted, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(CardBorder).clickable {
                                markName = ""; markRoll = ""; showMarkForm = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (enrolled.isEmpty()) {
                    // No students enrolled yet — fall back to manual entry
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)).background(CardDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("No students enrolled yet", color = TextMuted, fontSize = 14.sp)
                            Text("Go to Enroll tab to register students first", color = TextMuted.copy(0.6f), fontSize = 12.sp)
                        }
                    }
                } else {
                    // Show enrolled students as tappable cards
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(enrolled) { student ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp)).background(CardDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        // One tap → mark present immediately, no re-typing
                                        vm.markAttendance(
                                            studentName = student.name,
                                            roll        = student.roll,
                                            className   = student.className
                                        )
                                        Toast.makeText(
                                            context,
                                            "✅ ${student.name} marked Present!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showMarkForm = false
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(NeonPurple.copy(0.6f), ElectricBlue.copy(0.6f)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        student.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(student.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Roll: ${student.roll}  ·  ${student.className}", color = TextMuted, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen.copy(0.4f), modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepNavy).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item { ScanFaceCard { showCamera = true } }

        item {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayCount = records.count { it.date == today }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMiniCard("Today",   "$todayCount",              SuccessGreen, Icons.Default.CheckCircle, Modifier.weight(1f))
                StatMiniCard("Total",   "${records.size}",          ElectricBlue, Icons.Default.People,      Modifier.weight(1f))
                StatMiniCard("Classes", "${records.map { it.className }.distinct().size}", NeonPurple, Icons.Default.School, Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Logs", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (records.isNotEmpty()) Text("${records.size} entries", color = TextMuted, fontSize = 12.sp)
            }
        }

        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                        .clip(RoundedCornerShape(16.dp)).background(CardDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No attendance records yet", color = TextMuted, fontSize = 14.sp)
                        Text("Scan a face to get started", color = TextMuted.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(records) { record -> AttendanceLogItem(record) }
        }
    }
}

@Composable
fun ScanFaceCard(onScan: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp)
            .clip(RoundedCornerShape(24.dp)).background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(ElectricBlue.copy(0.07f), Color.Transparent), radius = 300f)
            )
        )
        Row(modifier = Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Face Scanner", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Point camera at face\nfor instant recognition", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NeonPurple, ElectricBlue)))
                        .clickable { onScan() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Now", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(90.dp).rotate(rotation).border(
                    2.dp, Brush.sweepGradient(listOf(ElectricBlue, ElectricBlue.copy(0f))), CircleShape
                ))
                Icon(Icons.Default.Face, null, tint = TextWhite.copy(0.4f), modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun StatMiniCard(label: String, value: String, tint: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(12.dp)
    ) {
        Column {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun AttendanceLogItem(r: AttendanceRecordEntity) {
    val time = remember(r.timestamp) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(r.timestamp)) }
    val date = remember(r.timestamp) { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(r.timestamp)) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(NeonPurple.copy(0.6f), ElectricBlue.copy(0.6f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(r.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(r.name, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("ID: ${r.roll}${if (r.className.isNotBlank()) "  ·  ${r.className}" else ""}", color = TextMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(date, color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PresentGreen.copy(0.15f)).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(PresentGreen))
                Spacer(Modifier.width(4.dp))
                Text("Present", color = PresentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// REGISTER TAB
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterTab(vm: AttendanceViewModel, userClass: String) {
    var name     by remember { mutableStateOf("") }
    var roll     by remember { mutableStateOf("") }
    var enrolled by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    val valid    = name.isNotBlank() && roll.isNotBlank()
    val ctx      = LocalContext.current

    // ── FIXED: snapshot name/roll at the moment the camera is launched ────────
    // If we close over `name` and `roll` directly in the lambda they are
    // stable Compose State values and ARE read at call time — but snapshot them
    // anyway so the callback is not affected by any state change that happens
    // while the camera is open (e.g. the user somehow navigates tabs).
    var pendingName  by remember { mutableStateOf("") }
    var pendingRoll  by remember { mutableStateOf("") }

    if (showCamera) {
        CameraScreen(
            onImageCaptured = { bmp ->
                if (pendingName.isNotBlank() && pendingRoll.isNotBlank()) {
                    vm.registerStudent(
                        name       = pendingName,
                        roll       = pendingRoll,
                        className  = userClass,
                        faceBitmap = bmp,
                        faceRect   = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                    )
                    enrolled = pendingName
                    name = ""
                    roll = ""
                }
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DeepNavy).padding(20.dp)
    ) {
        Text("Enroll Student", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("Capture face data for AI recognition", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        AnimatedVisibility(visible = enrolled != null) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(SuccessGreen.copy(0.12f)).border(1.dp, SuccessGreen.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Enrolled Successfully!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$enrolled has been registered", color = SuccessGreen.copy(0.7f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)).padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(NeonPurple.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.School, null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(userClass.ifBlank { "No class assigned" }, color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(18.dp))
                DarkTextField(Icons.Outlined.Person, name, "Student Full Name") { name = it; enrolled = null }
                Spacer(Modifier.height(14.dp))
                DarkTextField(Icons.Outlined.Tag, roll, "Roll Number / Student ID") { roll = it; enrolled = null }
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (valid) Brush.linearGradient(listOf(NeonPurple, ElectricBlue)) else Brush.linearGradient(listOf(CardBorder, CardBorder)))
                        .clickable(enabled = valid) {
                            // Snapshot so the camera callback always has the right values
                            pendingName = name.trim()
                            pendingRoll = roll.trim()
                            enrolled = null
                            showCamera = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Capture Face & Enroll", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = WarnAmber, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text("Good lighting + frontal face position gives best accuracy.", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun DarkTextField(icon: ImageVector, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (focused) ElectricBlue else CardBorder, label = "border")
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp))
            .background(DeepNavy).border(1.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (focused) ElectricBlue else TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = TextMuted, fontSize = 14.sp)
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = TextStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(ElectricBlue),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            )
        }
    }
}