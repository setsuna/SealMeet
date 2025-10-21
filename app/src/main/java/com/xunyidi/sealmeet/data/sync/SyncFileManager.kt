package com.xunyidi.sealmeet.data.sync

import android.os.Environment
import timber.log.Timber
import java.io.File

/**
 * 同步文件管理器
 * 
 * 负责管理同步目录、检测待同步文件、全局锁机制等
 */
class SyncFileManager {
    
    companion object {
        private const val SYNC_LOCK_FILE = ".sync_lock" // 全局同步锁文件
        private const val PACKAGE_FILE_EXTENSION = ".zip.enc"
        private const val MANIFEST_FILE_NAME = "manifest.json"
    }
    
    /**
     * 获取同步目录（Download目录）
     */
    fun getSyncDirectory(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }
    
    /**
     * 检查同步目录是否可访问
     */
    fun isSyncDirectoryAccessible(): Boolean {
        val syncDir = getSyncDirectory()
        return syncDir.exists() && syncDir.isDirectory && syncDir.canRead()
    }
    
    /**
     * 检查是否正在同步
     * 
     * @return true表示正在同步中，不应进行解包操作
     */
    fun isSyncing(): Boolean {
        val syncDir = getSyncDirectory()
        val lockFile = File(syncDir, SYNC_LOCK_FILE)
        return lockFile.exists()
    }
    
    /**
     * 扫描同步目录，查找所有待解包的会议包文件
     * 
     * @return 待解包的文件列表（如果正在同步则返回空列表）
     */
    fun scanPackageFiles(): List<PackageFile> {
        val syncDir = getSyncDirectory()
        
        if (!isSyncDirectoryAccessible()) {
            Timber.w("同步目录不可访问: ${syncDir.absolutePath}")
            return emptyList()
        }
        
        // 检查全局同步锁
        if (isSyncing()) {
            Timber.i("检测到同步锁文件，文件同步中，跳过解包")
            return emptyList()
        }
        
        val packageFiles = mutableListOf<PackageFile>()
        
        syncDir.listFiles()?.forEach { file ->
            // 只处理 .zip.enc 文件
            if (file.isFile && file.name.endsWith(PACKAGE_FILE_EXTENSION)) {
                // 从文件名提取meetingID
                // 格式: meeting_{meetingID}.zip.enc
                val meetingId = extractMeetingId(file.name)
                
                if (meetingId != null) {
                    packageFiles.add(
                        PackageFile(
                            file = file,
                            meetingId = meetingId
                        )
                    )
                    Timber.d("发现待解包文件: ${file.name}, meetingId=$meetingId")
                } else {
                    Timber.w("无法从文件名提取meetingID: ${file.name}")
                }
            }
        }
        
        Timber.i("扫描完成，发现 ${packageFiles.size} 个待解包文件")
        return packageFiles
    }
    
    /**
     * 从文件名提取meetingID
     * 文件名格式: meeting_{meetingID}.zip.enc
     */
    private fun extractMeetingId(fileName: String): String? {
        val pattern = """meeting_(.+)\.zip\.enc""".toRegex()
        val matchResult = pattern.find(fileName)
        return matchResult?.groupValues?.get(1)
    }
    
    /**
     * 读取manifest.json文件
     */
    fun readManifest(): ManifestData? {
        val syncDir = getSyncDirectory()
        val manifestFile = File(syncDir, MANIFEST_FILE_NAME)
        
        if (!manifestFile.exists()) {
            Timber.d("manifest.json 不存在")
            return null
        }
        
        return try {
            val json = manifestFile.readText()
            // 这里可以使用 Gson 或 kotlinx.serialization 解析
            // 暂时返回null，由具体实现决定
            Timber.d("manifest.json 读取成功")
            null // TODO: 解析JSON
        } catch (e: Exception) {
            Timber.e(e, "读取manifest.json失败")
            null
        }
    }
    
    /**
     * 删除包文件
     * 
     * @param packageFile 要删除的包文件
     * @return 是否删除成功
     */
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
    
    /**
     * 清理损坏的包文件
     */
    fun cleanupCorruptedPackage(packageFile: PackageFile, reason: String) {
        Timber.w("清理损坏的包文件: ${packageFile.file.name}, 原因: $reason")
        deletePackageFile(packageFile)
    }
}

/**
 * 包文件信息
 */
data class PackageFile(
    val file: File,
    val meetingId: String
) {
    val fileName: String
        get() = file.name
    
    val size: Long
        get() = file.length()
}

/**
 * Manifest数据结构
 * 对应后台的 manifest.json
 */
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
