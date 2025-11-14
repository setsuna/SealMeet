package com.xunyidi.sealmeet.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xunyidi.sealmeet.presentation.theme.AppColors

/**
 * 设置页面
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    // 处理副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsContract.Effect.DataCleared -> {
                    // 数据清空后可以做些什么，比如刷新列表
                }
            }
        }
    }
    
    // 设置对话框
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("开发设置", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 增量更新开关
                SettingItem(
                    title = "增量更新",
                    description = if (state.incrementalUpdateEnabled) {
                        "启用时，解包会保留现有数据并更新"
                    } else {
                        "关闭时，每次解包前会清空所有数据"
                    },
                    checked = state.incrementalUpdateEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.handleIntent(
                            SettingsContract.Intent.ToggleIncrementalUpdate(enabled)
                        )
                    }
                )
                
                Divider()
                
                // 保留临时文件开关
                SettingItem(
                    title = "保留临时文件",
                    description = if (state.keepTempFilesEnabled) {
                        "已启用，可查看 /cache/unpack_* 目录"
                    } else {
                        "解包后自动删除临时文件"
                    },
                    checked = state.keepTempFilesEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.handleIntent(
                            SettingsContract.Intent.ToggleKeepTempFiles(enabled)
                        )
                    }
                )
                
                Divider()
                
                // 允许服务器配置覆盖开关
                SettingItem(
                    title = "允许服务器配置覆盖",
                    description = if (state.allowServerConfigOverride) {
                        "服务器配置会覆盖本地设置"
                    } else {
                        "保持本地配置，忽略服务器配置"
                    },
                    checked = state.allowServerConfigOverride,
                    onCheckedChange = { enabled ->
                        viewModel.handleIntent(
                            SettingsContract.Intent.ToggleServerConfigOverride(enabled)
                        )
                    }
                )
                
                Divider()
                
                // 开发者模式开关
                SettingItem(
                    title = "开发者模式",
                    description = if (state.developerModeEnabled) {
                        "已启用，使用 Download 目录"
                    } else {
                        "已关闭，使用 /data/userdata/meetings"
                    },
                    checked = state.developerModeEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.handleIntent(
                            SettingsContract.Intent.ToggleDeveloperMode(enabled)
                        )
                    }
                )
                
                Divider()
                
                // 清空数据按钮
                Button(
                    onClick = { showClearDataDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !state.isLoading
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isLoading) "清空中..." else "清空所有数据")
                }
                
                // 说明文字
                Text(
                    text = "⚠️ 清空数据将删除所有会议、文件和数据库内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
    
    // 清空数据确认对话框
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("确认清空数据") },
            text = {
                Text("此操作将删除：\n\n• 所有会议数据\n• 所有文件\n• 所有数据库内容\n\n确定要继续吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.handleIntent(SettingsContract.Intent.ClearAllData)
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 设置项组件
 */
@Composable
private fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
