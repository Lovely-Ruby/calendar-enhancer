package com.example.calendarenhancer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calendarenhancer.data.BirthdayEntity
import com.example.calendarenhancer.data.DatabaseProvider
import com.example.calendarenhancer.ui.*
import com.example.calendarenhancer.ui.theme.CalendarEnhancerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 主题状态：初始跟随系统
            val systemInDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemInDark) }

            CalendarEnhancerTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val db = remember { DatabaseProvider.get(context) }
                val dao = remember { db.birthdayDao() }
                val scope = rememberCoroutineScope()

                val rawList by dao.getAll().collectAsState(initial = emptyList())
                var showDialog by remember { mutableStateOf(false) }
                var editingEntity by remember { mutableStateOf<BirthdayEntity?>(null) }

                // 获取当前路由，用于控制 UI 显示
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(if (currentRoute == "settings") "设置" else "岁岁念") }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.height(64.dp)
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.List, "列表") },
                                selected = currentRoute == "list" || currentRoute == null,
                                onClick = { navController.navigate("list") }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, "设置") },
                                selected = currentRoute == "settings",
                                onClick = { navController.navigate("settings") }
                            )
                        }
                    },
                    floatingActionButton = {
                        // 仅在列表页显示悬浮按钮
                        if (currentRoute == "list" || currentRoute == null) {
                            FloatingActionButton(onClick = {
                                editingEntity = null
                                showDialog = true
                            }) { Icon(Icons.Default.Add, "添加") }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "list",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("list") {
                            BirthdayListScreen(
                                modifier = Modifier.fillMaxSize(),
                                list = rawList,
                                onDelete = { entity -> scope.launch { dao.delete(entity) } },
                                onEdit = { entity ->
                                    editingEntity = entity
                                    showDialog = true
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                isDarkTheme = isDarkTheme,
                                onThemeChange = { isDarkTheme = it }
                            )
                        }
                    }

                    if (showDialog) {
                        AddOrEditDialog(
                            initialEntity = editingEntity,
                            onDismiss = { showDialog = false },
                            onConfirm = { name, date, isLunar, id ->
                                scope.launch {
                                    val entity = BirthdayEntity(id = id, name = name, dateStr = date, isLunar = isLunar)
                                    if (id == 0) dao.insert(entity) else dao.update(entity)
                                }
                                showDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}