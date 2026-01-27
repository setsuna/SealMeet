package com.xunyidi.sealmeet.util

import android.content.Context
import android.os.Environment
import timber.log.Timber
import java.io.File

/**
 * 存储路径统一管理工具类
 * 
 * 根据开发者模式自动切换存储路径：
 * - 开发者模式：使用标准 Android 路径（便于在通用平板上测试）
 * - 生产模式：使用定制平板的 /data/userdata/ 路径
 */
object StoragePathManager {
    
    // 应用专属外部存储根目录
    private const val APP_EXTERNAL_ROOT = "/data/userdata/com.xunyidi.sealmeet"
    
    /**
     * 获取同步目录（用于监控加密文件）
     * 
     * @param isDeveloperMode 是否为开发者模式
     * @return 同步目录
     */
    fun getSyncDirectory(isDeveloperMode: Boolean): File {
        return if (isDeveloperMode) {
            // 开发者模式：使用 Download 目录
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            // 生产模式：使用应用专属同步目录
            File("$APP_EXTERNAL_ROOT/sync")
        }
    }
    
    /**
     * 获取会议文件存储根目录
     * 
     * @param context Context
     * @param isDeveloperMode 是否为开发者模式
     * @return 会议文件根目录
     */
    fun getMeetingsRoot(context: Context, isDeveloperMode: Boolean): File {
        return if (isDeveloperMode) {
            // 开发者模式：使用内部存储
            File(context.filesDir, "meetings")
        } else {
            // 生产模式：使用应用专属外部存储
            File("$APP_EXTERNAL_ROOT/meetings")
        }
    }
    
    /**
     * 获取指定会议的存储目录
     * 
     * @param context Context
     * @param meetingId 会议ID
     * @param isDeveloperMode 是否为开发者模式
     * @return 会议存储目录
     */
    fun getMeetingDirectory(context: Context, meetingId: String, isDeveloperMode: Boolean): File {
        return File(getMeetingsRoot(context, isDeveloperMode), meetingId)
    }
    
    /**
     * 获取上行同步目录（审计日志、设备状态等）
     * 
     * 平板写入，服务器读取
     * 
     * @param isDeveloperMode 是否为开发者模式
     * @return 上行同步目录
     */
    fun getUploadDirectory(isDeveloperMode: Boolean): File {
        return if (isDeveloperMode) {
            // 开发者模式：使用 Documents/SealMeetUpload 目录
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).resolve("SealMeetUpload")
        } else {
            // 生产模式：使用应用专属上行目录
            File("$APP_EXTERNAL_ROOT/upload")
        }
    }
    
    /**
     * 获取签名文件存储目录
     * 
     * 存储手写签到的签名图片
     * 
     * @param isDeveloperMode 是否为开发者模式
     * @return 签名文件目录
     */
    fun getSignaturesDirectory(isDeveloperMode: Boolean): File {
        return File(getUploadDirectory(isDeveloperMode), "signatures")
    }
    
    /**
     * 获取日志目录（未来可能需要）
     * 
     * @param isDeveloperMode 是否为开发者模式
     * @return 日志目录
     */
    fun getLogsDirectory(isDeveloperMode: Boolean): File {
        return if (isDeveloperMode) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).resolve("SealMeetLogs")
        } else {
            File("$APP_EXTERNAL_ROOT/logs")
        }
    }
    
    /**
     * 获取缓存目录（未来可能需要）
     * 
     * @param isDeveloperMode 是否为开发者模式
     * @return 缓存目录
     */
    fun getCacheDirectory(isDeveloperMode: Boolean): File {
        return if (isDeveloperMode) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).resolve("SealMeetCache")
        } else {
            File("$APP_EXTERNAL_ROOT/cache")
        }
    }
    
    /**
     * 初始化所有必要目录
     * 建议在 Application.onCreate() 中调用
     * 
     * @param context Context
     * @param isDeveloperMode 是否为开发者模式
     */
    fun initializeDirectories(context: Context, isDeveloperMode: Boolean) {
        val directories = listOf(
            getSyncDirectory(isDeveloperMode),
            getUploadDirectory(isDeveloperMode),
            getMeetingsRoot(context, isDeveloperMode),
            getLogsDirectory(isDeveloperMode),
            getCacheDirectory(isDeveloperMode)
        )
        
        directories.forEach { dir ->
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (created) {
                    Timber.i("创建目录成功: ${dir.absolutePath}")
                } else {
                    Timber.w("创建目录失败: ${dir.absolutePath}")
                }
            } else {
                Timber.d("目录已存在: ${dir.absolutePath}")
            }
        }
    }
}
