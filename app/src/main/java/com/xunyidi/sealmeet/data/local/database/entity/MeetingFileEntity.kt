package com.xunyidi.sealmeet.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 会议文件表
 * 用于存储解包后的文件信息
 */
@Entity(
    tableName = "meeting_files",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MeetingAgendaEntity::class,
            parentColumns = ["id"],
            childColumns = ["agenda_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["meeting_id"]),
        Index(value = ["agenda_id"]),
        Index(value = ["meeting_id", "agenda_id"])
    ]
)
data class MeetingFileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "meeting_id")
    val meetingId: String,
    
    @ColumnInfo(name = "agenda_id")
    val agendaId: String? = null, // 关联议程ID，为null表示不属于任何议程
    
    @ColumnInfo(name = "original_name")
    val originalName: String, // 原始文件名
    
    @ColumnInfo(name = "local_path")
    val localPath: String, // 本地存储路径
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long, // 文件大小（字节）
    
    @ColumnInfo(name = "mime_type")
    val mimeType: String, // MIME类型
    
    @ColumnInfo(name = "checksum")
    val checksum: String, // SHA256校验和
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
