package com.example.calendarenhancer

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.PushPin
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
fun BirthdayItem(
    entity: BirthdayEntity, 
    onDelete: () -> Unit, 
    onEdit: () -> Unit,
    onTogglePin: () -> Unit
) {
    val context = LocalContext.current
    val currentOnTogglePin by rememberUpdatedState(onTogglePin)
    
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)
    
    val rightActionWidth = -550f 
    val triggerThreshold = 200f  
    val isTriggered = offsetX > triggerThreshold

    // 背景颜色动画
    val backgroundColor by animateColorAsState(
        targetValue = when {
            // 触发阈值后：
            // 置顶(Primary) vs 取消置顶(深灰色，保证可见度)
            offsetX > triggerThreshold -> if (entity.isPinned) Color.DarkGray else MaterialTheme.colorScheme.primary
            // 滑动过程中：
            offsetX > 0 -> if (entity.isPinned) Color.LightGray else MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        }
    )

    val days = remember(entity.dateStr, entity.isLunar) { calculateDays(entity.dateStr, entity.isLunar) }
    val displayDate = remember(entity.dateStr, entity.isLunar) {
        if (entity.isLunar) formatLunarDate(entity.dateStr) else entity.dateStr
    }
    
    val syncAction = {
        offsetX = 0f 
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
        // --- 背景层：右滑触发区 ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor, MaterialTheme.shapes.medium)
                .padding(start = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (offsetX > 50f) {
                val icon = if (entity.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin
                
                // 颜色逻辑：
                // 取消置顶时：白色图标
                // 置顶时：触发后反白，未触发主色
                val tint = if (entity.isPinned) {
                    Color.White
                } else {
                    if (isTriggered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(if (isTriggered) 32.dp else 24.dp)
                )
            }
        }

        // --- 背景层：左滑按钮 ---
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IconButton(onClick = { showSyncConfirm = true }) {
                Icon(Icons.Default.Refresh, "同步", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(modifier = Modifier.size(40.dp), onClick = { offsetX = 0f; onEdit() }) {
                Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(modifier = Modifier.size(40.dp), onClick = { offsetX = 0f; onDelete() }) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }

        // --- 前景层：生日卡片 ---
        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragCancel = { offsetX = 0f },
                        onDragEnd = {
                            if (offsetX > triggerThreshold) {
                                currentOnTogglePin() // 触发置顶/取消置顶
                            } else if (offsetX < rightActionWidth / 2) {
                                offsetX = rightActionWidth
                                return@detectHorizontalDragGestures
                            }
                            offsetX = 0f 
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = (offsetX + dragAmount).coerceIn(rightActionWidth - 50f, triggerThreshold + 100f)
                            offsetX = newOffset
                        }
                    )
                }
                .combinedClickable(
                    onClick = { 
                        if (offsetX != 0f) offsetX = 0f else onEdit() 
                    }
                ),
            colors = if (entity.isPinned) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            } else {
                CardDefaults.cardColors()
            }
        ) {
            Box(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entity.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
fun BirthdayListScreen(
    modifier: Modifier, 
    list: List<BirthdayEntity>, 
    onDelete: (BirthdayEntity) -> Unit, 
    onEdit: (BirthdayEntity) -> Unit,
    onTogglePin: (BirthdayEntity) -> Unit
) {
    var entityToDelete by remember { mutableStateOf<BirthdayEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (list.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(60.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "岁岁平安，从记录开始",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "还没有添加任何人的生日记录呢\n点击屏幕右上角的“+”按钮，记下每一个重要的日子吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(list, key = { it.id }) { entity ->
                    BirthdayItem(
                        entity = entity, 
                        onDelete = { entityToDelete = entity }, 
                        onEdit = { onEdit(entity) },
                        onTogglePin = { onTogglePin(entity) }
                    )
                }
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