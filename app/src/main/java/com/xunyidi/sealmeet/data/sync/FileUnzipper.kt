package com.xunyidi.sealmeet.data.sync

import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * ZIP 文件解压工具
 */
class FileUnzipper {
    
    /**
     * 解压ZIP数据到指定目录
     * 
     * @param zipData ZIP文件的字节数据
     * @param destDir 解压目标目录
     * @return 解压后的文件列表
     * @throws UnzipException 解压失败
     */
    fun unzip(zipData: ByteArray, destDir: File): List<File> {
        try {
            // 确保目标目录存在
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            
            val extractedFiles = mutableListOf<File>()
            
            ZipInputStream(zipData.inputStream()).use { zipIn ->
                var entry = zipIn.nextEntry
                
                while (entry != null) {
                    val entryName = entry.name
                    
                    // 安全检查：防止路径穿越攻击
                    if (entryName.contains("..")) {
                        Timber.w("跳过可疑的ZIP条目: $entryName")
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }
                    
                    val destFile = File(destDir, entryName)
                    
                    if (entry.isDirectory) {
                        // 创建目录
                        destFile.mkdirs()
                    } else {
                        // 确保父目录存在
                        destFile.parentFile?.mkdirs()
                        
                        // 解压文件
                        FileOutputStream(destFile).use { output ->
                            zipIn.copyTo(output)
                        }
                        
                        extractedFiles.add(destFile)
                        Timber.d("解压文件: ${destFile.absolutePath}")
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            Timber.i("解压完成，共解压 ${extractedFiles.size} 个文件")
            return extractedFiles
            
        } catch (e: Exception) {
            Timber.e(e, "解压ZIP失败")
            throw UnzipException("解压失败: ${e.message}", e)
        }
    }
    
    /**
     * 从ZIP数据中读取指定文件的内容
     * 
     * @param zipData ZIP文件的字节数据
     * @param fileName 要读取的文件名
     * @return 文件内容，如果文件不存在返回null
     */
    fun readFileFromZip(zipData: ByteArray, fileName: String): ByteArray? {
        try {
            ZipInputStream(zipData.inputStream()).use { zipIn ->
                var entry = zipIn.nextEntry
                
                while (entry != null) {
                    if (entry.name == fileName && !entry.isDirectory) {
                        return zipIn.readBytes()
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            return null
            
        } catch (e: Exception) {
            Timber.e(e, "从ZIP中读取文件失败: $fileName")
            return null
        }
    }
}

/**
 * 解压异常
 */
class UnzipException(message: String, cause: Throwable? = null) : Exception(message, cause)
