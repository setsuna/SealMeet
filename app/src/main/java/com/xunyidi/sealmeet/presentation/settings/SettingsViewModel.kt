package com.xunyidi.sealmeet.presentation.settings

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.local.database.AppDatabase
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * 设置页面ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val database: AppDatabase,
    private val context: android.content.Context
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
                
                // 使用 IO 线程执行数据库和文件操作
                withContext(Dispatchers.IO) {
                    // 1. 清空数据库
                    database.clearAllTables()
                    Timber.i("✅ 数据库已清空")
                    
                    // 2. 删除所有会议文件
                    val meetingsDir = File(context.filesDir, "meetings")
                    if (meetingsDir.exists()) {
                        val deleted = meetingsDir.deleteRecursively()
                        if (deleted) {
                            Timber.i("✅ 会议文件已删除")
                        } else {
                            Timber.w("⚠️ 会议文件删除失败")
                        }
                    }
                    
                    // 3. 删除临时文件
                    val cacheFiles = context.cacheDir.listFiles()
                    cacheFiles?.filter { it.name.startsWith("unpack_") }?.forEach { file ->
                        file.deleteRecursively()
                    }
                    Timber.i("✅ 临时文件已清理")
                }
                
                sendEffect(SettingsContract.Effect.ShowToast("所有数据已清空"))
                sendEffect(SettingsContract.Effect.DataCleared)
                
            } catch (e: Exception) {
                Timber.e(e, "清空数据失败")
                sendEffect(SettingsContract.Effect.ShowToast("清空数据失败: ${e.message}"))
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }
}
