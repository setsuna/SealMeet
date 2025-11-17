package com.xunyidi.sealmeet.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        private val KEY_ALLOW_SERVER_OVERRIDE = booleanPreferencesKey("allow_server_config_override")
        private val KEY_DEVELOPER_MODE = booleanPreferencesKey("developer_mode_enabled")
        
        // 登录用户信息键
        private val KEY_CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val KEY_CURRENT_USER_NAME = stringPreferencesKey("current_user_name")
        private val KEY_CURRENT_MEETING_ID = stringPreferencesKey("current_meeting_id")
        private val KEY_CURRENT_USER_ROLE = stringPreferencesKey("current_user_role")
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
     * 允许服务器配置覆盖本地配置
     * true: 服务器下发的config.json会覆盖本地配置（默认）
     * false: 忽略服务器配置，保持本地配置
     */
    val allowServerConfigOverride: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ALLOW_SERVER_OVERRIDE] ?: true // 默认允许
        }
    
    /**
     * 开发者模式开关
     * true: 开发者模式，从 Download 目录读取会议包
     * false: 生产者模式，从 /data/userdata/meetings 读取会议包（默认）
     */
    val developerModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEVELOPER_MODE] ?: false // 默认为生产者模式
        }
    
    /**
     * 当前登录用户ID
     */
    val currentUserId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CURRENT_USER_ID]
        }
    
    /**
     * 当前登录用户名
     */
    val currentUserName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CURRENT_USER_NAME]
        }
    
    /**
     * 当前会议ID
     */
    val currentMeetingId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CURRENT_MEETING_ID]
        }
    
    /**
     * 当前用户角色
     */
    val currentUserRole: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CURRENT_USER_ROLE]
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
    
    /**
     * 设置允许服务器配置覆盖开关
     */
    suspend fun setAllowServerConfigOverride(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ALLOW_SERVER_OVERRIDE] = enabled
        }
    }
    
    /**
     * 设置开发者模式开关
     */
    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVELOPER_MODE] = enabled
        }
    }
    
    /**
     * 保存当前登录用户信息
     */
    suspend fun setCurrentUser(
        userId: String,
        userName: String,
        meetingId: String,
        role: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENT_USER_ID] = userId
            preferences[KEY_CURRENT_USER_NAME] = userName
            preferences[KEY_CURRENT_MEETING_ID] = meetingId
            preferences[KEY_CURRENT_USER_ROLE] = role
        }
    }
    
    /**
     * 清除当前登录用户信息（退出登录）
     */
    suspend fun clearCurrentUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_CURRENT_USER_ID)
            preferences.remove(KEY_CURRENT_USER_NAME)
            preferences.remove(KEY_CURRENT_MEETING_ID)
            preferences.remove(KEY_CURRENT_USER_ROLE)
        }
    }
}
