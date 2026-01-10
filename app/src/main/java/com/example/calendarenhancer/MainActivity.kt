package com.example.calendarenhancer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.calendarenhancer.data.BirthdayEntity
import com.example.calendarenhancer.data.DatabaseProvider
import com.example.calendarenhancer.ui.theme.CalendarEnhancerTheme
import kotlinx.coroutines.launch
import com.example.calendarenhancer.ui.*

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarEnhancerTheme {
                val context = LocalContext.current
                val db = remember { DatabaseProvider.get(context) }
                val dao = remember { db.birthdayDao() }
                val scope = rememberCoroutineScope()

                val rawList by dao.getAll().collectAsState(initial = emptyList())
                var showDialog by remember { mutableStateOf(false) }
                var editingEntity by remember { mutableStateOf<BirthdayEntity?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { CenterAlignedTopAppBar(title = { Text("生日增强器") }) },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            editingEntity = null
                            showDialog = true
                        }) { Icon(Icons.Default.Add, "添加") }
                    }
                ) { innerPadding ->
                    BirthdayListScreen(
                        modifier = Modifier.padding(innerPadding),
                        list = rawList,
                        onDelete = { entity -> scope.launch { dao.delete(entity) } },
                        onEdit = { entity ->
                            editingEntity = entity
                            showDialog = true
                        }
                    )

                    if (showDialog) {
                        AddOrEditDialog(
                            initialEntity = editingEntity,
                            onDismiss = { showDialog = false },
                            onConfirm = { name, date, id ->
                                scope.launch {
                                    val entity = BirthdayEntity(id = id, name = name, dateStr = date)
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