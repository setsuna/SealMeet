package com.xunyidi.sealmeet.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xunyidi.sealmeet.presentation.settings.SettingsScreen

/**
 * 登录页示例
 * 展示如何在左下角添加设置按钮
 */
@Composable
fun LoginScreenExample() {
    var showSettings by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 主要的登录内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "登录页面",
                style = MaterialTheme.typography.headlineLarge
            )
            
            // ... 其他登录表单内容 ...
        }
        
        // 左下角的设置按钮
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // 显示设置对话框
    if (showSettings) {
        SettingsScreen(
            onDismiss = { showSettings = false }
        )
    }
}
