package com.example.calendarenhancer // 建议保持包名一致，引用最方便

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calendarenhancer.data.BirthdayEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import com.nlf.calendar.LunarMonth
import com.nlf.calendar.LunarYear

// --- 3. UI 组件 ---
@OptIn(ExperimentalFoundationApi::class)

@Composable
fun BirthdayItem(entity: BirthdayEntity, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    // 计算倒计时天数
    val days = remember(entity.dateStr, entity.isLunar) { calculateDays(entity.dateStr, entity.isLunar) }
    // 格式化日期显示
    val displayDate = remember(entity.dateStr, entity.isLunar) {
        if (entity.isLunar) {
            formatLunarDate(entity.dateStr)
        } else {
            entity.dateStr
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(onClick = onEdit, onLongClick = { showMenu = true })
    ) {
        Box(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(entity.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (entity.isLunar) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
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
                    horizontalAlignment = androidx.compose.ui.Alignment.End,
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
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("删除", color = Color.Red) }, onClick = { onDelete(); showMenu = false })
            }
        }
    }
}

@Composable
fun BirthdayListScreen(modifier: Modifier, list: List<BirthdayEntity>, onDelete: (BirthdayEntity) -> Unit, onEdit: (BirthdayEntity) -> Unit) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(list, key = { it.id }) { entity ->
            BirthdayItem(entity, onDelete = { onDelete(entity) }, onEdit = { onEdit(entity) })
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
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isLunar, onCheckedChange = { isLunar = it })
                    Text("阴历生日")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val cal = Calendar.getInstance()
                    // 如果已有日期，解析出年月日作为初始值，否则用今天
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

// 辅助函数：将 "1990-03-04" 转换为 "一九九〇(庚午)年三月初四"
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
        // 兼容旧格式 MM-DD 和新格式 YYYY-MM-DD
        val month = if (parts.size == 3) parts[1].toInt() else parts[0].toInt()
        val day = if (parts.size == 3) parts[2].toInt() else parts[1].toInt()

        val targetDate = if (!isLunar) {
            var target = LocalDate.of(today.year, month, day)
            if (target.isBefore(today)) target = target.plusYears(1)
            target
        } else {
            val currentYear = today.year
            
            // 辅助函数：获取该年该月合法的农历日期（处理30日变29日的情况）
            fun getValidLunar(y: Int, m: Int, d: Int): Lunar {
                val lunarYear = LunarYear.fromYear(y)
                val lunarMonth = lunarYear.getMonth(m)
                val monthDays = lunarMonth?.dayCount ?: 30
                return Lunar.fromYmd(y, m, if (d > monthDays) monthDays else d)
            }

            // 尝试今年的阴历生日
            val lunarThisYear = getValidLunar(currentYear, month, day)
            val solarThisYear = lunarThisYear.solar
            var target = LocalDate.of(solarThisYear.year, solarThisYear.month, solarThisYear.day)
            
            if (target.isBefore(today)) {
                // 如果今年的已经过了，计算明年的
                
                val lunarNextYear = getValidLunar(currentYear + 1, month, day)
                val solarNextYear = lunarNextYear.solar
                target = LocalDate.of(solarNextYear.year, solarNextYear.month, solarNextYear.day)
            }
            target
        }
        
        ChronoUnit.DAYS.between(today, targetDate).toInt()
    } catch (e: Exception) { 0 }
}