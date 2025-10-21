package com.xunyidi.sealmeet.data.sync

import timber.log.Timber
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM 解密器
 * 
 * 用于解密后台打包的加密文件
 * 加密算法：AES-256-GCM
 * 密钥派生：SHA256(masterKey + meetingID + salt)
 */
class AesGcmDecryptor(
    private val masterKey: String
) {
    
    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BIT = 128
        private const val SALT = "meeting-api-file-salt-2024"
        
        /**
         * 默认的包加密密钥（与后台保持一致）
         */
        const val DEFAULT_PACKAGE_KEY = "package-encryption-key-32-chars!!"
    }
    
    /**
     * 派生文件专用密钥
     * 算法与后台保持一致：SHA256(masterKey + fileID + salt)
     */
    private fun deriveKey(fileId: String): ByteArray {
        val data = "${masterKey}${fileId}${SALT}"
        return MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
    }
    
    /**
     * 解密数据
     * 
     * @param encryptedData 加密数据（包含nonce + ciphertext）
     * @param fileId 文件ID（用于密钥派生）
     * @return 解密后的原始数据
     * @throws DecryptionException 解密失败
     */
    fun decrypt(encryptedData: ByteArray, fileId: String): ByteArray {
        try {
            // 派生密钥
            val key = deriveKey(fileId)
            val secretKey = SecretKeySpec(key, "AES")
            
            // 初始化密码器
            val cipher = Cipher.getInstance(ALGORITHM)
            
            // GCM模式的nonce大小
            val nonceSize = 12 // GCM标准nonce大小
            
            // 检查数据长度
            if (encryptedData.size < nonceSize) {
                throw DecryptionException("加密数据长度不足，无法提取nonce")
            }
            
            // 提取nonce和密文
            val nonce = encryptedData.copyOfRange(0, nonceSize)
            val ciphertext = encryptedData.copyOfRange(nonceSize, encryptedData.size)
            
            // 使用GCM参数规范
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, nonce)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            // 解密
            return cipher.doFinal(ciphertext)
            
        } catch (e: Exception) {
            Timber.e(e, "解密失败: fileId=$fileId")
            throw DecryptionException("解密失败: ${e.message}", e)
        }
    }
    
    /**
     * 计算SHA-256校验和
     */
    fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
}

/**
 * 解密异常
 */
class DecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
