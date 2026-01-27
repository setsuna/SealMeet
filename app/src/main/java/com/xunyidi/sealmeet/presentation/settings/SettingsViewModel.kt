package com.xunyidi.sealmeet.presentation.settings

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.domain.usecase.ClearDataUseCase
import com.xunyidi.sealmeet.domain.usecase.ClearResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 设置页面ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val clearDataUseCase: ClearDataUseCase
) : BaseViewModel<SettingsContract.State, SettingsContract.Intent, SettingsContract.Effect>(
    SettingsContract.State()
) {
    
    init {
        // 加载配置
        loadSettings()
    }
    
    override fun handleIntent(intent: SettingsContract.Intent) {
        when (intent) {
            is SettingsContract.Intent.ToggleIncrementalUpdate -> {
                toggleIncrementalUpdate(intent.enabled)
            }
            is SettingsContract.Intent.ToggleServerConfigOverride -> {
                toggleServerConfigOverride(intent.enabled)
            }
            is SettingsContract.Intent.ToggleDeveloperMode -> {
                toggleDeveloperMode(intent.enabled)
            }
            is SettingsContract.Intent.ClearAllData -> {
                clearAllData()
            }
        }
    }
    
    /**
     * 加载配置
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // 监听增量更新配置
            launch {
                appPreferences.incrementalUpdateEnabled.collect { enabled ->
                    updateState { copy(incrementalUpdateEnabled = enabled) }
                }
            }
            
            // 监听允许服务器配置覆盖
            launch {
                appPreferences.allowServerConfigOverride.collect { enabled ->
                    updateState { copy(allowServerConfigOverride = enabled) }
                }
            }
            
            // 监听开发者模式
            launch {
                appPreferences.developerModeEnabled.collect { enabled ->
                    updateState { copy(developerModeEnabled = enabled) }
                }
            }
        }
    }
    
    /**
     * 切换增量更新开关
     */
    private fun toggleIncrementalUpdate(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appPreferences.setIncrementalUpdateEnabled(enabled)
                val message = if (enabled) {
                    "已启用增量更新"
                } else {
                    "已关闭增量更新，下次解包将清空所有数据"
                }
                sendEffect(SettingsContract.Effect.ShowToast(message))
                Timber.i("增量更新开关: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "切换增量更新开关失败")
                sendEffect(SettingsContract.Effect.ShowToast("设置失败"))
            }
        }
    }
    
    /**
     * 切换允许服务器配置覆盖开关
     */
    private fun toggleServerConfigOverride(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appPreferences.setAllowServerConfigOverride(enabled)
                val message = if (enabled) {
                    "已启用服务器配置覆盖"
                } else {
                    "已关闭服务器配置覆盖，保持本地设置"
                }
                sendEffect(SettingsContract.Effect.ShowToast(message))
                Timber.i("允许服务器配置覆盖开关: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "切换服务器配置覆盖开关失败")
                sendEffect(SettingsContract.Effect.ShowToast("设置失败"))
            }
        }
    }
    
    /**
     * 切换开发者模式开关
     * 开发者模式会同时启用保留临时文件功能
     */
    private fun toggleDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appPreferences.setDeveloperModeEnabled(enabled)
                // 开发者模式同时控制保留临时文件
                appPreferences.setKeepTempFilesEnabled(enabled)
                val message = if (enabled) {
                    "已启用开发者模式：使用 Download 目录，保留临时文件"
                } else {
                    "已关闭开发者模式：使用 /data/userdata/meetings，自动清理临时文件"
                }
                sendEffect(SettingsContract.Effect.ShowToast(message))
                Timber.i("开发者模式开关: $enabled, 保留临时文件: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "切换开发者模式开关失败")
                sendEffect(SettingsContract.Effect.ShowToast("设置失败"))
            }
        }
    }
    
    /**
     * 清空所有数据
     */
    private fun clearAllData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            
            try {
                Timber.i("开始清空所有数据...")
                
                val result = clearDataUseCase.clearAllData(writeAckFile = false)
                
                when (result) {
                    is ClearResult.Success -> {
                        sendEffect(SettingsContract.Effect.ShowToast("所有数据已清空"))
                        sendEffect(SettingsContract.Effect.DataCleared)
                    }
                    is ClearResult.Failure -> {
                        sendEffect(SettingsContract.Effect.ShowToast("清空数据失败: ${result.error}"))
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "清空数据失败")
                sendEffect(SettingsContract.Effect.ShowToast("清空数据失败: ${e.message}"))
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }
}
