package com.xunyidi.sealmeet.data.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 目录监控管理器（简化版防抖）
 * 
 * 适用于 API 服务保证文件写入完整性的场景
 * 只保留最小防抖延迟，避免多次事件触发
 */
@Singleton
class DirectoryMonitorManagerSimple @Inject constructor(
    private val syncFileManager: SyncFileManager
) {
    private var observer: SyncDirectoryObserver? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 防抖处理：记录待处理的文件
    private val pendingFiles = mutableMapOf<String, Job>()
    
    // 监控状态
    private val _isMonitoring = MutableStateFlow(false)
    @Suppress("unused")
    val isMonitoring: StateFlow<Boolean> = _isMonitoring
    
    // 解包回调
    private var onUnpackTriggered: (suspend () -> Unit)? = null

    companion object {
        // 最小防抖延迟（毫秒）：避免多次事件触发
        private const val DEBOUNCE_DELAY = 300L
    }

    /**
     * 启动目录监控
     */
    @Suppress("unused")
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

        observer = SyncDirectoryObserver(
            directory = syncDir,
            onFileChanged = ::onFileDetected
        )
        
        observer?.start()
        _isMonitoring.value = true
        
        Timber.i("✅ 目录监控已启动（简化防抖版）")
        Timber.i("   同步目录: ${syncDir.absolutePath}")
    }

    /**
     * 停止目录监控
     */
    private fun stopMonitoring() {
        if (!_isMonitoring.value) {
            Timber.w("目录监控未运行")
            return
        }

        observer?.stop()
        observer = null
        
        pendingFiles.values.forEach { it.cancel() }
        pendingFiles.clear()
        
        _isMonitoring.value = false
        
        Timber.i("🛑 目录监控已停止")
    }

    /**
     * 检查监控状态
     */
    @Suppress("unused")
    fun isRunning(): Boolean = _isMonitoring.value

    /**
     * 文件检测回调（简化版）
     */
    private fun onFileDetected(file: File) {
        val fileName = file.name
        
        Timber.i("🔍 检测到文件变化: $fileName")

        // 取消之前的待处理任务
        pendingFiles[fileName]?.cancel()

        // 创建新的延迟任务（简化版：只有最小延迟）
        val job = scope.launch {
            try {
                // 最小延迟，避免多次事件触发
                delay(DEBOUNCE_DELAY)

                // 检查文件是否存在
                if (!file.exists()) {
                    Timber.w("⚠️  文件不存在: $fileName")
                    return@launch
                }

                // 检查是否正在同步
                if (syncFileManager.isSyncing()) {
                    Timber.i("🔒 检测到同步锁，跳过解包")
                    return@launch
                }

                Timber.i("✅ 触发解包: $fileName")
                
                // 直接触发解包（假设 API 服务保证文件完整性）
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
     * 清理资源
     */
    @Suppress("unused")
    fun cleanup() {
        stopMonitoring()
        scope.cancel()
        Timber.i("🧹 监控管理器资源已清理")
    }
}
