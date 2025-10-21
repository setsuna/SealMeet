package com.xunyidi.sealmeet.presentation.theme

import androidx.compose.ui.graphics.Color

// ============================================
// 一、可配置颜色（Theme Variables）
// 这些颜色会根据主题切换而改变
// ============================================

// 1. 主品牌色系统 - 浅色主题（占位符，等待用户提供配色）
val LightPrimaryDefault = Color(0xFFBE0110)      // 主色-默认态
val LightPrimaryHover = Color(0xFF9C0D0E)        // 主色-悬停态
val LightPrimaryActive = Color(0xFF7A0A0B)       // 主色-点击态
val LightPrimaryDisabled = Color(0xFFE6989D)     // 主色-禁用态

// 2. 辅助品牌色 - 浅色主题（可选）
val LightSecondaryDefault = Color(0xFF1890FF)    // 辅助色-默认
val LightSecondaryHover = Color(0xFF0B7DD6)      // 辅助色-悬停
val LightSecondaryActive = Color(0xFF0960A8)     // 辅助色-点击

// 3. 背景色系统 - 浅色主题
val LightBgPage = Color(0xFFFAFAFA)              // 页面主背景
val LightBgContainer = Color(0xFFEFF2F5)         // 容器次级背景
val LightBgCard = Color(0xFFFFFFFF)              // 卡片背景
val LightBgElevated = Color(0xFFFFFFFF)          // 悬浮层背景

// 1. 主品牌色系统 - 深色主题（占位符）
val DarkPrimaryDefault = Color(0xFFFF4D5A)       // 主色-默认态
val DarkPrimaryHover = Color(0xFFFF6B75)         // 主色-悬停态
val DarkPrimaryActive = Color(0xFFFF8A93)        // 主色-点击态
val DarkPrimaryDisabled = Color(0xFF5A2A2D)      // 主色-禁用态

// 2. 辅助品牌色 - 深色主题
val DarkSecondaryDefault = Color(0xFF3AA0FF)     // 辅助色-默认
val DarkSecondaryHover = Color(0xFF5BB0FF)       // 辅助色-悬停
val DarkSecondaryActive = Color(0xFF7CC0FF)      // 辅助色-点击

// 3. 背景色系统 - 深色主题
val DarkBgPage = Color(0xFF121212)               // 页面主背景
val DarkBgContainer = Color(0xFF1E1E1E)          // 容器次级背景
val DarkBgCard = Color(0xFF2C2C2C)               // 卡片背景
val DarkBgElevated = Color(0xFF383838)           // 悬浮层背景

// ============================================
// 二、固定不变的颜色（Global Constants）
// 这些颜色不随主题变化
// ============================================

// 1. 功能语义色
val ColorSuccess = Color(0xFF52C41A)             // 成功-绿
val ColorWarning = Color(0xFFFAAD14)             // 警告-黄
val ColorError = Color(0xFFFF2600)               // 错误-红
val ColorInfo = Color(0xFF1B8CF6)                // 信息-蓝

// 2. 文字颜色系统（浅色模式）
val TextPrimary = Color(0xFF404040)              // 一级标题
val TextSecondary = Color(0xFF545454)            // 二级标题
val TextRegular = Color(0xFF848484)              // 正文
val TextTertiary = Color(0xFFBFBFBF)             // 辅助文字
val TextInverse = Color(0xFFFFFFFF)              // 反色文字

// 2. 文字颜色系统（深色模式）
val TextPrimaryDark = Color(0xFFE5E5E5)          // 一级标题
val TextSecondaryDark = Color(0xFFB8B8B8)        // 二级标题
val TextRegularDark = Color(0xFF8C8C8C)          // 正文
val TextTertiaryDark = Color(0xFF595959)         // 辅助文字

// 3. 边框与分割线
val ColorBorder = Color(0xFFDBDBDB)              // 边框线
val ColorDivider = Color(0xFFE8E8E8)             // 分割线

// 3. 边框与分割线（深色模式）
val ColorBorderDark = Color(0xFF404040)          // 边框线
val ColorDividerDark = Color(0xFF303030)         // 分割线
