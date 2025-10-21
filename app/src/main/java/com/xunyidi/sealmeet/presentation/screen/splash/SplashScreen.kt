package com.xunyidi.sealmeet.presentation.screen.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xunyidi.sealmeet.presentation.theme.AppColors
import com.xunyidi.sealmeet.presentation.theme.TextInverse
import com.xunyidi.sealmeet.presentation.theme.ThemeManager

/**
 * 启动页示例
 * 演示MVI架构和主题系统的使用
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgPage),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SealMeet",
                color = AppColors.textPrimary
            )
            
            Text(
                text = "离线会议平板应用",
                color = AppColors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 主题切换按钮示例
            Button(
                onClick = { ThemeManager.toggleTheme() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryDefault,
                    disabledContainerColor = AppColors.primaryDisabled
                )
            ) {
                Text("切换主题", color = TextInverse)
            }
            
            // 固定颜色示例
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge("成功", AppColors.success)
                StatusBadge("警告", AppColors.warning)
                StatusBadge("错误", AppColors.error)
                StatusBadge("信息", AppColors.info)
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = color)
    }
}
