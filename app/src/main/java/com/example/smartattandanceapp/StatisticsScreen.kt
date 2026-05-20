package com.example.smartattendanceapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartattendanceapp.data.AttendanceRecordEntity
import java.text.SimpleDateFormat
import java.util.*

// ─── Design tokens ─────────────────────────────────────────────────────────────
private val DeepNavy     = Color(0xFF0A0E1A)
private val CardDark     = Color(0xFF111827)
private val CardBorder   = Color(0xFF1E2D40)
private val ElectricBlue = Color(0xFF00D4FF)
private val NeonPurple   = Color(0xFF7B5EFF)
private val SuccessGreen = Color(0xFF00E676)
private val WarnAmber    = Color(0xFFFFAB40)
private val TextWhite    = Color(0xFFE8EAED)
private val TextMuted    = Color(0xFF6B7A99)

enum class TimeFilter(val label: String) {
    ALL("All"), TODAY("Today"), WEEK("Week"), MONTH("Month")
}

@Composable
fun StatisticsScreen(vm: AttendanceViewModel) {
    val allRecords by vm.attendanceRecords.collectAsState()
    var selectedFilter by remember { mutableStateOf(TimeFilter.ALL) }

    val filtered = remember(allRecords, selectedFilter) {
        val now = System.currentTimeMillis()
        val dayMs  = 86_400_000L
        val fmt    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today  = fmt.format(Date())
        when (selectedFilter) {
            TimeFilter.ALL   -> allRecords
            TimeFilter.TODAY -> allRecords.filter { it.date == today }
            TimeFilter.WEEK  -> allRecords.filter { now - it.timestamp <= 7 * dayMs }
            TimeFilter.MONTH -> allRecords.filter { now - it.timestamp <= 30 * dayMs }
        }
    }

    val byDate = remember(filtered) {
        filtered.groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .take(7)
    }

    val byClass = remember(filtered) {
        filtered.groupBy { it.className.ifBlank { "Unknown" } }
            .entries
            .sortedByDescending { it.value.size }
    }

    val uniqueStudents = remember(filtered) {
        filtered.map { it.roll }.distinct().size
    }

    val todayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayCount = remember(filtered) { filtered.count { it.date == todayFmt } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        // Header
        item {
            Text("Analytics", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Attendance insights & trends", color = TextMuted, fontSize = 13.sp)
        }

        // Time filter pills
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeFilter.values().forEach { filter ->
                    val selected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) Brush.linearGradient(listOf(NeonPurple, ElectricBlue))
                                else Brush.linearGradient(listOf(CardDark, CardDark))
                            )
                            .border(
                                1.dp,
                                if (selected) Color.Transparent else CardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            filter.label,
                            color = if (selected) Color.White else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Summary row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard("Total Records", "${filtered.size}", ElectricBlue, Icons.Default.EventNote, Modifier.weight(1f))
                SummaryCard("Unique Students", "$uniqueStudents", NeonPurple, Icons.Default.People, Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard("Today", "$todayCount", SuccessGreen, Icons.Default.Today, Modifier.weight(1f))
                SummaryCard("Classes", "${byClass.size}", WarnAmber, Icons.Default.School, Modifier.weight(1f))
            }
        }

        // Attendance chart (last 7 days)
        if (byDate.isNotEmpty()) {
            item {
                SectionTitle("Last 7 Days", Icons.Default.BarChart, ElectricBlue)
            }
            item {
                AttendanceBarChart(byDate.map { it.key to it.value.size })
            }
        }

        // Class-wise breakdown
        if (byClass.isNotEmpty()) {
            item {
                SectionTitle("By Class", Icons.Default.School, NeonPurple)
            }
            items(byClass) { (cls, records) ->
                ClassBreakdownRow(cls, records.size, filtered.size)
            }
        }

        // Recent activity
        if (filtered.isNotEmpty()) {
            item {
                SectionTitle("Recent Activity", Icons.Default.History, WarnAmber)
            }
            items(filtered.take(10)) { record ->
                RecentActivityItem(record)
            }
        }

        // Empty state
        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 40.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("No data for this period", color = TextMuted, fontSize = 14.sp)
                        Text("Try a different filter", color = TextMuted.copy(0.5f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── Summary card ───────────────────────────────────────────────────────────────
@Composable
private fun SummaryCard(
    label: String,
    value: String,
    tint: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = tint, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TextMuted, fontSize = 12.sp)
        }
    }
}

// ── Section title ──────────────────────────────────────────────────────────────
@Composable
private fun SectionTitle(title: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Bar chart (last 7 days) ────────────────────────────────────────────────────
@Composable
private fun AttendanceBarChart(data: List<Pair<String, Int>>) {
    val maxVal = data.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (date, count) ->
                    val fraction = count / maxVal
                    val barHeight = (fraction * 100).coerceAtLeast(4f)

                    // Animated bar height
                    val animatedHeight by animateFloatAsState(
                        targetValue = barHeight,
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                        label = "bar_$date"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "$count",
                            color = ElectricBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(animatedHeight.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    Brush.verticalGradient(listOf(ElectricBlue, NeonPurple))
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        // Date label (show day only)
                        val dayLabel = try {
                            date.split("-").getOrNull(2) ?: date
                        } catch (e: Exception) { date }
                        Text(dayLabel, color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── Class breakdown row ────────────────────────────────────────────────────────
@Composable
private fun ClassBreakdownRow(className: String, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "class_bar"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(NeonPurple.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(className, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "$count records",
                    color = NeonPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(NeonPurple, ElectricBlue)))
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${(fraction * 100).toInt()}% of total records",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

// ── Recent activity item ───────────────────────────────────────────────────────
@Composable
private fun RecentActivityItem(record: AttendanceRecordEntity) {
    val time = remember(record.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(NeonPurple.copy(0.5f), ElectricBlue.copy(0.5f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                record.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(record.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(record.date, color = TextMuted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, color = ElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(SuccessGreen.copy(0.12f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(SuccessGreen))
                Spacer(Modifier.width(4.dp))
                Text("Present", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
