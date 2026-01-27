package com.xunyidi.sealmeet.data.audit

/**
 * 审计日志事件类型常量
 * 
 * 所有可记录的用户操作和系统事件
 */
object AuditAction {
    
    // ===== 会议相关 =====
    /** 打开会议 */
    const val MEETING_OPEN = "meeting_open"
    /** 关闭会议 */
    const val MEETING_CLOSE = "meeting_close"
    
    // ===== 文件相关 =====
    /** 打开文件 */
    const val FILE_OPEN = "file_open"
    /** 关闭文件 */
    const val FILE_CLOSE = "file_close"
    
    // ===== 系统相关 =====
    /** 解包成功 */
    const val UNPACK_SUCCESS = "unpack_success"
    /** 解包失败 */
    const val UNPACK_FAILED = "unpack_failed"
    /** 数据清空 */
    const val DATA_CLEARED = "data_cleared"
    
    // ===== 用户相关 =====
    /** 用户登录 */
    const val USER_LOGIN = "user_login"
    /** 用户登出 */
    const val USER_LOGOUT = "user_logout"
    
    // ===== 应用相关 =====
    /** 应用启动 */
    const val APP_START = "app_start"
    /** 应用退出 */
    const val APP_EXIT = "app_exit"
    
    // ===== 预留扩展 =====
    /** 提交投票 */
    const val VOTE_SUBMIT = "vote_submit"
    /** 打开笔记 */
    const val NOTE_OPEN = "note_open"
    /** 保存笔记 */
    const val NOTE_SAVE = "note_save"
    /** 编辑文件 */
    const val FILE_EDIT = "file_edit"
    /** 文件批注 */
    const val FILE_ANNOTATE = "file_annotate"
}
