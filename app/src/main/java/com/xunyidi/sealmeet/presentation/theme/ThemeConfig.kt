package com.xunyidi.sealmeet.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * 主题类型枚举
 */
enum class ThemeType {
    LIGHT,    // 浅色主题
    DARK,     // 深色主题
    CUSTOM    // 自定义主题（预留）
}

/**
 * 主题颜色状态（包含default/hover/active/disabled）
 */
data class ColorState(
    val default: androidx.compose.ui.graphics.Color,
    val hover: androidx.compose.ui.graphics.Color,
    val active: androidx.compose.ui.graphics.Color,
    val disabled: androidx.compose.ui.graphics.Color
)

/**
 * 背景颜色系统
 */
data class BackgroundColors(
    val page: androidx.compose.ui.graphics.Color,
    val container: androidx.compose.ui.graphics.Color,
    val card: androidx.compose.ui.graphics.Color,
    val elevated: androidx.compose.ui.graphics.Color
)

/**
 * 主题颜色配置
 */
data class ThemeColors(
    val primary: ColorState,
    val secondary: ColorState? = null,  // 辅助色（可选）
    val background: BackgroundColors
)

/**
 * 主题配置数据类
 */
data class ThemeConfig(
    val themeName: String,
    val themeId: String,
    val colors: ThemeColors,
    val isDark: Boolean = false
) {
    companion object {
        
        /**
         * 获取指定类型的主题配置
         */
        fun getTheme(type: ThemeType): ThemeConfig {
            return when (type) {
                ThemeType.LIGHT -> lightTheme()
                ThemeType.DARK -> darkTheme()
                ThemeType.CUSTOM -> customTheme()
            }
        }

        /**
         * 浅色主题配置
         */
        private fun lightTheme(): ThemeConfig {
            return ThemeConfig(
                themeName = "浅色主题",
                themeId = "light",
                isDark = false,
                colors = ThemeColors(
                    primary = ColorState(
                        default = LightPrimaryDefault,
                        hover = LightPrimaryHover,
                        active = LightPrimaryActive,
                        disabled = LightPrimaryDisabled
                    ),
                    secondary = ColorState(
                        default = LightSecondaryDefault,
                        hover = LightSecondaryHover,
                        active = LightSecondaryActive,
                        disabled = LightSecondaryDefault.copy(alpha = 0.4f)
                    ),
                    background = BackgroundColors(
                        page = LightBgPage,
                        container = LightBgContainer,
                        card = LightBgCard,
                        elevated = LightBgElevated
                    )
                )
            )
        }

        /**
         * 深色主题配置
         */
        private fun darkTheme(): ThemeConfig {
            return ThemeConfig(
                themeName = "深色主题",
                themeId = "dark",
                isDark = true,
                colors = ThemeColors(
                    primary = ColorState(
                        default = DarkPrimaryDefault,
                        hover = DarkPrimaryHover,
                        active = DarkPrimaryActive,
                        disabled = DarkPrimaryDisabled
                    ),
                    secondary = ColorState(
                        default = DarkSecondaryDefault,
                        hover = DarkSecondaryHover,
                        active = DarkSecondaryActive,
                        disabled = DarkSecondaryDefault.copy(alpha = 0.4f)
                    ),
                    background = BackgroundColors(
                        page = DarkBgPage,
                        container = DarkBgContainer,
                        card = DarkBgCard,
                        elevated = DarkBgElevated
                    )
                )
            )
        }

        /**
         * 自定义主题配置（预留，等待用户提供配色方案）
         */
        private fun customTheme(): ThemeConfig {
            return lightTheme() // 暂时返回浅色主题
        }
    }

    /**
     * 转换为Material3 ColorScheme
     */
    fun toColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = colors.primary.default,
                secondary = colors.secondary?.default ?: colors.primary.default,
                background = colors.background.page,
                surface = colors.background.card,
                surfaceVariant = colors.background.container,
                error = ColorError,
                
                // 文字颜色
                onPrimary = TextInverse,
                onSecondary = TextInverse,
                onBackground = TextPrimaryDark,
                onSurface = TextPrimaryDark,
                onError = TextInverse,
                
                // 边框和分割线
                outline = ColorBorderDark,
                outlineVariant = ColorDividerDark
            )
        } else {
            lightColorScheme(
                primary = colors.primary.default,
                secondary = colors.secondary?.default ?: colors.primary.default,
                background = colors.background.page,
                surface = colors.background.card,
                surfaceVariant = colors.background.container,
                error = ColorError,
                
                // 文字颜色
                onPrimary = TextInverse,
                onSecondary = TextInverse,
                onBackground = TextPrimary,
                onSurface = TextPrimary,
                onError = TextInverse,
                
                // 边框和分割线
                outline = ColorBorder,
                outlineVariant = ColorDivider
            )
        }
    }
}

/**
 * 主题配置验证
 */
object ThemeValidator {
    
    /**
     * 验证主题配置的合法性
     */
    fun validateTheme(theme: ThemeConfig): Boolean {
        // TODO: 添加颜色对比度检查、色值格式检查等
        return theme.themeName.isNotEmpty() && theme.themeId.isNotEmpty()
    }
}
