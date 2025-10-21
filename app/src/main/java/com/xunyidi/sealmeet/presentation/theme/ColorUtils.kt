package com.xunyidi.sealmeet.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 颜色计算工具类
 * 用于自动生成半透明色、浅色背景等衍生颜色
 */
object ColorUtils {

    /**
     * 生成背景蒙层色（主色 + 60%透明度）
     * 用于Modal、Dialog遮罩层
     */
    fun getOverlayColor(primaryColor: Color): Color {
        return primaryColor.copy(alpha = 0.6f)
    }

    /**
     * 生成浅色背景（主色 + 10%透明度）
     * 用于选中态、悬停态的背景色
     */
    fun getLightBgColor(primaryColor: Color): Color {
        return primaryColor.copy(alpha = 0.1f)
    }

    /**
     * 生成边框高亮色（主色 + 40%透明度）
     * 用于聚焦态的边框
     */
    fun getBorderActiveColor(primaryColor: Color): Color {
        return primaryColor.copy(alpha = 0.4f)
    }

    /**
     * 生成涟漪效果色（主色 + 20%透明度）
     * 用于点击涟漪效果
     */
    fun getRippleColor(primaryColor: Color): Color {
        return primaryColor.copy(alpha = 0.2f)
    }

    /**
     * 根据背景色自动选择文字颜色（确保对比度）
     * @param backgroundColor 背景色
     * @return 深色或浅色文字
     */
    fun getContrastTextColor(backgroundColor: Color): Color {
        // 计算亮度（简化版）
        val luminance = (0.299 * backgroundColor.red + 
                        0.587 * backgroundColor.green + 
                        0.114 * backgroundColor.blue)
        
        // 亮度大于0.5使用深色文字，否则使用浅色文字
        return if (luminance > 0.5f) {
            TextPrimary
        } else {
            TextInverse
        }
    }

    /**
     * 颜色变暗
     * @param color 原始颜色
     * @param factor 变暗因子（0.0 - 1.0）
     */
    fun darken(color: Color, factor: Float = 0.2f): Color {
        return Color(
            red = (color.red * (1 - factor)).coerceIn(0f, 1f),
            green = (color.green * (1 - factor)).coerceIn(0f, 1f),
            blue = (color.blue * (1 - factor)).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }

    /**
     * 颜色变亮
     * @param color 原始颜色
     * @param factor 变亮因子（0.0 - 1.0）
     */
    fun lighten(color: Color, factor: Float = 0.2f): Color {
        return Color(
            red = (color.red + (1 - color.red) * factor).coerceIn(0f, 1f),
            green = (color.green + (1 - color.green) * factor).coerceIn(0f, 1f),
            blue = (color.blue + (1 - color.blue) * factor).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }
}
