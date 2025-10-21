package com.xunyidi.sealmeet.presentation.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * 主题管理器（单例）
 * 负责管理应用的主题切换和持久化
 */
object ThemeManager {
    
    private val _currentTheme = MutableStateFlow(ThemeType.LIGHT)
    val currentTheme: StateFlow<ThemeType> = _currentTheme.asStateFlow()

    private val _currentConfig = MutableStateFlow(ThemeConfig.getTheme(ThemeType.LIGHT))
    val currentConfig: StateFlow<ThemeConfig> = _currentConfig.asStateFlow()

    /**
     * 切换主题
     */
    fun switchTheme(theme: ThemeType) {
        _currentTheme.value = theme
        _currentConfig.value = ThemeConfig.getTheme(theme)
        Timber.d("Theme switched to: ${theme.name}")
    }

    /**
     * 应用自定义主题配置
     */
    fun applyCustomTheme(config: ThemeConfig) {
        if (ThemeValidator.validateTheme(config)) {
            _currentTheme.value = ThemeType.CUSTOM
            _currentConfig.value = config
            Timber.d("Custom theme applied: ${config.themeName}")
        } else {
            Timber.e("Invalid theme config: ${config.themeName}")
        }
    }

    /**
     * 获取当前主题类型
     */
    fun getCurrentThemeType(): ThemeType {
        return _currentTheme.value
    }

    /**
     * 获取当前主题配置
     */
    fun getCurrentConfig(): ThemeConfig {
        return _currentConfig.value
    }

    /**
     * 切换到深色模式
     */
    fun switchToDarkMode() {
        switchTheme(ThemeType.DARK)
    }

    /**
     * 切换到浅色模式
     */
    fun switchToLightMode() {
        switchTheme(ThemeType.LIGHT)
    }

    /**
     * 切换主题（浅色⇄深色）
     */
    fun toggleTheme() {
        val newTheme = when (_currentTheme.value) {
            ThemeType.LIGHT -> ThemeType.DARK
            ThemeType.DARK -> ThemeType.LIGHT
            ThemeType.CUSTOM -> {
                // 自定义主题根据isDark属性切换
                if (_currentConfig.value.isDark) ThemeType.LIGHT else ThemeType.DARK
            }
        }
        switchTheme(newTheme)
    }

    /**
     * 获取主色调（默认态）
     */
    fun getPrimaryColor(): androidx.compose.ui.graphics.Color {
        return _currentConfig.value.colors.primary.default
    }

    /**
     * 获取主色调（悬停态）
     */
    fun getPrimaryHoverColor(): androidx.compose.ui.graphics.Color {
        return _currentConfig.value.colors.primary.hover
    }

    /**
     * 获取主色调（点击态）
     */
    fun getPrimaryActiveColor(): androidx.compose.ui.graphics.Color {
        return _currentConfig.value.colors.primary.active
    }

    /**
     * 获取主色调（禁用态）
     */
    fun getPrimaryDisabledColor(): androidx.compose.ui.graphics.Color {
        return _currentConfig.value.colors.primary.disabled
    }

    /**
     * 获取背景色-页面
     */
    fun getBgPageColor(): androidx.compose.ui.graphics.Color {
        return _currentConfig.value.colors.background.page
    }

    /**
     * 获取背景色-卡片
     */
    fun getBgCardColor(): androidx.compose.ui.graphics.Color {
        return _currentConfig.value.colors.background.card
    }

    /**
     * 获取背景色-容器
     */
    fun getBgContainerColor(): androidx.compose.ui.graphics.Color {
        return _currentConfig.value.colors.background.container
    }

    /**
     * 获取当前主题的文字主色
     */
    fun getTextPrimaryColor(): androidx.compose.ui.graphics.Color {
        return if (_currentConfig.value.isDark) TextPrimaryDark else TextPrimary
    }

    /**
     * 获取当前主题的文字次要色
     */
    fun getTextSecondaryColor(): androidx.compose.ui.graphics.Color {
        return if (_currentConfig.value.isDark) TextSecondaryDark else TextSecondary
    }

    /**
     * 获取当前主题的边框色
     */
    fun getBorderColor(): androidx.compose.ui.graphics.Color {
        return if (_currentConfig.value.isDark) ColorBorderDark else ColorBorder
    }
}
