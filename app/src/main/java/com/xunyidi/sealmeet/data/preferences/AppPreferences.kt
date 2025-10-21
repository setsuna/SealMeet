package com.xunyidi.sealmeet.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用配置存储
 * 使用DataStore持久化配置项
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
        
        // 配置键
        private val KEY_INCREMENTAL_UPDATE = booleanPreferencesKey("incremental_update_enabled")
        private val KEY_KEEP_TEMP_FILES = booleanPreferencesKey("keep_temp_files_enabled")
    }
    
    /**
     * 增量更新开关
     * true: 启用增量更新（默认）
     * false: 关闭增量更新，每次解包前清空数据
     */
    val incrementalUpdateEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_INCREMENTAL_UPDATE] ?: true // 默认启用
        }
    
    /**
     * 保留临时文件开关
     * true: 保留解包后的原始文件结构（方便调试）
     * false: 解包后删除临时文件（默认）
     */
    val keepTempFilesEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_KEEP_TEMP_FILES] ?: false // 默认不保留
        }
    
    /**
     * 设置增量更新开关
     */
    suspend fun setIncrementalUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_INCREMENTAL_UPDATE] = enabled
        }
    }
    
    /**
     * 设置保留临时文件开关
     */
    suspend fun setKeepTempFilesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KEEP_TEMP_FILES] = enabled
        }
    }
}
