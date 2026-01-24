package com.example.calendarenhancer.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.calendarenhancer.data.BirthdayDao
import com.example.calendarenhancer.data.BirthdayEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.filled.SystemUpdate
import com.example.calendarenhancer.util.UpdateManager
import com.example.calendarenhancer.data.GithubRelease
import com.example.calendarenhancer.BuildConfig

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    dao: BirthdayDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }

    // 导出逻辑
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val allData = dao.getAll().first()
                    val json = gson.toJson(allData)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(json.toByteArray())
                        }
                    }
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 导入逻辑
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                            reader.readText()
                        }
                    }
                    if (json != null) {
                        val type = object : TypeToken<List<BirthdayEntity>>() {}.type
                        val list: List<BirthdayEntity> = gson.fromJson(json, type)
                        dao.insertAll(list)
                        Toast.makeText(context, "导入成功，共 ${list.size} 条数据", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 更新相关状态
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<GithubRelease?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }

    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本: ${updateInfo?.tagName}") },
            text = {
                Column {
                    Text("更新内容:")
                    Text(updateInfo?.body ?: "暂无描述", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false
                        val asset = updateInfo?.assets?.firstOrNull { it.name.endsWith(".apk") }
                        if (asset != null) {
                            scope.launch {
                                try {
                                    downloadProgress = 0f
                                    UpdateManager.downloadApk(
                                        context,
                                        asset.downloadUrl,
                                        asset.name
                                    ).collect { progress ->
                                        downloadProgress = progress
                                    }
                                    downloadProgress = null // 完成
                                } catch (e: Exception) {
                                    downloadProgress = null
                                    Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "未找到安装包", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("下载更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (downloadProgress != null) {
        AlertDialog(
            onDismissRequest = { /* 禁止关闭 */ },
            title = { Text("正在下载...") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { downloadProgress!! },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(downloadProgress!! * 100).toInt()}%")
                }
            },
            confirmButton = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "显示设置",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "暗色模式",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = isDarkTheme,
                onCheckedChange = onThemeChange
            )
        }
        Text(
            text = "开启后将强制使用暗色主题",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "数据管理",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { exportLauncher.launch("birthdays_backup.json") },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导出数据到文件")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("从文件导入数据")
        }
        
        Text(
            text = "提示：导入的数据如果 ID 相同将会覆盖现有数据",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "关于与更新",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                if (!isChecking) {
                    isChecking = true
                    scope.launch {
                        val release = UpdateManager.checkUpdate()
                        isChecking = false
                        if (release != null) {
                            updateInfo = release
                            showUpdateDialog = true
                        } else {
                            Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            enabled = !isChecking,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("正在检测...")
            } else {
                Icon(Icons.Default.SystemUpdate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("检查更新")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "当前版本: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
