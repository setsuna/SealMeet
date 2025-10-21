package com.xunyidi.sealmeet.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/**
 * SealMeet应用主题
 * 
 * @param darkTheme 是否使用深色主题（默认跟随系统，但会被ThemeManager覆盖）
 * @param content 应用内容
 */
@Composable
fun SealMeetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 监听主题配置变化
    val themeConfig by ThemeManager.currentConfig.collectAsState()
    
    // 转换为Material3 ColorScheme
    val colorScheme = remember(themeConfig) {
        themeConfig.toColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}
