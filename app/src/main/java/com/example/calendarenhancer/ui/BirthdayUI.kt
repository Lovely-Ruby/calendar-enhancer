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

// --- 3. UI 组件 ---
@OptIn(ExperimentalFoundationApi::class)

@Composable
fun BirthdayItem(entity: BirthdayEntity, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    // 计算倒计时天数
    val days = remember(entity.dateStr) { calculateDays(entity.dateStr) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(onClick = onEdit, onLongClick = { showMenu = true })
    ) {
        Box(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(entity.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("日期: ${entity.dateStr}", color = Color.Gray)
                }
                Text("${days}天后", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
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
fun AddOrEditDialog(initialEntity: BirthdayEntity?, onDismiss: () -> Unit, onConfirm: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf(initialEntity?.name ?: "") }
    var date by remember { mutableStateOf(initialEntity?.dateStr ?: "") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntity == null) "新增生日" else "编辑生日") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") })
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, _, m, d -> date = String.format("%02d-%02d", m + 1, d) },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text(if(date.isEmpty()) "选择日期" else date) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if(name.isNotBlank() && date.isNotBlank()) onConfirm(name, date, initialEntity?.id ?: 0) }) { Text("确定") }
        }
    )
}

fun calculateDays(dateStr: String): Int {
    return try {
        val today = LocalDate.now()
        val parts = dateStr.split("-")
        var target = LocalDate.of(today.year, parts[0].toInt(), parts[1].toInt())
        if (target.isBefore(today)) target = target.plusYears(1)
        ChronoUnit.DAYS.between(today, target).toInt()
    } catch (e: Exception) { 0 }
}