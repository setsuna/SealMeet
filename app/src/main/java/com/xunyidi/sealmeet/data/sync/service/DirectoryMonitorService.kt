package com.xunyidi.sealmeet.data.sync.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xunyidi.sealmeet.R
import com.xunyidi.sealmeet.data.sync.DirectoryMonitorManager
import com.xunyidi.sealmeet.domain.usecase.UnpackMeetingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * 目录监控前台服务
 * 
 * 保证 App 在后台或锁屏时也能继续监控同步目录
 * 使用 Foreground Service 提高进程优先级，避免被系统杀死
 */
@AndroidEntryPoint
class DirectoryMonitorService : Service() {

    @Inject
    lateinit var directoryMonitorManager: DirectoryMonitorManager
    
    @Inject
    lateinit var unpackMeetingUseCase: UnpackMeetingUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sync_monitor_channel"
        private const val CHANNEL_NAME = "会议同步监控"
        
        private const val UNPACK_NOTIFICATION_ID = 1002
        private const val UNPACK_CHANNEL_ID = "unpack_result_channel"
        private const val UNPACK_CHANNEL_NAME = "解包结果通知"

        /**
         * 启动监控服务
         */
        fun start(context: Context) {
            val intent = Intent(context, DirectoryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 停止监控服务
         */
        fun stop(context: Context) {
            val intent = Intent(context, DirectoryMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("🚀 DirectoryMonitorService onCreate")
        
        // 创建通知渠道
        createNotificationChannel()
        createUnpackNotificationChannel()
        
        // 启动前台服务
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 启动目录监控
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("📢 DirectoryMonitorService onStartCommand")
        return START_STICKY // 服务被杀后自动重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("🛑 DirectoryMonitorService onDestroy")
        
        // 停止监控
        directoryMonitorManager.stopMonitoring()
        
        // 取消所有协程
        serviceScope.cancel()
    }

    /**
     * 启动目录监控
     */
    private fun startMonitoring() {
        serviceScope.launch {
            try {
                Timber.i("========== 服务启动目录监控 ==========")
                
                directoryMonitorManager.startMonitoring {
                    // 当检测到文件变化时，触发解包
                    triggerUnpack()
                }
                
                // 更新通知为监控中状态
                updateNotification("监控中", "正在监控同步目录...")
                
                Timber.i("========== 服务目录监控启动完成 ==========")
            } catch (e: Exception) {
                Timber.e(e, "启动目录监控失败")
                updateNotification("监控失败", "无法启动目录监控")
            }
        }
    }

    /**
     * 触发解包任务
     */
    private suspend fun triggerUnpack() {
        try {
            Timber.i("========== 服务触发自动解包 ==========")
            
            // 更新通知
            updateNotification("解包中", "正在解包会议文件...")
            
            val results = unpackMeetingUseCase.unpackAllPendingPackages()
            
            var successCount = 0
            var failureCount = 0
            val successMeetings = mutableListOf<String>()
            
            results.forEach { result ->
                when (result) {
                    is com.xunyidi.sealmeet.data.sync.model.UnpackResult.Success -> {
                        successCount++
                        successMeetings.add(result.meetingId)
                        Timber.i("✅ 解包成功: ${result.meetingId}, 文件数: ${result.fileCount}")
                    }
                    is com.xunyidi.sealmeet.data.sync.model.UnpackResult.Failure -> {
                        failureCount++
                        Timber.e("❌ 解包失败: ${result.meetingId}, 原因: ${result.error}")
                    }
                }
            }
            
            if (results.isNotEmpty()) {
                Timber.i("========== 服务自动解包完成，成功: $successCount, 失败: $failureCount ==========")
                
                // 发送解包结果通知
                if (successCount > 0) {
                    showUnpackSuccessNotification(successCount, successMeetings)
                }
                
                updateNotification(
                    "解包完成", 
                    "成功: $successCount, 失败: $failureCount"
                )
                
                // 3秒后恢复监控状态显示
                delay(3000)
                updateNotification("监控中", "正在监控同步目录...")
            } else {
                Timber.i("========== 无待解包文件 ==========")
                updateNotification("监控中", "正在监控同步目录...")
            }
        } catch (e: Exception) {
            Timber.e(e, "服务自动解包过程异常")
            updateNotification("解包失败", "发生错误: ${e.message}")
            
            // 3秒后恢复监控状态显示
            delay(3000)
            updateNotification("监控中", "正在监控同步目录...")
        }
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // 低重要性，不发出声音
            ).apply {
                description = "监控会议同步目录，自动解包会议文件"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建解包结果通知渠道
     */
    private fun createUnpackNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UNPACK_CHANNEL_ID,
                UNPACK_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // 默认重要性，会发出声音
            ).apply {
                description = "会议文件解包成功后的通知"
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建通知
     */
    private fun createNotification(
        title: String = "会议同步监控",
        content: String = "初始化中..."
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 不可滑动删除
            .build()
    }

    /**
     * 更新通知内容
     */
    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 显示解包成功通知
     */
    private fun showUnpackSuccessNotification(count: Int, meetingIds: List<String>) {
        val title = "会议文件解包成功"
        val content = if (count == 1) {
            "会议 ${meetingIds.first()} 已解包完成"
        } else {
            "成功解包 $count 个会议文件"
        }
        
        val notification = NotificationCompat.Builder(this, UNPACK_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // 点击后自动消失
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(UNPACK_NOTIFICATION_ID, notification)
    }
}
