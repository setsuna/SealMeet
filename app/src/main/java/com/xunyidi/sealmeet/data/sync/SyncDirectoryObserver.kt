package com.xunyidi.sealmeet.data.sync

import android.os.FileObserver
import timber.log.Timber
import java.io.File

/**
 * 同步目录监控器
 * 
 * 使用 FileObserver 监控同步目录的文件变化
 * 当检测到 .zip.enc 文件创建/移动/修改时，触发回调
 */
class SyncDirectoryObserver(
    private val directory: File,
    private val onFileChanged: (File) -> Unit
) : FileObserver(directory, EVENTS) {

    companion object {
        // 监控的事件类型
        private const val EVENTS = CREATE or MOVED_TO or CLOSE_WRITE
        
        // 会议包文件扩展名
        private const val PACKAGE_EXTENSION = ".zip.enc"
        
        // 同步锁文件
        private const val SYNC_LOCK_FILE = ".sync_lock"
    }

    init {
        Timber.i("📂 初始化目录监控器")
        Timber.i("   监控目录: ${directory.absolutePath}")
        Timber.i("   监控事件: CREATE | MOVED_TO | CLOSE_WRITE")
    }

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return

        val eventName = getEventName(event)
        Timber.d("📝 目录事件: $eventName -> $path")

        // 忽略锁文件
        if (path == SYNC_LOCK_FILE) {
            Timber.d("   ⏭️  忽略同步锁文件")
            return
        }

        // 只处理会议包文件
        if (!path.endsWith(PACKAGE_EXTENSION)) {
            Timber.d("   ⏭️  忽略非会议包文件")
            return
        }

        // 获取完整文件路径
        val file = File(directory, path)
        
        if (!file.exists()) {
            Timber.w("   ⚠️  文件不存在，可能是删除事件")
            return
        }

        if (!file.isFile) {
            Timber.w("   ⚠️  不是文件")
            return
        }

        Timber.i("✅ 检测到会议包文件: ${file.name}")
        Timber.i("   大小: ${file.length()} bytes")

        // 触发回调
        onFileChanged(file)
    }

    /**
     * 启动监控
     */
    fun start() {
        startWatching()
        Timber.i("🚀 目录监控已启动")
    }

    /**
     * 停止监控
     */
    fun stop() {
        stopWatching()
        Timber.i("🛑 目录监控已停止")
    }

    /**
     * 获取事件名称（用于日志）
     */
    private fun getEventName(event: Int): String = when (event) {
        CREATE -> "CREATE"
        MOVED_TO -> "MOVED_TO"
        CLOSE_WRITE -> "CLOSE_WRITE"
        else -> "UNKNOWN($event)"
    }
}
