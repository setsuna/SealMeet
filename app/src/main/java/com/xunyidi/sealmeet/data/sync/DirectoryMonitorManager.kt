package com.xunyidi.sealmeet.data.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 目录监控管理器
 * 
 * 负责管理 SyncDirectoryObserver 的生命周期
 * 处理文件变化事件的防抖和解包任务触发
 */
@Singleton
class DirectoryMonitorManager @Inject constructor(
    private val syncFileManager: SyncFileManager
) {
    private var observer: SyncDirectoryObserver? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 防抖处理：记录待处理的文件
    private val pendingFiles = mutableMapOf<String, Job>()
    
    // 监控状态
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring
    
    // 解包回调
    private var onUnpackTriggered: (suspend () -> Unit)? = null

    companion object {
        // 防抖延迟（毫秒）：等待文件写入完成
        private const val DEBOUNCE_DELAY = 2000L
        
        // 文件稳定性检查间隔
        private const val STABILITY_CHECK_INTERVAL = 500L
    }

    /**
     * 启动目录监控
     * 
     * @param onUnpackTriggered 当需要解包时触发的回调
     */
    fun startMonitoring(onUnpackTriggered: suspend () -> Unit) {
        if (_isMonitoring.value) {
            Timber.w("目录监控已在运行中")
            return
        }

        val syncDir = syncFileManager.getSyncDirectory()
        
        if (!syncDir.exists()) {
            Timber.e("同步目录不存在: ${syncDir.absolutePath}")
            return
        }

        if (!syncDir.isDirectory) {
            Timber.e("同步目录不是一个目录: ${syncDir.absolutePath}")
            return
        }

        this.onUnpackTriggered = onUnpackTriggered

        // 创建并启动监控器
        observer = SyncDirectoryObserver(
            directory = syncDir,
            onFileChanged = ::onFileDetected
        )
        
        observer?.start()
        _isMonitoring.value = true
        
        Timber.i("✅ 目录监控已启动")
        Timber.i("   同步目录: ${syncDir.absolutePath}")
    }

    /**
     * 停止目录监控
     */
    fun stopMonitoring() {
        if (!_isMonitoring.value) {
            Timber.w("目录监控未运行")
            return
        }

        observer?.stop()
        observer = null
        
        // 取消所有待处理的任务
        pendingFiles.values.forEach { it.cancel() }
        pendingFiles.clear()
        
        _isMonitoring.value = false
        
        Timber.i("🛑 目录监控已停止")
    }

    /**
     * 检查监控状态
     */
    fun isRunning(): Boolean = _isMonitoring.value

    /**
     * 文件检测回调
     */
    private fun onFileDetected(file: File) {
        val fileName = file.name
        
        Timber.i("🔍 检测到文件变化: $fileName")

        // 取消之前的待处理任务（防抖）
        pendingFiles[fileName]?.cancel()

        // 创建新的延迟任务
        val job = scope.launch {
            try {
                Timber.d("⏱️  等待文件稳定: $fileName")
                delay(DEBOUNCE_DELAY)

                // 检查文件是否稳定（大小不再变化）
                if (!isFileStable(file)) {
                    Timber.w("⚠️  文件未稳定，跳过: $fileName")
                    return@launch
                }

                // 检查是否正在同步
                if (syncFileManager.isSyncing()) {
                    Timber.i("🔒 检测到同步锁，跳过解包")
                    return@launch
                }

                Timber.i("✅ 文件稳定，触发解包: $fileName")
                
                // 触发解包
                onUnpackTriggered?.invoke()
                
            } catch (e: CancellationException) {
                Timber.d("⏹️  解包任务被取消: $fileName")
            } catch (e: Exception) {
                Timber.e(e, "❌ 处理文件时出错: $fileName")
            } finally {
                pendingFiles.remove(fileName)
            }
        }

        pendingFiles[fileName] = job
    }

    /**
     * 检查文件是否稳定（大小不再变化）
     * 
     * 用于确保文件已经完全写入，避免解包不完整的文件
     */
    private suspend fun isFileStable(file: File): Boolean {
        if (!file.exists()) return false

        val size1 = file.length()
        delay(STABILITY_CHECK_INTERVAL)
        
        if (!file.exists()) return false
        
        val size2 = file.length()
        
        return size1 == size2 && size1 > 0
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopMonitoring()
        scope.cancel()
        Timber.i("🧹 监控管理器资源已清理")
    }
}
