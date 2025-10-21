package com.xunyidi.sealmeet.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 主题颜色扩展属性
 * 方便在Compose中快速访问主题颜色
 */

/**
 * 可配置颜色 - 主品牌色
 */
object AppColors {
    
    // 主色调状态
    val primaryDefault: Color
        @Composable get() = ThemeManager.getPrimaryColor()
    
    val primaryHover: Color
        @Composable get() = ThemeManager.getPrimaryHoverColor()
    
    val primaryActive: Color
        @Composable get() = ThemeManager.getPrimaryActiveColor()
    
    val primaryDisabled: Color
        @Composable get() = ThemeManager.getPrimaryDisabledColor()
    
    // 背景色
    val bgPage: Color
        @Composable get() = ThemeManager.getBgPageColor()
    
    val bgCard: Color
        @Composable get() = ThemeManager.getBgCardColor()
    
    val bgContainer: Color
        @Composable get() = ThemeManager.getBgContainerColor()
    
    // 文字颜色
    val textPrimary: Color
        @Composable get() = ThemeManager.getTextPrimaryColor()
    
    val textSecondary: Color
        @Composable get() = ThemeManager.getTextSecondaryColor()
    
    val textTertiary: Color
        @Composable get() = if (ThemeManager.getCurrentConfig().isDark) TextTertiaryDark else TextTertiary
    
    // 固定颜色（全局常量，不随主题变化）
    val success: Color = ColorSuccess
    val warning: Color = ColorWarning
    val error: Color = ColorError
    val info: Color = ColorInfo
    
    // 边框
    val border: Color
        @Composable get() = ThemeManager.getBorderColor()
    
    // 自动计算颜色
    val primaryOverlay: Color
        @Composable get() = ColorUtils.getOverlayColor(primaryDefault)
    
    val primaryLightBg: Color
        @Composable get() = ColorUtils.getLightBgColor(primaryDefault)
    
    val primaryBorderActive: Color
        @Composable get() = ColorUtils.getBorderActiveColor(primaryDefault)
    
    val primaryRipple: Color
        @Composable get() = ColorUtils.getRippleColor(primaryDefault)
}

/**
 * 使用示例：
 * ```
 * Box(
 *     modifier = Modifier.background(AppColors.bgPage)
 * ) {
 *     Text(
 *         text = "Hello",
 *         color = AppColors.textPrimary
 *     )
 * }
 * ```
 */
