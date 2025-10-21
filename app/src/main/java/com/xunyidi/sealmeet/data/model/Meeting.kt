package com.xunyidi.sealmeet.data.model

/**
 * 会议数据模型
 */
data class Meeting(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String = "",
    val description: String = "",
    val participants: List<String> = emptyList(),
    val materialCount: Int = 0
)
