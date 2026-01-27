package com.xunyidi.sealmeet.domain.usecase

import android.content.Context
import com.xunyidi.sealmeet.data.audit.AuditLogger
import com.xunyidi.sealmeet.data.local.database.AppDatabase
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.util.StoragePathManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 清空数据 UseCase
 * 
 * 负责清空所有本地数据：
 * 1. 清空 Room 数据库
 * 2. 删除会议文件目录
 * 3. 清理临时文件
 * 4. 记录审计日志
 * 
 * 可被以下场景调用：
 * - 设置页面手动清空
 * - 检测到 .clear_all 标记文件
 */
@Singleton
class ClearDataUseCase @Inject constructor(
    private val database: AppDatabase,
    private val appPreferences: AppPreferences,
    private val auditLogger: AuditLogger,
    @ApplicationContext private val context: Context
) {
    
    /**
     * 清空所有数据
     * 
     * @param writeAckFile 是否写入确认文件（响应 .clear_all 时需要）
     * @return 清空结果
     */
    suspend fun clearAllData(writeAckFile: Boolean = false): ClearResult = withContext(Dispatchers.IO) {
        try {
            Timber.i("========== 开始清空所有数据 ==========")
            
            val isDeveloperMode = appPreferences.developerModeEnabled.first()
            Timber.i("当前模式: ${if (isDeveloperMode) "开发者模式" else "生产模式"}")
            
            // 1. 清空数据库
            database.clearAllTables()
            Timber.i("✅ 数据库已清空")
            
            // 2. 删除会议文件目录（使用正确的路径）
            val meetingsDir = StoragePathManager.getMeetingsRoot(context, isDeveloperMode)
            if (meetingsDir.exists()) {
                val deleted = meetingsDir.deleteRecursively()
                if (deleted) {
                    Timber.i("✅ 会议文件已删除: ${meetingsDir.absolutePath}")
                } else {
                    Timber.w("⚠️ 会议文件删除失败: ${meetingsDir.absolutePath}")
                }
            } else {
                Timber.i("会议文件目录不存在，跳过: ${meetingsDir.absolutePath}")
            }
            
            // 3. 清理临时文件
            val cacheFiles = context.cacheDir.listFiles()
            var tempFileCount = 0
            cacheFiles?.filter { it.name.startsWith("unpack_") }?.forEach { file ->
                if (file.deleteRecursively()) {
                    tempFileCount++
                }
            }
            Timber.i("✅ 临时文件已清理: $tempFileCount 个")
            
            // 4. 写入确认文件（如果需要）
            if (writeAckFile) {
                writeClearAllAck(isDeveloperMode)
            }
            
            // 5. 记录审计日志
            auditLogger.logDataCleared()
            
            Timber.i("========== 清空数据完成 ==========")
            
            ClearResult.Success
            
        } catch (e: Exception) {
            Timber.e(e, "清空数据失败")
            ClearResult.Failure(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 检查是否存在 .clear_all 标记文件
     */
    suspend fun checkClearAllFlag(): Boolean = withContext(Dispatchers.IO) {
        val isDeveloperMode = appPreferences.developerModeEnabled.first()
        val syncDir = StoragePathManager.getSyncDirectory(isDeveloperMode)
        val clearAllFile = File(syncDir, CLEAR_ALL_FLAG)
        
        val exists = clearAllFile.exists()
        if (exists) {
            Timber.i("🔴 检测到清空标记文件: ${clearAllFile.absolutePath}")
        }
        exists
    }
    
    /**
     * 处理 .clear_all 标记
     * 
     * 1. 检测到标记后清空所有数据
     * 2. 删除标记文件
     * 3. 写入确认文件
     * 
     * @return true 如果处理了清空操作
     */
    suspend fun handleClearAllFlag(): Boolean = withContext(Dispatchers.IO) {
        val isDeveloperMode = appPreferences.developerModeEnabled.first()
        val syncDir = StoragePathManager.getSyncDirectory(isDeveloperMode)
        val clearAllFile = File(syncDir, CLEAR_ALL_FLAG)
        
        if (!clearAllFile.exists()) {
            return@withContext false
        }
        
        Timber.i("🔴 检测到 .clear_all 标记，开始清空数据...")
        
        // 执行清空
        val result = clearAllData(writeAckFile = true)
        
        // 删除标记文件
        if (clearAllFile.delete()) {
            Timber.i("✅ 已删除 .clear_all 标记文件")
        } else {
            Timber.w("⚠️ 删除 .clear_all 标记文件失败")
        }
        
        result is ClearResult.Success
    }
    
    /**
     * 写入清空确认文件
     */
    private fun writeClearAllAck(isDeveloperMode: Boolean) {
        try {
            val uploadDir = StoragePathManager.getUploadDirectory(isDeveloperMode)
            if (!uploadDir.exists()) {
                uploadDir.mkdirs()
            }
            
            val ackFile = File(uploadDir, CLEAR_ALL_ACK)
            val timestamp = java.time.OffsetDateTime.now().format(
                java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )
            ackFile.writeText("""{"cleared_at":"$timestamp"}""")
            
            Timber.i("✅ 已写入清空确认文件: ${ackFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "写入清空确认文件失败")
        }
    }
    
    companion object {
        /** 清空标记文件名 */
        private const val CLEAR_ALL_FLAG = ".clear_all"
        
        /** 清空确认文件名 */
        private const val CLEAR_ALL_ACK = "clear_all.ack"
    }
}

/**
 * 清空结果
 */
sealed class ClearResult {
    data object Success : ClearResult()
    data class Failure(val error: String) : ClearResult()
}
