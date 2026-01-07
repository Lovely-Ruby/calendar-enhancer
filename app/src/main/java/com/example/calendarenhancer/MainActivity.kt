package com.example.birthdayapp // 确保包名和你创建项目时一致

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.sp

// 1. 定义数据模型（类似前端的 Interface）
data class BirthdayPerson(
    val id: Int,
    val name: String,
    val birthdayTag: String, // 比如 "农历八月十五" 或 "10-24"
    val daysRemaining: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 设置主题
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BirthdayListScreen()
                }
            }
        }
    }
}

// 2. 模拟数据 (Mock Data)
val mockData = listOf(
    BirthdayPerson(1, "张三", "农历正月初五", 3),
    BirthdayPerson(2, "李四", "02-14", 12),
    BirthdayPerson(3, "王五", "农历八月十六", 150),
    BirthdayPerson(4, "赵六", "12-25", 350),
    BirthdayPerson(5, "前端老同事", "05-20", 45)
)

// 3. 主界面组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayListScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("生日增强器") })
        }
    ) { innerPadding ->
        // LazyColumn 相当于前端的 虚拟列表 (Virtual List)，性能很高
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(mockData) { person ->
                BirthdayItem(person)
            }
        }
    }
}

// 4. 列表项组件 (相当于一个个的组件卡片)
@Composable
fun BirthdayItem(person: BirthdayPerson) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧信息
            Column {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = person.birthdayTag,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // 右侧倒计时
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${person.daysRemaining}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (person.daysRemaining < 10) Color.Red else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "天后生日",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}