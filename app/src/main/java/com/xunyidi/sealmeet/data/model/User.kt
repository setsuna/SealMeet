package com.xunyidi.sealmeet.data.model

/**
 * 用户数据模型
 */
data class User(
    val id: String,
    val name: String,
    val role: String = "",
    val department: String = ""
)
