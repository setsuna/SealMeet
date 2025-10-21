package com.xunyidi.sealmeet.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 会议议程表
 * 对应后台 meeting_agendas 表
 */
@Entity(
    tableName = "meeting_agendas",
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
        Index(value = ["meeting_id", "order_num"]),
        Index(value = ["meeting_id", "status"])
    ]
)
data class MeetingAgendaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "meeting_id")
    val meetingId: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "duration")
    val duration: Int? = null, // 时长（分钟）
    
    @ColumnInfo(name = "presenter")
    val presenter: String? = null, // 主讲人/报告人
    
    @ColumnInfo(name = "order_num")
    val orderNum: Int = 1, // 排序号
    
    @ColumnInfo(name = "status")
    val status: String = "pending", // pending, in_progress, completed
    
    @ColumnInfo(name = "started_at")
    val startedAt: Long? = null,
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
