package com.xunyidi.sealmeet.data.audit

import com.squareup.moshi.Moshi
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.util.StoragePathManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 审计日志记录器
 * 
 * 负责将用户操作和系统事件记录到 upload/audit_{date}.log 文件
 * 日志格式：JSON Lines（每行一个JSON对象）
 * 
 * 使用示例：
 * ```kotlin
 * auditLogger.logMeetingOpen("meeting_123", "user_001", "张三")
 * auditLogger.logFileOpen("meeting_123", "file_001", "方案.pdf", "user_001", "张三")
 * ```
 */
@Singleton
class AuditLogger @Inject constructor(
    private val appPreferences: AppPreferences
) {
    
    private val moshi = Moshi.Builder().build()
    private val eventAdapter = moshi.adapter(AuditEvent::class.java)
    
    // 写入锁，保证线程安全
    private val writeMutex = Mutex()
    
    // 时间格式化器
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    // ========== 会议相关 ==========
    
    /**
     * 记录打开会议
     */
    fun logMeetingOpen(meetingId: String, userId: String, userName: String) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.MEETING_OPEN,
                meetingId = meetingId,
                userId = userId,
                userName = userName
            )
        )
    }
    
    /**
     * 记录关闭会议
     */
    fun logMeetingClose(meetingId: String, durationSec: Long, userId: String, userName: String) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.MEETING_CLOSE,
                meetingId = meetingId,
                duration = durationSec,
                userId = userId,
                userName = userName
            )
        )
    }
    
    // ========== 文件相关 ==========
    
    /**
     * 记录打开文件
     */
    fun logFileOpen(
        meetingId: String,
        fileId: String,
        fileName: String,
        userId: String,
        userName: String
    ) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.FILE_OPEN,
                meetingId = meetingId,
                fileId = fileId,
                fileName = fileName,
                userId = userId,
                userName = userName
            )
        )
    }
    
    /**
     * 记录关闭文件
     */
    fun logFileClose(
        meetingId: String,
        fileId: String,
        durationSec: Long,
        userId: String,
        userName: String
    ) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.FILE_CLOSE,
                meetingId = meetingId,
                fileId = fileId,
                duration = durationSec,
                userId = userId,
                userName = userName
            )
        )
    }
    
    // ========== 系统相关 ==========
    
    /**
     * 记录解包成功
     */
    fun logUnpackSuccess(meetingId: String, fileCount: Int) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.UNPACK_SUCCESS,
                meetingId = meetingId,
                count = fileCount
            )
        )
    }
    
    /**
     * 记录解包失败
     */
    fun logUnpackFailed(meetingId: String?, error: String) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.UNPACK_FAILED,
                meetingId = meetingId,
                error = error
            )
        )
    }
    
    /**
     * 记录数据清空
     */
    fun logDataCleared() {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.DATA_CLEARED
            )
        )
    }
    
    // ========== 用户相关 ==========
    
    /**
     * 记录用户登录
     */
    fun logUserLogin(userId: String, userName: String) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.USER_LOGIN,
                userId = userId,
                userName = userName
            )
        )
    }
    
    /**
     * 记录用户登出
     */
    fun logUserLogout(userId: String, userName: String) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = AuditAction.USER_LOGOUT,
                userId = userId,
                userName = userName
            )
        )
    }
    
    // ========== 通用方法（用于扩展）==========
    
    /**
     * 通用日志记录方法
     * 用于记录自定义事件或未来扩展的事件类型
     * 
     * @param action 事件类型
     * @param meetingId 会议ID（可选）
     * @param userId 用户ID（可选）
     * @param userName 用户名（可选）
     * @param extra 扩展数据（可选）
     */
    fun log(
        action: String,
        meetingId: String? = null,
        userId: String? = null,
        userName: String? = null,
        fileId: String? = null,
        fileName: String? = null,
        duration: Long? = null,
        count: Int? = null,
        error: String? = null,
        extra: Map<String, String>? = null
    ) {
        writeEvent(
            AuditEvent(
                timestamp = now(),
                action = action,
                meetingId = meetingId,
                userId = userId,
                userName = userName,
                fileId = fileId,
                fileName = fileName,
                duration = duration,
                count = count,
                error = error,
                extra = extra
            )
        )
    }
    
    // ========== 内部方法 ==========
    
    /**
     * 获取当前时间戳（ISO8601格式，秒级精度）
     */
    private fun now(): String {
        return OffsetDateTime.now(ZoneId.systemDefault()).format(timestampFormatter)
    }
    
    /**
     * 获取今天的日期字符串
     */
    private fun today(): String {
        return LocalDate.now().format(dateFormatter)
    }
    
    /**
     * 获取日志文件
     */
    private fun getLogFile(): File {
        val isDeveloperMode = runBlocking {
            appPreferences.developerModeEnabled.first()
        }
        
        val uploadDir = StoragePathManager.getUploadDirectory(isDeveloperMode)
        
        // 确保目录存在
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }
        
        return File(uploadDir, "audit_${today()}.log")
    }
    
    /**
     * 写入事件到日志文件
     */
    private fun writeEvent(event: AuditEvent) {
        try {
            // 序列化为JSON
            val json = eventAdapter.toJson(event)
            
            // 异步写入（使用 runBlocking 确保写入完成，但不阻塞调用线程太久）
            runBlocking {
                writeMutex.withLock {
                    val logFile = getLogFile()
                    logFile.appendText(json + "\n")
                }
            }
            
            Timber.d("审计日志: ${event.action} -> ${logFile().name}")
            
        } catch (e: Exception) {
            Timber.e(e, "写入审计日志失败: ${event.action}")
        }
    }
    
    /**
     * 获取当前日志文件（用于调试）
     */
    private fun logFile(): File = getLogFile()
    
    /**
     * 获取所有日志文件列表（用于调试）
     */
    @Suppress("unused")
    fun getLogFiles(): List<File> {
        val isDeveloperMode = runBlocking {
            appPreferences.developerModeEnabled.first()
        }
        
        val uploadDir = StoragePathManager.getUploadDirectory(isDeveloperMode)
        
        return uploadDir.listFiles { file ->
            file.isFile && file.name.startsWith("audit_") && file.name.endsWith(".log")
        }?.toList() ?: emptyList()
    }
}
