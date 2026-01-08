package com.example.calendarenhancer

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calendarenhancer.ui.theme.CalendarEnhancerTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

data class BirthdayPerson(
    val id: Int,
    val name: String,
    val birthdayTag: String,
    val daysRemaining: Int
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarEnhancerTheme {
                val birthdayList = remember {
                    mutableStateListOf(
                        BirthdayPerson(1, "张三", "10-24", calculateDays("10-24")),
                        BirthdayPerson(2, "李四", "02-14", calculateDays("02-14"))
                    )
                }

                var showDialog by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { CenterAlignedTopAppBar(title = { Text("生日增强器") }) },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "添加")
                        }
                    }
                ) { innerPadding ->
                    BirthdayListScreen(
                        modifier = Modifier.padding(innerPadding),
                        list = birthdayList
                    )

                    if (showDialog) {
                        AddBirthdayDialog(
                            onDismiss = { showDialog = false },
                            onConfirm = { name, date ->
                                birthdayList.add(
                                    BirthdayPerson(
                                        id = birthdayList.size + 1,
                                        name = name,
                                        birthdayTag = date,
                                        daysRemaining = calculateDays(date)
                                    )
                                )
                                showDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// 辅助函数：计算公历生日距离今天还有几天 (前端逻辑类似)
fun calculateDays(dateStr: String): Int {
    val today = LocalDate.now()
    val parts = dateStr.split("-")
    val month = parts[0].toInt()
    val day = parts[1].toInt()

    // 先假设是今年的生日
    var nextBirthday = LocalDate.of(today.year, month, day)

    // 如果今年的生日已经过了，就计算明年的
    if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today)) {
        nextBirthday = nextBirthday.plusYears(1)
    }

    return ChronoUnit.DAYS.between(today, nextBirthday).toInt()
}

@Composable
fun AddBirthdayDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") } // 格式: MM-dd

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // 定义日期选择器弹窗
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            // 注意：月份从0开始，所以要+1
            val formattedDate = String.format("%02d-%02d", month + 1, dayOfMonth)
            selectedDate = formattedDate
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增生日") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // 只读的输入框，点击触发日期选择器
                OutlinedTextField(
                    value = if (selectedDate.isEmpty()) "点击选择日期" else selectedDate,
                    onValueChange = {},
                    label = { Text("生日日期") },
                    readOnly = true,
                    enabled = false, // 禁用原生输入
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() } // 点击弹出
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && selectedDate.isNotBlank()) {
                    onConfirm(name, selectedDate)
                }
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// --- 以下 BirthdayListScreen 和 BirthdayItem 保持不变 ---
@Composable
fun BirthdayListScreen(modifier: Modifier = Modifier, list: List<BirthdayPerson>) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(list, key = { it.id }) { person ->
            BirthdayItem(person)
        }
    }
}

@Composable
fun BirthdayItem(person: BirthdayPerson) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = person.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "公历: ${person.birthdayTag}", color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${person.daysRemaining}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (person.daysRemaining < 10) Color.Red else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text(text = "天后", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}