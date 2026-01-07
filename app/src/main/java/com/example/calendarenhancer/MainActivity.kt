package com.example.calendarenhancer // 确保包名和你项目一致

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calendarenhancer.ui.theme.CalendarEnhancerTheme

// 1. 数据模型
data class BirthdayPerson(
    val id: Int,
    val name: String,
    val birthdayTag: String,
    val daysRemaining: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 启用沉浸式状态栏
        setContent {
            // 使用你项目自带的主题
            CalendarEnhancerTheme {
                // 这里的 Scaffold 是 Compose 的标准页面结构
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // 如果报错，可以先注释掉这行，或者导入相应的包
                        @OptIn(ExperimentalMaterial3Api::class)
                        CenterAlignedTopAppBar(title = { Text("生日增强器") })
                    }
                ) { innerPadding ->
                    // 2. 调用列表组件，并传入边距
                    BirthdayListScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// 模拟数据
val mockData = listOf(
    BirthdayPerson(1, "张三", "农历正月初五", 3),
    BirthdayPerson(2, "李四", "02-14", 12),
    BirthdayPerson(3, "王五", "农历八月十六", 150),
    BirthdayPerson(4, "老同事", "05-20", 45)
)

@Composable
fun BirthdayListScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(mockData) { person ->
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
                Text(text = person.birthdayTag, color = Color.Gray)
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