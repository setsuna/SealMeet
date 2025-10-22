package com.xunyidi.sealmeet.domain.usecase

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.data.sync.SyncFileManager
import com.xunyidi.sealmeet.data.sync.model.ClientConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置应用 UseCase
 * 
 * 负责：
 * 1. 检查Download目录下的config.json
 * 2. 根据"允许服务器配置覆盖"设置决定是否应用
 * 3. 应用配置到本地DataStore
 * 4. 删除config.json
 */
@Singleton
class ConfigApplyUseCase @Inject constructor(
    private val syncFileManager: SyncFileManager,
    private val appPreferences: AppPreferences
) {
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * 检查并应用服务器配置
     * 
     * @return 是否应用了新配置
     */
    suspend fun checkAndApplyConfig(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 检查config.json是否存在
            val syncDir = syncFileManager.getSyncDirectory()
            val configFile = File(syncDir, "config.json")
            
            if (!configFile.exists()) {
                Timber.d("config.json 不存在，跳过配置应用")
                return@withContext false
            }
            
            Timber.i("发现config.json文件")
            
            // 2. 读取本地配置：是否允许服务器覆盖
            val allowLocalOverride = appPreferences.allowServerConfigOverride.first()
            
            // 3. 解析config.json
            val configJson = configFile.readText()
            val adapter = moshi.adapter(ClientConfig::class.java)
            val config = adapter.fromJson(configJson)
            
            if (config == null) {
                Timber.e("config.json 解析失败")
                configFile.delete()
                return@withContext false
            }
            
            Timber.i("config.json 解析成功，版本: ${config.version}")
            
            // 4. 判断是否应该应用配置
            // 只有当两个条件都满足时才应用：
            // 1. 服务器配置允许覆盖 (config.allowOverrideLocal == true)
            // 2. 本地设置允许覆盖 (allowLocalOverride == true)
            val shouldApply = config.allowOverrideLocal && allowLocalOverride
            
            if (!shouldApply) {
                if (!config.allowOverrideLocal) {
                    Timber.i("服务器配置不允许覆盖本地设置，删除config.json")
                } else {
                    Timber.i("本地设置不允许服务器配置覆盖，保留config.json")
                    // 不删除文件，留给下次处理
                    return@withContext false
                }
                configFile.delete()
                return@withContext false
            }
            
            // 5. 应用配置
            appPreferences.setIncrementalUpdateEnabled(config.sync.incrementalUpdateEnabled)
            appPreferences.setKeepTempFilesEnabled(config.storage.keepTempFilesEnabled)
            
            Timber.i("""
                配置已应用:
                - 增量更新: ${config.sync.incrementalUpdateEnabled}
                - 保留临时文件: ${config.storage.keepTempFilesEnabled}
            """.trimIndent())
            
            // 6. 删除config.json
            configFile.delete()
            Timber.i("config.json 已删除")
            
            return@withContext true
            
        } catch (e: Exception) {
            Timber.e(e, "应用服务器配置失败")
            return@withContext false
        }
    }
}
