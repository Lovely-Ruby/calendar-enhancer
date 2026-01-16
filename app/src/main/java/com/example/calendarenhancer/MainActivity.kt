package com.example.calendarenhancer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calendarenhancer.data.BirthdayEntity
import com.example.calendarenhancer.data.DatabaseProvider
import com.example.calendarenhancer.ui.*
import com.example.calendarenhancer.ui.theme.CalendarEnhancerTheme
import kotlinx.coroutines.launch

import com.example.calendarenhancer.util.CalendarUtil
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        setContent {
            // 主题状态：默认亮色 (false)，从 SharedPreferences 读取
            var isDarkTheme by remember { 
                mutableStateOf(sharedPref.getBoolean("is_dark_theme", false)) 
            }

            // 创建一个受状态驱动的“今天”日期
            var today by remember { mutableStateOf(LocalDate.now()) }
            
            // 监听生命周期，回到前台时刷新日期
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        today = LocalDate.now()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            CalendarEnhancerTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val db = remember { DatabaseProvider.get(context) }
                val dao = remember { db.birthdayDao() }
                val scope = rememberCoroutineScope()

                val rawList by dao.getAll().collectAsState(initial = emptyList())
                var isSortedByDays by remember { mutableStateOf(true) } // 默认按天数排序
                
                // 处理排序逻辑
                val displayList = remember(rawList, isSortedByDays, today) {
                    if (isSortedByDays) {
                        rawList.sortedWith(
                            compareByDescending<BirthdayEntity> { it.isPinned }
                                .thenBy { CalendarUtil.calculateDays(it.dateStr, it.isLunar, today) }
                        )
                    } else {
                        rawList // 默认由 DAO 排序 (Pinned -> ID)
                    }
                }

                var showDialog by remember { mutableStateOf(false) }
                var editingEntity by remember { mutableStateOf<BirthdayEntity?>(null) }

                // 获取当前路由，用于控制 UI 显示
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(if (currentRoute == "settings") "设置" else "岁岁念") },
                            actions = {
                                if (currentRoute == "list" || currentRoute == null) {
                                    IconButton(onClick = { isSortedByDays = !isSortedByDays }) {
                                        Icon(
                                            imageVector = Icons.Default.Sort, 
                                            contentDescription = "排序",
                                            tint = if (isSortedByDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingEntity = null
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "添加")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.height(64.dp)
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.List, "列表") },
                                selected = currentRoute == "list" || currentRoute == null,
                                onClick = {
                                    if (currentRoute != "list") {
                                        navController.navigate("list") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, "设置") },
                                selected = currentRoute == "settings",
                                onClick = {
                                    if (currentRoute != "settings") {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
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
                                list = displayList, // 使用排序后的列表
                                today = today, // 传递最新的日期
                                onDelete = { entity -> scope.launch { dao.delete(entity) } },
                                onEdit = { entity ->
                                    editingEntity = entity
                                    showDialog = true
                                },
                                onTogglePin = { entity ->
                                    scope.launch {
                                        val newStatus = !entity.isPinned
                                        dao.updatePinStatus(entity.id, newStatus)
                                        // 添加提示
                                        android.widget.Toast.makeText(context, if (newStatus) "已置顶" else "已取消置顶", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                isDarkTheme = isDarkTheme,
                                onThemeChange = { 
                                    isDarkTheme = it
                                    sharedPref.edit().putBoolean("is_dark_theme", it).apply()
                                },
                                dao = dao
                            )
                        }
                    }

                    if (showDialog) {
                        AddOrEditDialog(
                            initialEntity = editingEntity,
                            onDismiss = { showDialog = false },
                            onConfirm = { name, date, isLunar, id ->
                                scope.launch {
                                    // 如果是编辑现有记录 (id != 0)，我们需要保留它原来的 isPinned 状态
                                    // 但是 editingEntity 可能是旧的，我们需要确保拿到最新的状态
                                    // 这里简单处理：如果 initialEntity 不为空且 id 匹配，则沿用其 isPinned
                                    // 更好的做法是从数据库查，或者由 UI 传递当前的 isPinned
                                    val currentPinned = if (id != 0 && editingEntity?.id == id) editingEntity?.isPinned ?: false else false
                                    
                                    val entity = BirthdayEntity(
                                        id = id, 
                                        name = name, 
                                        dateStr = date, 
                                        isLunar = isLunar, 
                                        isPinned = currentPinned
                                    )
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
