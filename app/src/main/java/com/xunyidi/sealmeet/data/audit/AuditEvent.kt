package com.xunyidi.sealmeet.data.audit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 审计日志事件数据类
 * 
 * 字段采用缩写以减少日志文件体积：
 * - ts: timestamp (时间戳)
 * - act: action (事件类型)
 * - mid: meeting_id (会议ID)
 * - uid: user_id (用户ID)
 * - uname: user_name (用户名)
 * - fid: file_id (文件ID)
 * - fname: file_name (文件名)
 * - dur: duration (持续时间，秒)
 * - cnt: count (数量)
 * - err: error (错误信息)
 * - ext: extra (扩展数据)
 */
@JsonClass(generateAdapter = true)
data class AuditEvent(
    /** 时间戳 (ISO8601格式，秒级精度) */
    @Json(name = "ts")
    val timestamp: String,
    
    /** 事件类型 */
    @Json(name = "act")
    val action: String,
    
    /** 会议ID */
    @Json(name = "mid")
    val meetingId: String? = null,
    
    /** 用户ID */
    @Json(name = "uid")
    val userId: String? = null,
    
    /** 用户名 */
    @Json(name = "uname")
    val userName: String? = null,
    
    /** 文件ID */
    @Json(name = "fid")
    val fileId: String? = null,
    
    /** 文件名 */
    @Json(name = "fname")
    val fileName: String? = null,
    
    /** 持续时间（秒） */
    @Json(name = "dur")
    val duration: Long? = null,
    
    /** 数量 */
    @Json(name = "cnt")
    val count: Int? = null,
    
    /** 错误信息 */
    @Json(name = "err")
    val error: String? = null,
    
    /** 扩展数据（用于未来功能扩展） */
    @Json(name = "ext")
    val extra: Map<String, String>? = null
)
