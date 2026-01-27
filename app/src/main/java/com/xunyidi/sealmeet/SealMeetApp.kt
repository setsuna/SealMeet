package com.xunyidi.sealmeet

import android.app.Application
import com.xunyidi.sealmeet.data.audit.AuditLogger
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.data.sync.DirectoryMonitorManager
import com.xunyidi.sealmeet.domain.usecase.ClearDataUseCase
import com.xunyidi.sealmeet.domain.usecase.UnpackMeetingUseCase
import com.xunyidi.sealmeet.util.NotificationHelper
import com.xunyidi.sealmeet.util.StoragePathManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * SealMeet应用程序类
 */
@HiltAndroidApp
class SealMeetApp : Application() {

    @Inject
    lateinit var directoryMonitorManager: DirectoryMonitorManager
    
    @Inject
    lateinit var unpackMeetingUseCase: UnpackMeetingUseCase
    
    @Inject
    lateinit var clearDataUseCase: ClearDataUseCase
    
    @Inject
    lateinit var auditLogger: AuditLogger
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var appPreferences: AppPreferences
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // 初始化Timber日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("SealMeet Application started")
        
        // 初始化存储目录
        initializeStorageDirectories()
        
        // 记录应用启动日志（用于验证审计功能）
        auditLogger.log(
            action = "app_start",
            extra = mapOf("version" to BuildConfig.VERSION_NAME)
        )
        
        // 启动目录监控
        startDirectoryMonitoring()
    }
    
    /**
     * 初始化存储目录
     * 
     * 在应用启动时创建所有必要的目录
     */
    private fun initializeStorageDirectories() {
        try {
            val isDeveloperMode = runBlocking {
                appPreferences.developerModeEnabled.first()
            }
            
            Timber.i("当前模式: ${if (isDeveloperMode) "开发者模式" else "生产模式"}")
            
            StoragePathManager.initializeDirectories(this, isDeveloperMode)
        } catch (e: Exception) {
            Timber.e(e, "初始化存储目录失败")
        }
    }
    
    /**
     * 启动目录监控
     * 
     * 监控同步目录，当检测到新的会议包文件时自动解包
     */
    private fun startDirectoryMonitoring() {
        applicationScope.launch {
            try {
                Timber.i("========== 启动目录监控 ==========")
                
                directoryMonitorManager.startMonitoring {
                    // 当检测到文件变化时，触发解包
                    triggerUnpack()
                }
                
                Timber.i("========== 目录监控启动完成 ==========")
            } catch (e: Exception) {
                Timber.e(e, "启动目录监控失败")
            }
        }
    }
    
    /**
     * 触发解包任务
     */
    private suspend fun triggerUnpack() {
        try {
            Timber.i("========== 触发自动解包 ==========")
            
            // 1. 先检查是否有 .clear_all 标记
            if (clearDataUseCase.handleClearAllFlag()) {
                Timber.i("🔴 已处理 .clear_all 标记，数据已清空")
                notificationHelper.showClearDataNotification()
                // 清空后仍然继续解包流程（可能有新的会议包）
            }
            
            // 2. 执行解包
            val results = unpackMeetingUseCase.unpackAllPendingPackages()
            
            var successCount = 0
            val successMeetings = mutableListOf<String>()
            
            results.forEach { result ->
                when (result) {
                    is com.xunyidi.sealmeet.data.sync.model.UnpackResult.Success -> {
                        successCount++
                        successMeetings.add(result.meetingId)
                        Timber.i("✅ 解包成功: ${result.meetingId}, 文件数: ${result.fileCount}")
                    }
                    is com.xunyidi.sealmeet.data.sync.model.UnpackResult.Failure -> {
                        Timber.e("❌ 解包失败: ${result.meetingId}, 原因: ${result.error}")
                    }
                }
            }
            
            if (results.isNotEmpty()) {
                Timber.i("========== 自动解包完成，共处理 ${results.size} 个会议包 ==========")
                
                // 发送解包成功通知
                if (successCount > 0) {
                    notificationHelper.showUnpackSuccessNotification(successCount, successMeetings)
                }
            } else {
                Timber.i("========== 无待解包文件 ==========")
            }
        } catch (e: Exception) {
            Timber.e(e, "自动解包过程异常")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // 清理监控器资源
        directoryMonitorManager.cleanup()
        
        Timber.d("SealMeet Application terminated")
    }
}
