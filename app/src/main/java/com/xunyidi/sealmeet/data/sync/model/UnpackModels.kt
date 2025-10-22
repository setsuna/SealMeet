package com.xunyidi.sealmeet.data.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 解包清单（元数据）
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
    
    @Json(name = "package_version")
    val packageVersion: String = "1.0",
    
    @Json(name = "file_count")
    val fileCount: Int,
    
    @Json(name = "total_file_size")
    val totalFileSize: Long,
    
    @Json(name = "has_meeting_data")
    val hasMeetingData: Boolean = false,
    
    @Json(name = "has_participants_data")
    val hasParticipantsData: Boolean = false,
    
    @Json(name = "has_agendas_data")
    val hasAgendasData: Boolean = false,
    
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
 * 会议完整信息
 * 对应包内的 meeting.json
 */
@JsonClass(generateAdapter = true)
data class MeetingData(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "name")
    val name: String,
    
    @Json(name = "start_time")
    val startTime: String,
    
    @Json(name = "end_time")
    val endTime: String,
    
    @Json(name = "location")
    val location: String? = null,
    
    @Json(name = "description")
    val description: String? = null,
    
    @Json(name = "category")
    val category: String? = null,
    
    @Json(name = "type")
    val type: String = "tablet",
    
    @Json(name = "status")
    val status: String = "preparation",
    
    @Json(name = "security_level")
    val securityLevel: String = "internal",
    
    @Json(name = "is_draft")
    val isDraft: Boolean = false,
    
    @Json(name = "max_participants")
    val maxParticipants: Int? = null,
    
    @Json(name = "created_by")
    val createdBy: String,
    
    @Json(name = "created_by_name")
    val createdByName: String,
    
    @Json(name = "created_at")
    val createdAt: String,
    
    @Json(name = "updated_at")
    val updatedAt: String
)

/**
 * 参会人员信息
 * 对应 participants.json 中的单个参会人员
 */
@JsonClass(generateAdapter = true)
data class ParticipantData(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "user_id")
    val userId: String,
    
    @Json(name = "user_name")
    val userName: String,
    
    @Json(name = "email")
    val email: String? = null,
    
    @Json(name = "department")
    val department: String? = null,
    
    @Json(name = "role")
    val role: String,
    
    @Json(name = "status")
    val status: String = "invited",
    
    @Json(name = "password")
    val password: String? = null,
    
    @Json(name = "joined_at")
    val joinedAt: String? = null,
    
    @Json(name = "left_at")
    val leftAt: String? = null,
    
    @Json(name = "created_at")
    val createdAt: String,
    
    @Json(name = "updated_at")
    val updatedAt: String
)

/**
 * 参会人员列表包装
 * 对应包内的 participants.json
 */
@JsonClass(generateAdapter = true)
data class ParticipantsWrapper(
    @Json(name = "participants")
    val participants: List<ParticipantData>
)

/**
 * 议程详细信息
 * 对应 agendas.json 中的单个议程
 */
@JsonClass(generateAdapter = true)
data class AgendaData(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "meeting_id")
    val meetingId: String,
    
    @Json(name = "title")
    val title: String,
    
    @Json(name = "description")
    val description: String? = null,
    
    @Json(name = "duration")
    val duration: Int? = null,
    
    @Json(name = "presenter")
    val presenter: String? = null,
    
    @Json(name = "order_num")
    val orderNum: Int = 1,
    
    @Json(name = "status")
    val status: String = "pending",
    
    @Json(name = "started_at")
    val startedAt: String? = null,
    
    @Json(name = "completed_at")
    val completedAt: String? = null,
    
    @Json(name = "created_at")
    val createdAt: String,
    
    @Json(name = "updated_at")
    val updatedAt: String
)

/**
 * 议程列表包装
 * 对应包内的 agendas.json
 */
@JsonClass(generateAdapter = true)
data class AgendasWrapper(
    @Json(name = "agendas")
    val agendas: List<AgendaData>
)

/**
 * 客户端配置
 * 对应 Download目录下的 config.json
 */
@JsonClass(generateAdapter = true)
data class ClientConfig(
    @Json(name = "version")
    val version: String,
    
    @Json(name = "updated_at")
    val updatedAt: String,
    
    @Json(name = "allow_override_local")
    val allowOverrideLocal: Boolean = true,
    
    @Json(name = "sync")
    val sync: SyncConfig,
    
    @Json(name = "storage")
    val storage: StorageConfig
)

@JsonClass(generateAdapter = true)
data class SyncConfig(
    @Json(name = "incremental_update_enabled")
    val incrementalUpdateEnabled: Boolean = true,
    
    @Json(name = "auto_sync_enabled")
    val autoSyncEnabled: Boolean = false,
    
    @Json(name = "sync_interval_minutes")
    val syncIntervalMinutes: Int = 30
)

@JsonClass(generateAdapter = true)
data class StorageConfig(
    @Json(name = "keep_temp_files_enabled")
    val keepTempFilesEnabled: Boolean = false,
    
    @Json(name = "auto_cleanup_days")
    val autoCleanupDays: Int = 30
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
