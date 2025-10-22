package com.xunyidi.sealmeet.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 会议表
 * 对应后台 meetings 表
 */
@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long, // 时间戳（毫秒）
    
    @ColumnInfo(name = "end_time")
    val endTime: Long,
    
    @ColumnInfo(name = "location")
    val location: String? = null,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "category")
    val category: String? = null,
    
    @ColumnInfo(name = "type")
    val type: String = "standard", // standard(标准会议) 或 tablet(快速会议)
    
    @ColumnInfo(name = "status")
    val status: String = "preparation", // preparation, distributable, in_progress, closed
    
    @ColumnInfo(name = "security_level")
    val securityLevel: String = "internal", // internal, confidential, secret
    
    @ColumnInfo(name = "is_draft")
    val isDraft: Boolean = false,
    
    @ColumnInfo(name = "max_participants")
    val maxParticipants: Int? = null,
    
    // 🆕 新增字段 - 与后端同步
    @ColumnInfo(name = "password")
    val password: String? = null,
    
    @ColumnInfo(name = "expiry_type")
    val expiryType: String = "none", // none, today, custom
    
    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long? = null, // 时间戳（毫秒）
    
    @ColumnInfo(name = "sign_in_type")
    val signInType: String = "none", // none, manual, password
    
    @ColumnInfo(name = "organizer")
    val organizer: String? = null,
    
    @ColumnInfo(name = "organizer_name")
    val organizerName: String? = null,
    
    @ColumnInfo(name = "host")
    val host: String? = null,
    
    @ColumnInfo(name = "host_name")
    val hostName: String? = null,
    
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    
    @ColumnInfo(name = "created_by_name")
    val createdByName: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long, // 时间戳（毫秒）
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    // 本地字段（非后台同步）
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long = System.currentTimeMillis(), // 同步时间
    
    @ColumnInfo(name = "package_checksum")
    val packageChecksum: String? = null // 包校验和，用于检测更新
)
