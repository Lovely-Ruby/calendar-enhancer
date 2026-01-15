package com.example.calendarenhancer

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.calendarenhancer.data.BirthdayEntity
import com.example.calendarenhancer.util.CalendarUtil
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarYear
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BirthdayItem(entity: BirthdayEntity, onDelete: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    
    // 滑动偏移量状态
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)
    
    // 按钮栏的宽度（大约 3 个按钮的宽度）
    val actionWidth = -550f 

    val days = remember(entity.dateStr, entity.isLunar) { calculateDays(entity.dateStr, entity.isLunar) }
    val displayDate = remember(entity.dateStr, entity.isLunar) {
        if (entity.isLunar) formatLunarDate(entity.dateStr) else entity.dateStr
    }
    
    val syncAction = {
        offsetX = 0f // 点击后收回
        syncToCalendar(context, entity)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            syncAction()
        } else {
            Toast.makeText(context, "需要日历权限才能同步", Toast.LENGTH_SHORT).show()
        }
    }

    var showSyncConfirm by remember { mutableStateOf(false) }

    if (showSyncConfirm) {
        AlertDialog(
            onDismissRequest = { showSyncConfirm = false },
            title = { Text("同步到日历") },
            text = { Text("将 ${entity.name} 的生日 ($displayDate) 同步到系统日历中并设置提醒？") },
            confirmButton = {
                TextButton(onClick = {
                    showSyncConfirm = false
                    val hasRead = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasWrite = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasRead && hasWrite) syncAction() else permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR))
                }) {
                    Text("同步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 背景层：操作按钮
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = {
                showSyncConfirm = true
            }) {
                Icon(Icons.Default.Refresh, "同步", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = {
                offsetX = 0f
                onEdit()
            }) {
                Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                offsetX = 0f
                onDelete()
            }) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }

        // 前景层：生日卡片
        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // 松手时判断：如果滑动超过一半宽度，则吸附到打开状态，否则弹回
                            offsetX = if (offsetX < actionWidth / 2) actionWidth else 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // 限制滑动范围，只能向左滑，且不能滑出按钮区域太多
                            val newOffset = (offsetX + dragAmount).coerceIn(actionWidth - 50f, 0f)
                            offsetX = newOffset
                        }
                    )
                }
                .combinedClickable(
                    onClick = { 
                        if (offsetX != 0f) offsetX = 0f else onEdit() 
                    }
                )
        ) {
            Box(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(entity.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (entity.isLunar) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "阴历",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("日期: $displayDate", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "$days",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "天后",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BirthdayListScreen(modifier: Modifier, list: List<BirthdayEntity>, onDelete: (BirthdayEntity) -> Unit, onEdit: (BirthdayEntity) -> Unit) {
    var entityToDelete by remember { mutableStateOf<BirthdayEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { entity ->
                BirthdayItem(
                    entity = entity, 
                    onDelete = { entityToDelete = entity }, 
                    onEdit = { onEdit(entity) }
                )
            }
        }

        if (entityToDelete != null) {
            AlertDialog(
                onDismissRequest = { entityToDelete = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除 ${entityToDelete?.name} 的生日记录吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            entityToDelete?.let { onDelete(it) }
                            entityToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entityToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun AddOrEditDialog(initialEntity: BirthdayEntity?, onDismiss: () -> Unit, onConfirm: (String, String, Boolean, Int) -> Unit) {
    var name by remember { mutableStateOf(initialEntity?.name ?: "") }
    var date by remember { mutableStateOf(initialEntity?.dateStr ?: "") }
    var isLunar by remember { mutableStateOf(initialEntity?.isLunar ?: false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntity == null) "新增生日" else "编辑生日") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") })
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isLunar, onCheckedChange = { isLunar = it })
                    Text("阴历生日")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val cal = Calendar.getInstance()
                    val (y, m, d) = if (date.contains("-") && date.split("-").size == 3) {
                        val p = date.split("-")
                        Triple(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
                    } else {
                        Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    }
                    
                    DatePickerDialog(context, { _, year, month, day -> 
                        date = String.format("%d-%02d-%02d", year, month + 1, day) 
                    }, y, m, d).show()
                }) { 
                    val label = if (date.isEmpty()) "选择出生日期" else {
                        if (isLunar) "阴历: ${formatLunarDate(date)}" else "公历: $date"
                    }
                    Text(label) 
                }
                if (isLunar) {
                    Text("提示：请在选择器中选择农历对应的公历年月日", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if(name.isNotBlank() && date.isNotBlank()) onConfirm(name, date, isLunar, initialEntity?.id ?: 0) }) { Text("确定") }
        }
    )
}

fun formatLunarDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        
        val lunar = Lunar.fromYmd(year, month, day)
        "${lunar.yearInChinese}(${lunar.yearInGanZhi}${lunar.yearShengXiao})年${lunar.monthInChinese}月${lunar.dayInChinese}"
    } catch (e: Exception) {
        dateStr
    }
}

fun calculateDays(dateStr: String, isLunar: Boolean): Int {
    return try {
        val today = LocalDate.now()
        val parts = dateStr.split("-")
        val month = if (parts.size == 3) parts[1].toInt() else parts[0].toInt()
        val day = if (parts.size == 3) parts[2].toInt() else parts[1].toInt()

        val targetDate = if (!isLunar) {
            var target = LocalDate.of(today.year, month, day)
            if (target.isBefore(today)) target = target.plusYears(1)
            target
        } else {
            val currentYear = today.year
            
            fun getValidLunar(y: Int, m: Int, d: Int): Lunar {
                val lunarYear = LunarYear.fromYear(y)
                val lunarMonth = lunarYear.getMonth(m)
                val monthDays = lunarMonth?.dayCount ?: 30
                return Lunar.fromYmd(y, m, if (d > monthDays) monthDays else d)
            }

            val lunarThisYear = getValidLunar(currentYear, month, day)
            val solarThisYear = lunarThisYear.solar
            var target = LocalDate.of(solarThisYear.year, solarThisYear.month, solarThisYear.day)
            
            if (target.isBefore(today)) {
                val lunarNextYear = getValidLunar(currentYear + 1, month, day)
                val solarNextYear = lunarNextYear.solar
                target = LocalDate.of(solarNextYear.year, solarNextYear.month, solarNextYear.day)
            }
            target
        }
        
        ChronoUnit.DAYS.between(today, targetDate).toInt()
    } catch (e: Exception) { 0 }
}

private fun syncToCalendar(context: android.content.Context, entity: BirthdayEntity) {
    try {
        val today = LocalDate.now()
        val parts = entity.dateStr.split("-")
        val month = if (parts.size == 3) parts[1].toInt() else parts[0].toInt()
        val day = if (parts.size == 3) parts[2].toInt() else parts[1].toInt()

        val targetDate: LocalDate = if (!entity.isLunar) {
            var target = LocalDate.of(today.year, month, day)
            if (target.isBefore(today)) target = target.plusYears(1)
            target
        } else {
            val currentYear = today.year
            
            fun getValidLunar(y: Int, m: Int, d: Int): Lunar {
                val lunarYear = LunarYear.fromYear(y)
                val lunarMonth = lunarYear.getMonth(m)
                val monthDays = lunarMonth?.dayCount ?: 30
                return Lunar.fromYmd(y, m, if (d > monthDays) monthDays else d)
            }

            val lunarThisYear = getValidLunar(currentYear, month, day)
            val solarThisYear = lunarThisYear.solar
            var target = LocalDate.of(solarThisYear.year, solarThisYear.month, solarThisYear.day)
            
            if (target.isBefore(today)) {
                val lunarNextYear = getValidLunar(currentYear + 1, month, day)
                val solarNextYear = lunarNextYear.solar
                target = LocalDate.of(solarNextYear.year, solarNextYear.month, solarNextYear.day)
            }
            target
        }
        
        val startTime = targetDate.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        CalendarUtil.addCalendarEvent(
            context,
            "${entity.name} 生日",
            "今天是 ${entity.name} 的生日，记得送上祝福！",
            startTime
        )
    } catch (e: Exception) {
        Toast.makeText(context, "日期计算错误: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}