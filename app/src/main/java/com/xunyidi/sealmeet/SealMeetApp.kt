package com.xunyidi.sealmeet

import android.app.Application
import com.xunyidi.sealmeet.data.sync.DirectoryMonitorManager
import com.xunyidi.sealmeet.domain.usecase.UnpackMeetingUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // 初始化Timber日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("SealMeet Application started")
        
        // 启动目录监控
        startDirectoryMonitoring()
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
            
            val results = unpackMeetingUseCase.unpackAllPendingPackages()
            
            results.forEach { result ->
                when (result) {
                    is com.xunyidi.sealmeet.data.sync.model.UnpackResult.Success -> {
                        Timber.i("✅ 解包成功: ${result.meetingId}, 文件数: ${result.fileCount}")
                    }
                    is com.xunyidi.sealmeet.data.sync.model.UnpackResult.Failure -> {
                        Timber.e("❌ 解包失败: ${result.meetingId}, 原因: ${result.error}")
                    }
                }
            }
            
            if (results.isNotEmpty()) {
                Timber.i("========== 自动解包完成，共处理 ${results.size} 个会议包 ==========")
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
