package com.xunyidi.sealmeet.presentation.settings

import com.xunyidi.sealmeet.core.mvi.UiEffect
import com.xunyidi.sealmeet.core.mvi.UiIntent
import com.xunyidi.sealmeet.core.mvi.UiState

/**
 * 设置页面契约
 */
object SettingsContract {
    
    /**
     * UI状态
     */
    data class State(
        val incrementalUpdateEnabled: Boolean = true,
        val keepTempFilesEnabled: Boolean = false,
        val allowServerConfigOverride: Boolean = true,
        val isLoading: Boolean = false
    ) : UiState
    
    /**
     * 用户意图
     */
    sealed interface Intent : UiIntent {
        /**
         * 切换增量更新开关
         */
        data class ToggleIncrementalUpdate(val enabled: Boolean) : Intent
        
        /**
         * 切换保留临时文件开关
         */
        data class ToggleKeepTempFiles(val enabled: Boolean) : Intent
        
        /**
         * 切换允许服务器配置覆盖开关
         */
        data class ToggleServerConfigOverride(val enabled: Boolean) : Intent
        
        /**
         * 清空所有数据
         */
        data object ClearAllData : Intent
    }
    
    /**
     * 副作用
     */
    sealed interface Effect : UiEffect {
        /**
         * 显示Toast消息
         */
        data class ShowToast(val message: String) : Effect
        
        /**
         * 数据清空成功
         */
        data object DataCleared : Effect
    }
}
