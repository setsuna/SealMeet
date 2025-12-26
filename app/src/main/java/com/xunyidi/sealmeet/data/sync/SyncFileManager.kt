package com.xunyidi.sealmeet.data.sync

import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.util.StoragePathManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步文件管理器
 * 
 * 负责管理同步目录、检测待同步文件、全局锁机制等
 */
@Singleton
class SyncFileManager @Inject constructor(
    private val appPreferences: AppPreferences
) {
    
    companion object {
        private const val SYNC_LOCK_FILE = ".sync_lock"
        private const val PACKAGE_FILE_EXTENSION = ".zip.enc"
        private const val MANIFEST_FILE_NAME = "manifest.json"
    }
    
    /**
     * 获取同步目录
     * 开发者模式： Download 目录
     * 生产者模式： /data/userdata/com.xunyidi.sealmeet/sync
     */
    fun getSyncDirectory(): File {
        val isDeveloperMode = runBlocking {
            appPreferences.developerModeEnabled.first()
        }
        
        return StoragePathManager.getSyncDirectory(isDeveloperMode)
    }
    
    /**
     * 检查同步目录是否可访问
     */
    fun isSyncDirectoryAccessible(): Boolean {
        val syncDir = getSyncDirectory()
        val exists = syncDir.exists()
        val isDir = syncDir.isDirectory
        val canRead = syncDir.canRead()
        
        Timber.d("同步目录: ${syncDir.absolutePath}")
        Timber.d("目录存在: $exists, 是目录: $isDir, 可读: $canRead")
        
        return exists && isDir && canRead
    }
    
    /**
     * 检查是否正在同步
     */
    fun isSyncing(): Boolean {
        val syncDir = getSyncDirectory()
        val lockFile = File(syncDir, SYNC_LOCK_FILE)
        val exists = lockFile.exists()
        
        if (exists) {
            Timber.i("检测到同步锁文件: ${lockFile.absolutePath}")
        }
        
        return exists
    }
    
    /**
     * 扫描同步目录，查找所有待解包的会议包文件
     */
    fun scanPackageFiles(): List<PackageFile> {
        val syncDir = getSyncDirectory()
        
        Timber.i("========== 开始扫描同步目录 ==========")
        Timber.i("目录路径: ${syncDir.absolutePath}")
        
        if (!isSyncDirectoryAccessible()) {
            Timber.w("同步目录不可访问")
            return emptyList()
        }
        
        if (isSyncing()) {
            Timber.i("检测到同步锁文件，跳过解包")
            return emptyList()
        }
        
        val files = syncDir.listFiles()
        
        if (files == null) {
            Timber.e("listFiles() 返回null，可能没有权限")
            return emptyList()
        }
        
        Timber.i("目录中共有 ${files.size} 个文件/文件夹:")
        files.forEachIndexed { index, file ->
            val type = if (file.isDirectory) "[目录]" else "[文件]"
            val size = if (file.isFile) "(${file.length()} bytes)" else ""
            Timber.i("  ${index + 1}. $type ${file.name} $size")
        }
        
        Timber.i("========== 开始筛选.zip.enc文件 ==========")
        
        val packageFiles = mutableListOf<PackageFile>()
        
        files.forEach { file ->
            if (file.isFile && file.name.endsWith(PACKAGE_FILE_EXTENSION)) {
                Timber.i("✓ 找到包文件: ${file.name}")
                
                val meetingId = extractMeetingId(file.name)
                
                if (meetingId != null) {
                    packageFiles.add(
                        PackageFile(
                            file = file,
                            meetingId = meetingId
                        )
                    )
                    Timber.i("  → meetingId: $meetingId")
                } else {
                    Timber.w("  → 无法提取meetingID")
                }
            }
        }
        
        Timber.i("========== 扫描完成 ==========")
        Timber.i("发现 ${packageFiles.size} 个待解包文件")
        
        return packageFiles
    }
    
    private fun extractMeetingId(fileName: String): String? {
        val pattern = """meeting_(.+)\.zip\.enc""".toRegex()
        val matchResult = pattern.find(fileName)
        return matchResult?.groupValues?.get(1)
    }
    
    fun readManifest(): ManifestData? {
        val syncDir = getSyncDirectory()
        val manifestFile = File(syncDir, MANIFEST_FILE_NAME)
        
        if (!manifestFile.exists()) {
            Timber.d("manifest.json 不存在")
            return null
        }
        
        return try {
            val json = manifestFile.readText()
            Timber.d("manifest.json 读取成功")
            null
        } catch (e: Exception) {
            Timber.e(e, "读取manifest.json失败")
            null
        }
    }
    
    fun deletePackageFile(packageFile: PackageFile): Boolean {
        return try {
            val deleted = packageFile.file.delete()
            if (deleted) {
                Timber.i("删除包文件成功: ${packageFile.file.name}")
            } else {
                Timber.w("删除包文件失败: ${packageFile.file.name}")
            }
            deleted
        } catch (e: Exception) {
            Timber.e(e, "删除包文件异常: ${packageFile.file.name}")
            false
        }
    }
    
    fun cleanupCorruptedPackage(packageFile: PackageFile, reason: String) {
        Timber.w("清理损坏的包文件: ${packageFile.file.name}, 原因: $reason")
        deletePackageFile(packageFile)
    }
}

data class PackageFile(
    val file: File,
    val meetingId: String
) {
    val fileName: String
        get() = file.name
    
    val size: Long
        get() = file.length()
}

data class ManifestData(
    val version: String,
    val updatedAt: String,
    val packages: List<ManifestPackage>
)

data class ManifestPackage(
    val meetingId: String,
    val packagePath: String,
    val packageSize: Long,
    val checksum: String,
    val fileCount: Int,
    val totalFileSize: Long,
    val updatedAt: String
)
