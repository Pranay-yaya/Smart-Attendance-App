package com.example.smartattendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartattendanceapp.data.UserEntity

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.TextStyle

// ─── Design tokens ─────────────────────────────────────────────────────────────
private val DeepNavy     = Color(0xFF0A0E1A)
private val CardDark     = Color(0xFF111827)
private val CardBorder   = Color(0xFF1E2D40)
private val ElectricBlue = Color(0xFF00D4FF)
private val NeonPurple   = Color(0xFF7B5EFF)
private val SuccessGreen = Color(0xFF00E676)
private val WarnAmber    = Color(0xFFFFAB40)
private val ErrorRed     = Color(0xFFFF5252)
private val TextWhite    = Color(0xFFE8EAED)
private val TextMuted    = Color(0xFF6B7A99)

@Composable
fun SettingsScreen(
    authVM: AuthViewModel,
    attendVM: AttendanceViewModel,    // needed for clearAllAttendance
    onLogout: () -> Unit
) {
    val user by authVM.currentUser.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var attendanceAlerts     by remember { mutableStateOf(true) }
    var biometricEnabled     by remember { mutableStateOf(false) }
    var showLogoutDialog     by remember { mutableStateOf(false) }
    var showPasswordDialog   by remember { mutableStateOf(false) }
    var showClearDialog      by remember { mutableStateOf(false) }
    var isDarkMode           by remember { mutableStateOf(user?.isDarkMode ?: false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        // Header
        item {
            Text("Settings", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Profile & preferences", color = TextMuted, fontSize = 13.sp)
        }

        // Profile card
        item {
            ProfileCard(user)
        }

        // Appearance
        item {
            SettingsGroup("Appearance") {
                // ── FIX: show the toggle as Light Mode / Dark Mode based on current state
                // If the user is already in dark mode, offer "Switch to Light Mode"
                // If the user is in light mode, offer "Switch to Dark Mode"
                // This way the option is always meaningful and never says "Dark Mode: on"
                // while you are already looking at a dark screen.
                SettingsToggleRow(
                    icon     = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    iconTint = if (isDarkMode) WarnAmber else NeonPurple,
                    title    = if (isDarkMode) "Light Mode" else "Dark Mode",
                    subtitle = if (isDarkMode) "Switch to light theme" else "Switch to dark theme",
                    checked  = isDarkMode,
                    onCheckedChange = {
                        isDarkMode = it
                        authVM.updateUserTheme(it)
                    }
                )
            }
        }

        // Notifications
        item {
            SettingsGroup("Notifications") {
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    iconTint = WarnAmber,
                    title = "Push Notifications",
                    subtitle = "Attendance alerts and reminders",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                Divider(color = CardBorder, thickness = 1.dp)
                SettingsToggleRow(
                    icon = Icons.Default.NotificationImportant,
                    iconTint = ElectricBlue,
                    title = "Absence Alerts",
                    subtitle = "Notify when student is absent",
                    checked = attendanceAlerts,
                    onCheckedChange = { attendanceAlerts = it }
                )
            }
        }

        // Privacy & Security
        item {
            SettingsGroup("Privacy & Security") {
                SettingsToggleRow(
                    icon = Icons.Default.Fingerprint,
                    iconTint = SuccessGreen,
                    title = "Biometric Lock",
                    subtitle = "Use fingerprint to unlock app",
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it }
                )
                Divider(color = CardBorder, thickness = 1.dp)
                SettingsActionRow(
                    icon = Icons.Default.Lock,
                    iconTint = ElectricBlue,
                    title = "Change Password",
                    subtitle = "Update your login password"
                ) { showPasswordDialog = true }
                Divider(color = CardBorder, thickness = 1.dp)
                SettingsActionRow(
                    icon = Icons.Default.DeleteForever,
                    iconTint = ErrorRed,
                    title = "Clear Attendance Data",
                    subtitle = "Remove all your recorded attendance"
                ) { showClearDialog = true }
            }
        }

        // App info
        item {
            SettingsGroup("About") {
                SettingsInfoRow(
                    icon = Icons.Default.Info,
                    iconTint = NeonPurple,
                    title = "App Version",
                    value = "AttendX AI v1.0"
                )
                Divider(color = CardBorder, thickness = 1.dp)
                SettingsInfoRow(
                    icon = Icons.Default.Face,
                    iconTint = ElectricBlue,
                    title = "Face Recognition",
                    value = "ML Kit (on-device)"
                )
                Divider(color = CardBorder, thickness = 1.dp)
                SettingsInfoRow(
                    icon = Icons.Default.Storage,
                    iconTint = WarnAmber,
                    title = "Data Storage",
                    value = "Local (Room DB)"
                )
            }
        }

        // Logout
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorRed.copy(0.08f))
                    .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(16.dp))
                    .clickable { showLogoutDialog = true }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorRed.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Logout, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Sign Out", color = ErrorRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Log out of ${user?.fullName ?: "your account"}", color = ErrorRed.copy(0.6f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = ErrorRed.copy(0.5f))
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    // ── Change Password dialog ────────────────────────────────────────────────
    if (showPasswordDialog) {
        ChangePasswordDialog(
            currentPasswordCheck = { pwd -> authVM.currentUser.value?.password == pwd },
            onConfirm  = { newPwd -> authVM.changePassword(newPwd); showPasswordDialog = false },
            onDismiss  = { showPasswordDialog = false }
        )
    }

    // ── Clear attendance confirmation dialog ──────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor   = CardDark,
            titleContentColor = TextWhite,
            textContentColor  = TextMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All Attendance?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("This will permanently delete ALL your attendance records. Your enrolled students will NOT be affected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        attendVM.clearAllAttendance()   // ← actually deletes from Room
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Yes, Clear", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardDark,
            titleContentColor = TextWhite,
            textContentColor = TextMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("You'll need to log in again to access AttendX AI.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authVM.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

// ── Profile card ───────────────────────────────────────────────────────────────
@Composable
private fun ProfileCard(user: UserEntity?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, ElectricBlue))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user?.fullName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user?.fullName ?: "Unknown User",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "@${user?.userId ?: "—"}",
                    color = ElectricBlue,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoleBadge(
                        label = if (user?.isTeacher == true) "Teacher" else "Student",
                        tint   = if (user?.isTeacher == true) WarnAmber else SuccessGreen
                    )
                    if (user?.className?.isNotBlank() == true) {
                        RoleBadge(label = user.className, tint = NeonPurple)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Settings group container ───────────────────────────────────────────────────
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(content = content)
        }
    }
}

// ── Toggle row ─────────────────────────────────────────────────────────────────
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardBorder
            )
        )
    }
}

// ── Action row ─────────────────────────────────────────────────────────────────
@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

// ── Info row ───────────────────────────────────────────────────────────────────
@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = TextMuted, fontSize = 12.sp)
    }
}

// ── Change Password dialog (appended) ─────────────────────────────────────────
@Composable
private fun ChangePasswordDialog(
    currentPasswordCheck: (String) -> Boolean,
    onConfirm: (newPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var current  by remember { mutableStateOf("") }
    var newPwd   by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardDark,
        titleContentColor = TextWhite,
        textContentColor  = TextMuted,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Change Password", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PasswordInputRow("Current Password", current)  { current = it }
                PasswordInputRow("New Password",     newPwd)   { newPwd  = it }
                PasswordInputRow("Confirm New",      confirm)  { confirm = it }
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMsg = when {
                        current.isBlank() || newPwd.isBlank() || confirm.isBlank() ->
                            "All fields are required"
                        !currentPasswordCheck(current) ->
                            "Current password is incorrect"
                        newPwd.length < 4 ->
                            "New password must be at least 4 characters"
                        newPwd != confirm ->
                            "Passwords do not match"
                        else -> {
                            onConfirm(newPwd)
                            return@Button
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) { Text("Update", color = DeepNavy, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

@Composable
private fun PasswordInputRow(placeholder: String, value: String, onChange: (String) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DeepNavy)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = TextMuted, fontSize = 13.sp)
            androidx.compose.foundation.text.BasicTextField(
                value     = value,
                onValueChange = onChange,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextWhite, fontSize = 13.sp),
                cursorBrush = SolidColor(ElectricBlue),
                visualTransformation = PasswordVisualTransformation(),
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }
}