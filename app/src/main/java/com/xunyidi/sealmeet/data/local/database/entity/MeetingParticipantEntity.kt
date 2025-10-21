package com.xunyidi.sealmeet.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 会议参会人员表
 * 对应后台 meeting_participants 表
 * 注意：增加了 password 字段用于本地验证
 */
@Entity(
    tableName = "meeting_participants",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meeting_id"]),
        Index(value = ["user_id"]),
        Index(value = ["meeting_id", "role"]),
        Index(value = ["meeting_id", "status"])
    ]
)
data class MeetingParticipantEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "meeting_id")
    val meetingId: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "user_name")
    val userName: String,
    
    @ColumnInfo(name = "email")
    val email: String? = null,
    
    @ColumnInfo(name = "department")
    val department: String? = null,
    
    @ColumnInfo(name = "role")
    val role: String, // host(主持人), participant(参会人), observer(列席人员)
    
    @ColumnInfo(name = "status")
    val status: String = "invited", // invited, accepted, declined, attended
    
    @ColumnInfo(name = "joined_at")
    val joinedAt: Long? = null,
    
    @ColumnInfo(name = "left_at")
    val leftAt: Long? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    // 本地新增字段：用于验证参会人员身份
    @ColumnInfo(name = "password")
    val password: String? = null // 加密存储的密码，用于标准会议(type=standard)的验证
)
