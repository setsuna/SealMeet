package com.xunyidi.sealmeet.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.xunyidi.sealmeet.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID = "unpack_result_channel"
        private const val CHANNEL_NAME = "解包结果通知"
        private const val NOTIFICATION_ID_UNPACK = 2001
        private const val NOTIFICATION_ID_CLEAR = 2002
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "会议文件解包成功后的通知"
            setShowBadge(true)
        }
        
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    fun showUnpackSuccessNotification(count: Int, meetingIds: List<String>) {
        val title = "会议文件解包成功"
        val content = if (count == 1) {
            "会议 ${meetingIds.first()} 已解包完成"
        } else {
            "成功解包 $count 个会议文件"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_UNPACK, notification)
    }
    
    /**
     * 显示数据清空通知
     */
    fun showClearDataNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("数据已清空")
            .setContentText("服务器请求清空所有会议数据，已执行完成")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_CLEAR, notification)
    }
}
