package com.xunyidi.sealmeet.data.sync

import timber.log.Timber
import java.security.MessageDigest

/**
 * 校验和验证器
 * 
 * 用于验证文件完整性
 */
class ChecksumVerifier {
    
    /**
     * 计算数据的SHA-256校验和
     */
    fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 验证数据的校验和
     * 
     * @param data 要验证的数据
     * @param expectedChecksum 期望的校验和（SHA-256十六进制字符串）
     * @return 是否匹配
     */
    fun verify(data: ByteArray, expectedChecksum: String): Boolean {
        val actualChecksum = calculateSha256(data)
        val isValid = actualChecksum.equals(expectedChecksum, ignoreCase = true)
        
        if (!isValid) {
            Timber.w("校验和不匹配! 期望: $expectedChecksum, 实际: $actualChecksum")
        }
        
        return isValid
    }
    
    /**
     * 验证多个文件的校验和
     * 
     * @param fileChecksums 文件ID到校验和的映射
     * @param fileDataMap 文件ID到文件数据的映射
     * @return 验证结果，包含失败的文件ID列表
     */
    fun verifyMultiple(
        fileChecksums: Map<String, String>,
        fileDataMap: Map<String, ByteArray>
    ): VerificationResult {
        val failedFiles = mutableListOf<String>()
        
        fileChecksums.forEach { (fileId, expectedChecksum) ->
            val fileData = fileDataMap[fileId]
            
            if (fileData == null) {
                Timber.w("文件数据缺失: $fileId")
                failedFiles.add(fileId)
            } else if (!verify(fileData, expectedChecksum)) {
                Timber.w("文件校验失败: $fileId")
                failedFiles.add(fileId)
            }
        }
        
        return VerificationResult(
            totalFiles = fileChecksums.size,
            passedFiles = fileChecksums.size - failedFiles.size,
            failedFiles = failedFiles
        )
    }
}

/**
 * 验证结果
 */
data class VerificationResult(
    val totalFiles: Int,
    val passedFiles: Int,
    val failedFiles: List<String>
) {
    val isAllPassed: Boolean
        get() = failedFiles.isEmpty()
    
    val hasFailures: Boolean
        get() = failedFiles.isNotEmpty()
}
