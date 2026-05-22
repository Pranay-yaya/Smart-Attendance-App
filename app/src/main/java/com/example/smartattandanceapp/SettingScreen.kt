package com.example.smartattendanceapp

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.smartattendanceapp.data.UserEntity

@Composable
fun SettingsScreen(
    authVM:    AuthViewModel,
    attendVM:  AttendanceViewModel,
    onLogout:  () -> Unit
) {
    val theme   = LocalAppTheme.current          // reads the live theme
    val context = LocalContext.current
    val prefs   = remember { AppPreferences.get(context) }
    val user    by authVM.currentUser.collectAsState()

    // ── Persisted state (reads from SharedPreferences) ────────────────────────
    var isLightMode          by remember { mutableStateOf(prefs.isLightMode) }
    var pushNotifications    by remember { mutableStateOf(prefs.pushNotificationsEnabled) }
    var absenceAlerts        by remember { mutableStateOf(prefs.absenceAlertsEnabled) }

    // ── Biometric state ───────────────────────────────────────────────────────
    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }
    var biometricEnabled  by remember { mutableStateOf(false) }
    var biometricStatus   by remember { mutableStateOf("") }

    // ── Dialog flags ──────────────────────────────────────────────────────────
    var showLogoutDialog   by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearDialog    by remember { mutableStateOf(false) }

    // Snackbar for operation feedback (attendance clear, password changed, etc.)
    val snackbarHost = remember { SnackbarHostState() }
    val opMsg by attendVM.operationMessage.collectAsState()
    LaunchedEffect(opMsg) {
        opMsg?.let { snackbarHost.showSnackbar(it); attendVM.clearOperationMessage() }
    }

    Scaffold(
        containerColor = theme.background,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(data, containerColor = theme.card, contentColor = theme.textPrimary,
                    actionColor = theme.accent, shape = RoundedCornerShape(12.dp))
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(theme.background)
                .padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            item {
                Text("Settings", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Profile & preferences", color = theme.textMuted, fontSize = 13.sp)
            }

            // ── Profile card ──────────────────────────────────────────────────
            item { ProfileCard(user, theme) }

            // ── APPEARANCE ────────────────────────────────────────────────────
            item {
                SGroup("Appearance", theme) {
                    // Light Mode toggle — app is dark by default.
                    // When toggled ON → everything switches to white/light theme.
                    // The toggle always shows the mode you can SWITCH TO,
                    // so in dark mode it says "Light Mode" (what you'd enable).
                    SToggle(
                        icon     = if (isLightMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        iconTint = if (isLightMode) theme.accent2 else theme.warning,
                        title    = if (isLightMode) "Dark Mode" else "Light Mode",
                        subtitle = if (isLightMode) "Currently: Light — tap to go Dark"
                        else             "Currently: Dark — tap to go Light",
                        checked  = isLightMode,
                        theme    = theme,
                        onCheckedChange = { newVal ->
                            isLightMode = newVal
                            prefs.isLightMode = newVal
                            // Save to DB so it persists across logins
                            authVM.updateUserTheme(newVal)
                        }
                    )
                }
            }

            // ── NOTIFICATIONS (stored in SharedPreferences) ───────────────────
            item {
                SGroup("Notifications", theme) {
                    SToggle(
                        icon     = Icons.Default.Notifications,
                        iconTint = theme.warning,
                        title    = "Push Notifications",
                        subtitle = if (pushNotifications) "Enabled — you'll get alerts" else "Disabled — no alerts",
                        checked  = pushNotifications,
                        theme    = theme,
                        onCheckedChange = { v ->
                            pushNotifications = v
                            prefs.pushNotificationsEnabled = v   // saved to SharedPrefs
                            // Request POST_NOTIFICATIONS permission on Android 13+
                            if (v && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val activity = context as? android.app.Activity
                                activity?.requestPermissions(
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001
                                )
                            }
                        }
                    )
                    SDivider(theme)
                    SToggle(
                        icon     = Icons.Default.NotificationImportant,
                        iconTint = theme.accent,
                        title    = "Absence Alerts",
                        subtitle = if (absenceAlerts) "Notifies when a student misses class"
                        else               "Absence alerts are off",
                        checked  = absenceAlerts,
                        theme    = theme,
                        onCheckedChange = { v ->
                            absenceAlerts = v
                            prefs.absenceAlertsEnabled = v       // saved to SharedPrefs
                        }
                    )
                }
            }

            // ── PRIVACY & SECURITY ────────────────────────────────────────────
            item {
                SGroup("Privacy & Security", theme) {

                    // Biometric — uses Android BiometricPrompt (real hardware check)
                    if (biometricAvailable) {
                        SToggle(
                            icon     = Icons.Default.Fingerprint,
                            iconTint = theme.success,
                            title    = "Biometric Lock",
                            subtitle = when {
                                biometricEnabled -> "Fingerprint lock is ON"
                                biometricStatus.isNotEmpty() -> biometricStatus
                                else -> "Use fingerprint / face to verify identity"
                            },
                            checked  = biometricEnabled,
                            theme    = theme,
                            onCheckedChange = { want ->
                                if (want) {
                                    // Trigger real fingerprint prompt to verify before enabling
                                    triggerBiometric(
                                        context = context,
                                        title   = "Enable Biometric Lock",
                                        subtitle = "Confirm your fingerprint to enable",
                                        onSuccess = {
                                            biometricEnabled = true
                                            biometricStatus  = ""
                                        },
                                        onFail = { msg ->
                                            biometricEnabled = false
                                            biometricStatus  = msg
                                        }
                                    )
                                } else {
                                    biometricEnabled = false
                                    biometricStatus  = ""
                                }
                            }
                        )
                    } else {
                        // Device has no biometric hardware or it's not enrolled
                        SInfoRow(
                            icon     = Icons.Default.Fingerprint,
                            iconTint = theme.textMuted,
                            title    = "Biometric Lock",
                            value    = "Not available on this device",
                            theme    = theme
                        )
                    }

                    SDivider(theme)
                    SActionRow(
                        icon     = Icons.Default.Lock,
                        iconTint = theme.accent,
                        title    = "Change Password",
                        subtitle = "Update your login password",
                        theme    = theme
                    ) { showPasswordDialog = true }

                    SDivider(theme)
                    SActionRow(
                        icon     = Icons.Default.DeleteForever,
                        iconTint = theme.error,
                        title    = "Clear Attendance Data",
                        subtitle = "Remove all your recorded attendance",
                        theme    = theme
                    ) { showClearDialog = true }
                }
            }

            // ── ABOUT ─────────────────────────────────────────────────────────
            item {
                SGroup("About", theme) {
                    SInfoRow(Icons.Default.Info,    theme.accent2, "App Version",        "AttendX AI v1.0", theme)
                    SDivider(theme)
                    SInfoRow(Icons.Default.Face,    theme.accent,  "Face Recognition",   "ML Kit (on-device)", theme)
                    SDivider(theme)
                    SInfoRow(Icons.Default.Storage, theme.warning, "Data Storage",       "Local · Room DB", theme)
                    SDivider(theme)
                    SInfoRow(Icons.Default.Android, theme.success, "Android",
                        "API ${Build.VERSION.SDK_INT}", theme)
                }
            }

            // ── SIGN OUT ──────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.error.copy(0.08f))
                        .border(1.dp, theme.error.copy(0.3f), RoundedCornerShape(16.dp))
                        .clickable { showLogoutDialog = true }.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(theme.error.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Logout, null, tint = theme.error, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Sign Out", color = theme.error, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Log out of ${user?.fullName ?: "your account"}",
                                color = theme.error.copy(0.6f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = theme.error.copy(0.5f))
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // ── Change Password ───────────────────────────────────────────────────────
    if (showPasswordDialog) {
        ChangePasswordDialog(
            theme = theme,
            currentPasswordCheck = { authVM.currentUser.value?.password == it },
            onConfirm = { newPwd ->
                authVM.changePassword(newPwd)
                showPasswordDialog = false
            },
            onDismiss = { showPasswordDialog = false }
        )
    }

    // ── Clear Attendance ──────────────────────────────────────────────────────
    if (showClearDialog) {
        AppAlertDialog(
            icon         = Icons.Default.DeleteForever,
            iconTint     = theme.error,
            title        = "Clear All Attendance?",
            message      = "This permanently deletes ALL your attendance records. Enrolled students are NOT affected.",
            confirmText  = "Yes, Clear",
            confirmColor = theme.error,
            theme        = theme,
            onConfirm    = { attendVM.clearAllAttendance(); showClearDialog = false },
            onDismiss    = { showClearDialog = false }
        )
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AppAlertDialog(
            icon         = Icons.Default.Logout,
            iconTint     = theme.error,
            title        = "Sign Out?",
            message      = "You'll need to log in again to use AttendX AI.",
            confirmText  = "Sign Out",
            confirmColor = theme.error,
            theme        = theme,
            onConfirm    = { showLogoutDialog = false; authVM.logout(); onLogout() },
            onDismiss    = { showLogoutDialog = false }
        )
    }
}

// ─── Biometric helper ─────────────────────────────────────────────────────────
private fun triggerBiometric(
    context:   Context,
    title:     String,
    subtitle:  String,
    onSuccess: () -> Unit,
    onFail:    (String) -> Unit
) {
    val activity = context as? FragmentActivity ?: run {
        onFail("Cannot show biometric prompt")
        return
    }
    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                onFail(if (code == BiometricPrompt.ERROR_CANCELED || code == BiometricPrompt.ERROR_USER_CANCELED)
                    "" else "Auth failed: $msg")
            }
            override fun onAuthenticationFailed() { onFail("Fingerprint not recognised") }
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    )
}

// ─── Profile card ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileCard(user: UserEntity?, theme: AppColors) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(theme.card).border(1.dp, theme.cardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(theme.accent2, theme.accent))),
                contentAlignment = Alignment.Center
            ) {
                Text(user?.fullName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user?.fullName ?: "Unknown", color = theme.textPrimary,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("@${user?.userId ?: "—"}", color = theme.accent, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoleBadge(if (user?.isTeacher == true) "Teacher" else "Student",
                        if (user?.isTeacher == true) theme.warning else theme.success, theme)
                    if (user?.className?.isNotBlank() == true)
                        RoleBadge(user.className, theme.accent2, theme)
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(label: String, tint: Color, theme: AppColors) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
        .background(tint.copy(0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Group container ──────────────────────────────────────────────────────────
@Composable
private fun SGroup(title: String, theme: AppColors, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = theme.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(theme.card).border(1.dp, theme.cardBorder, RoundedCornerShape(16.dp))
        ) { Column(content = content) }
    }
}

@Composable
private fun SDivider(theme: AppColors) = Divider(color = theme.cardBorder, thickness = 1.dp)

// ─── Toggle row ───────────────────────────────────────────────────────────────
@Composable
private fun SToggle(
    icon: ImageVector, iconTint: Color, title: String, subtitle: String,
    checked: Boolean, theme: AppColors, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SIcon(icon, iconTint, theme)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = theme.textMuted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = theme.accent,
                uncheckedThumbColor = theme.textMuted,
                uncheckedTrackColor = theme.cardBorder
            )
        )
    }
}

// ─── Action row ───────────────────────────────────────────────────────────────
@Composable
private fun SActionRow(
    icon: ImageVector, iconTint: Color, title: String, subtitle: String,
    theme: AppColors, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SIcon(icon, iconTint, theme)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = theme.textMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = theme.textMuted, modifier = Modifier.size(18.dp))
    }
}

// ─── Info row ─────────────────────────────────────────────────────────────────
@Composable
private fun SInfoRow(
    icon: ImageVector, iconTint: Color, title: String, value: String, theme: AppColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SIcon(icon, iconTint, theme)
        Spacer(Modifier.width(14.dp))
        Text(title, color = theme.textPrimary, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = theme.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun SIcon(icon: ImageVector, tint: Color, theme: AppColors) {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(0.12f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
}

// ─── Change Password dialog ───────────────────────────────────────────────────
@Composable
private fun ChangePasswordDialog(
    theme: AppColors,
    currentPasswordCheck: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var current  by remember { mutableStateOf("") }
    var newPwd   by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = theme.card,
        titleContentColor = theme.textPrimary,
        textContentColor  = theme.textMuted,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = theme.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Change Password", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PwdField("Current Password", current, theme) { current = it }
                PwdField("New Password",     newPwd,  theme) { newPwd  = it }
                PwdField("Confirm New",      confirm, theme) { confirm = it }
                AnimatedVisibility(visible = errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = theme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMsg = when {
                        current.isBlank() || newPwd.isBlank() || confirm.isBlank() -> "All fields required"
                        !currentPasswordCheck(current) -> "Current password incorrect"
                        newPwd.length < 4 -> "New password must be ≥ 4 characters"
                        newPwd != confirm -> "Passwords don't match"
                        else -> { onConfirm(newPwd); return@Button }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) { Text("Update", color = if (theme.isLight) Color.White else theme.background, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = theme.textMuted) }
        }
    )
}

@Composable
private fun PwdField(placeholder: String, value: String, theme: AppColors, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(10.dp))
            .background(theme.background).border(1.dp, theme.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = theme.textMuted, fontSize = 13.sp)
            BasicTextField(value = value, onValueChange = onChange,
                textStyle = TextStyle(color = theme.textPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(theme.accent),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─── Generic alert dialog ─────────────────────────────────────────────────────
@Composable
private fun AppAlertDialog(
    icon: ImageVector, iconTint: Color, title: String, message: String,
    confirmText: String, confirmColor: Color, theme: AppColors,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = theme.card,
        titleContentColor = theme.textPrimary,
        textContentColor  = theme.textMuted,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text          = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) { Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = theme.textMuted) }
        }
    )
}