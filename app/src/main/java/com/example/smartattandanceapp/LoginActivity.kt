package com.example.smartattendanceapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ElectricBlue = Color(0xFF00D4FF)
private val NeonPurple   = Color(0xFF7B5EFF)
private val DeepNavy     = Color(0xFF0A0E1A)
private val CardDark     = Color(0xFF111827)
private val CardBorder   = Color(0xFF1E2D40)
private val TextWhite    = Color(0xFFE8EAED)
private val TextMuted    = Color(0xFF6B7A99)
private val SuccessGreen = Color(0xFF00E676)
private val ErrorRed     = Color(0xFFFF5252)

class LoginActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── FIX 1: WHITE FLASH ──────────────────────────────────────────────────
        // The default Theme.Smartattendanceapp has a white windowBackground.
        // Android draws that background before Compose renders its first frame,
        // causing the white blink on every activity start.
        // Setting the window background to the app's dark color here (before
        // setContent) means the window is already dark when it first appears.
        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#0A0E1A"))
        )

        setContent {
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

            // ── FIX 2: PASS userId TO MAINACTIVITY ──────────────────────────────
            // We put the userId in the Intent so MainActivity can call
            // authViewModel.restoreSession(userId). Without this, MainActivity's
            // fresh AuthViewModel has isLoggedIn=false and immediately bounces
            // the user back to LoginActivity, causing the blink loop.
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    val userId = authViewModel.currentUser.value?.userId ?: ""
                    startActivity(
                        Intent(this@LoginActivity, MainActivity::class.java)
                            .putExtra("userId", userId)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
            }

            AttendXAuthScreen(authViewModel)
        }
    }
}

@Composable
fun AttendXAuthScreen(viewModel: AuthViewModel) {
    var isSignup     by remember { mutableStateOf(false) }
    var userId       by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var fullName     by remember { mutableStateOf("") }
    var className    by remember { mutableStateOf("") }
    var isTeacher    by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.errorMessage.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // Background glow blobs — drawn in a separate layer so they never
        // affect the scrollable content's layout measurements
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = glowAlpha), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricBlue.copy(alpha = glowAlpha * 0.7f), Color.Transparent)
                    )
                )
        )

        // ── FIX 3A: NO imePadding() ─────────────────────────────────────────
        // AndroidManifest already sets windowSoftInputMode="adjustPan".
        // adjustPan = the whole window pans up when the keyboard appears.
        // imePadding() = adds bottom padding equal to keyboard height.
        // Using BOTH at the same time causes a layout oscillation loop every
        // frame the keyboard is visible → visible as shaking / blinking.
        // Remove imePadding() here; adjustPan alone handles the keyboard.
        //
        // ── FIX 3B: Arrangement.Top instead of Arrangement.Center ───────────
        // Arrangement.Center inside a verticalScroll Column is a Compose bug:
        // verticalScroll gives the Column infinite height, and Compose cannot
        // center content in an infinite container → repeated layout passes →
        // contributes to shaking.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // ↑ NO .imePadding() here — would conflict with adjustPan
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top  // NOT Center — broken with verticalScroll
        ) {

            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(NeonPurple, ElectricBlue))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("👁", fontSize = 34.sp)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "AttendX AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = (-0.5).sp
            )
            Text(
                if (isSignup) "Create your account" else "Sign in to continue",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(28.dp))

            // ── Main card ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {

                    // Signup-only fields
                    AnimatedVisibility(
                        visible = isSignup,
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            LoginTextField(
                                icon = Icons.Outlined.Person,
                                value = fullName,
                                placeholder = "Full Name",
                                onValueChange = { fullName = it }
                            )
                            Spacer(Modifier.height(12.dp))
                            LoginTextField(
                                icon = Icons.Outlined.School,
                                value = className,
                                placeholder = "Class (e.g. CSE-3A)",
                                onValueChange = { className = it }
                            )
                            Spacer(Modifier.height(12.dp))
                            RoleToggle(isTeacher) { isTeacher = it }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    LoginTextField(
                        icon = Icons.Outlined.AccountCircle,
                        value = userId,
                        placeholder = "User ID",
                        onValueChange = { userId = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    LoginTextField(
                        icon = Icons.Outlined.Lock,
                        value = password,
                        placeholder = "Password",
                        onValueChange = { password = it },
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword }
                    )

                    // Demo credentials hint
                    if (!isSignup) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricBlue.copy(0.07f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Demo: teacher01 / 1234", color = ElectricBlue.copy(0.85f), fontSize = 12.sp)
                        }
                    }

                    // Error message
                    AnimatedVisibility(visible = error != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ErrorRed.copy(0.1f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error ?: "", color = ErrorRed, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Sign in / Sign up button ──────────────────────────────
                    val btnEnabled = !isLoading &&
                            userId.isNotBlank() && password.isNotBlank() &&
                            (!isSignup || (fullName.isNotBlank() && className.isNotBlank()))

                    Button(
                        onClick = {
                            viewModel.clearError()
                            if (isSignup)
                                viewModel.signup(userId.trim(), password, fullName.trim(), className.trim(), isTeacher, false)
                            else
                                viewModel.login(userId.trim(), password)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = btnEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (btnEnabled)
                                        Brush.linearGradient(listOf(NeonPurple, ElectricBlue))
                                    else
                                        Brush.linearGradient(listOf(CardBorder, CardBorder)),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isSignup) Icons.Default.PersonAdd else Icons.Default.Login,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isSignup) "Create Account" else "Sign In",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Switch between login / signup ─────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isSignup) "Already have an account? " else "Don't have an account? ",
                    color = TextMuted,
                    fontSize = 14.sp
                )
                Text(
                    if (isSignup) "Sign In" else "Sign Up",
                    color = ElectricBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        isSignup = !isSignup
                        viewModel.clearError()
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── AI badge ──────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark.copy(0.7f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(Modifier.width(8.dp))
                Text("AI Face Recognition Active", color = TextMuted, fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Text field ────────────────────────────────────────────────────────────────
@Composable
fun LoginTextField(
    icon: ImageVector,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) ElectricBlue else CardBorder,
        label = "border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DeepNavy)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (focused) ElectricBlue else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextMuted, fontSize = 14.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(ElectricBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                visualTransformation = if (isPassword && !showPassword)
                    PasswordVisualTransformation() else VisualTransformation.None
            )
        }
        if (isPassword && onTogglePassword != null) {
            Icon(
                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                null,
                tint = TextMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onTogglePassword() }
            )
        }
    }
}

// ── Role toggle ───────────────────────────────────────────────────────────────
@Composable
fun RoleToggle(isTeacher: Boolean, onRoleChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepNavy)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        listOf(false to "🎓 Student", true to "📋 Teacher").forEach { (teacher, label) ->
            val selected = isTeacher == teacher
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (selected)
                            Brush.linearGradient(listOf(NeonPurple.copy(0.8f), ElectricBlue.copy(0.8f)))
                        else
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onRoleChange(teacher) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else TextMuted,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}