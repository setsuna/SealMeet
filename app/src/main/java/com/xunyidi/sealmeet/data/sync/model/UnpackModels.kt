package com.xunyidi.sealmeet.data.sync.model

/**
 * 解包清单
 * 对应包内的 manifest.json
 */
data class PackageManifest(
    val meetingId: String,
    val meetingName: String,
    val packageTime: String,
    val fileCount: Int,
    val totalFileSize: Long,
    val files: List<PackageFileInfo>,
    val agendas: Map<String, List<String>> // agenda_id -> file_ids
)

/**
 * 包文件信息
 */
data class PackageFileInfo(
    val id: String,
    val originalName: String,
    val fileSize: Long,
    val mimeType: String,
    val checksum: String,
    val agendaId: String? = null
)

/**
 * 校验和文件
 * 对应包内的 checksum.json
 */
data class PackageChecksum(
    val packageChecksum: String,
    val fileChecksums: Map<String, String>, // file_id -> checksum
    val createdAt: String
)

/**
 * 解包结果
 */
sealed class UnpackResult {
    /**
     * 解包成功
     */
    data class Success(
        val meetingId: String,
        val fileCount: Int
    ) : UnpackResult()
    
    /**
     * 解包失败
     */
    data class Failure(
        val meetingId: String?,
        val error: UnpackError
    ) : UnpackResult()
}

/**
 * 解包错误类型
 */
sealed class UnpackError {
    data class DecryptionFailed(val message: String) : UnpackError()
    data class UnzipFailed(val message: String) : UnpackError()
    data class ManifestInvalid(val message: String) : UnpackError()
    data class ChecksumMismatch(val fileIds: List<String>) : UnpackError()
    data class DatabaseError(val message: String) : UnpackError()
    data class IOError(val message: String) : UnpackError()
    data class Unknown(val message: String) : UnpackError()
}
