package com.xunyidi.sealmeet.data.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 解包清单
 * 对应包内的 manifest.json
 */
@JsonClass(generateAdapter = true)
data class PackageManifest(
    @Json(name = "meeting_id")
    val meetingId: String,
    
    @Json(name = "meeting_name")
    val meetingName: String,
    
    @Json(name = "package_time")
    val packageTime: String,
    
    @Json(name = "file_count")
    val fileCount: Int,
    
    @Json(name = "total_file_size")
    val totalFileSize: Long,
    
    @Json(name = "files")
    val files: List<PackageFileInfo>,
    
    @Json(name = "agendas")
    val agendas: Map<String, List<String>> // agenda_id -> file_ids
)

/**
 * 包文件信息
 */
@JsonClass(generateAdapter = true)
data class PackageFileInfo(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "original_name")
    val originalName: String,
    
    @Json(name = "file_size")
    val fileSize: Long,
    
    @Json(name = "mime_type")
    val mimeType: String,
    
    @Json(name = "checksum")
    val checksum: String,
    
    @Json(name = "agenda_id")
    val agendaId: String? = null
)

/**
 * 校验和文件
 * 对应包内的 checksum.json
 */
@JsonClass(generateAdapter = true)
data class PackageChecksum(
    @Json(name = "package_checksum")
    val packageChecksum: String,
    
    @Json(name = "file_checksums")
    val fileChecksums: Map<String, String>, // file_id -> checksum
    
    @Json(name = "created_at")
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
