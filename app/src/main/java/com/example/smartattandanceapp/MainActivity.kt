package com.example.smartattandanceapp

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepak.smartattendanceapp.data.AttendanceRecordEntity
import com.deepak.smartattendanceapp.ui.theme.SmartattendanceappTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartattendanceappTheme {
                SmartAttendanceScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAttendanceScreen(viewModel: AttendanceViewModel) {
    var name by remember { mutableStateOf("") }
    var roll by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("register") }
    val records by viewModel.attendanceRecords.collectAsState()
    val isLoaded by viewModel.isModelLoaded.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val msg by viewModel.message.collectAsState()
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { processImage(it, mode, name, roll) }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    fun processImage(bitmap: Bitmap, mode: String, name: String, roll: String) {
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val rect = faces[0].boundingBox
                    if (mode == "register") {
                        viewModel.registerStudent(name, roll, bitmap, rect) {}
                    } else {
                        viewModel.markAttendance(bitmap, rect, {}, {})
                    }
                } else {
                    viewModel.clearMessage()
                    // Handled via UI snackbar if needed
                }
            }
            .addOnFailureListener { viewModel.clearMessage() }
    }

    LaunchedEffect(msg) {
        msg?.let { delay(4000); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AttendX AI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().background(
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                        ).padding(20.dp)) {
                            Column {
                                Text("Smart Attendance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("AI-Powered Face Recognition", color = Color.White.copy(0.8f))
                            }
                        }
                    }
                }

                item {
                    if (!isLoaded) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                            Text("⚠️ Add mobile_face_net.tflite to assets folder", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                item {
                    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Student Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = roll, onValueChange = { roll = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { mode = "register"; permLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f).height(56.dp), enabled = isLoaded && !isLoading, shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.padding(end = 6.dp))
                            Text("Register")
                        }
                        Button(onClick = { mode = "attendance"; permLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f).height(56.dp), enabled = isLoaded && !isLoading, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.padding(end = 6.dp))
                            Text("Mark")
                        }
                    }
                }

                item { Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }

                if (records.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("No records yet", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(records) { record ->
                        AttendanceCard(record)
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            msg?.let { (text, success) ->
                AnimatedVisibility(visible = true, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (success) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(text, color = Color.White, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearMessage() }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceCard(record: AttendanceRecordEntity) {
    val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val d = Date(record.timestamp)
    val conf = record.confidence
    val confColor = if (conf >= 0.85) Color(0xFF4CAF50) else if (conf >= 0.75) Color(0xFFFF9800) else Color(0xFFF44336)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
                Text(record.name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.SemiBold)
                Text("Roll: ${record.roll}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text("${dateFmt.format(d)} • ${timeFmt.format(d)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (conf > 0) Text("${(conf * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = confColor, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
        }
    }
}