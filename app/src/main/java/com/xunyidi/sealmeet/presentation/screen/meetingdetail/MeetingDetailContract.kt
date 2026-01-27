package com.xunyidi.sealmeet.presentation.screen.meetingdetail

import com.xunyidi.sealmeet.core.mvi.UiEffect
import com.xunyidi.sealmeet.core.mvi.UiIntent
import com.xunyidi.sealmeet.core.mvi.UiState
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity

/**
 * 会议详情页面 - Contract
 */
object MeetingDetailContract {
    
    data class State(
        val isLoading: Boolean = true,
        val meeting: MeetingEntity? = null,
        val agendas: List<AgendaWithFiles> = emptyList(),
        val participants: List<MeetingParticipantEntity> = emptyList(),
        val errorMessage: String? = null,
        /** 当前登录用户名（快速会议时为null） */
        val currentUserName: String? = null,
        
        // ===== 签到状态 =====
        /** 是否已签到 */
        val isSignedIn: Boolean = false,
        /** 显示密码签到弹窗 */
        val showPasswordSignInDialog: Boolean = false,
        /** 显示手写签到弹窗 */
        val showManualSignInDialog: Boolean = false,
        /** 签到密码输入 */
        val signInPassword: String = "",
        /** 签到密码错误信息 */
        val signInPasswordError: String? = null,
        
        // ===== 弹窗状态 =====
        /** 显示会议介绍弹窗 */
        val showInfoDialog: Boolean = false,
        /** 显示参会人员弹窗 */
        val showParticipantsDialog: Boolean = false,
        /** 显示退出确认弹窗 */
        val showExitConfirmDialog: Boolean = false
    ) : UiState {
        
        /**
         * 签到类型
         */
        val signInType: String
            get() = meeting?.signInType ?: "none"
        
        /**
         * 是否需要签到（非免签模式）
         */
        val needSignIn: Boolean
            get() = signInType != "none"
        
        /**
         * 功能按钮是否可用（免签或已签到）
         */
        val isFunctionEnabled: Boolean
            get() = !needSignIn || isSignedIn
    }
    
    /**
     * 议程及其关联文件
     */
    data class AgendaWithFiles(
        val agenda: MeetingAgendaEntity,
        val files: List<MeetingFileEntity>
    )
    
    sealed interface Intent : UiIntent {
        data class LoadMeetingDetail(val meetingId: String) : Intent
        
        // ===== 签到相关 =====
        /** 点击签到按钮 */
        data object ClickSignIn : Intent
        /** 更新签到密码输入 */
        data class UpdateSignInPassword(val password: String) : Intent
        /** 提交密码签到 */
        data object SubmitPasswordSignIn : Intent
        /** 关闭密码签到弹窗 */
        data object DismissPasswordSignInDialog : Intent
        /** 提交手写签到 */
        data class SubmitManualSignIn(val signatureBitmap: android.graphics.Bitmap) : Intent
        /** 关闭手写签到弹窗 */
        data object DismissManualSignInDialog : Intent
        
        // ===== 功能按钮 =====
        /** 会议介绍 */
        data object ClickMeetingInfo : Intent
        /** 会议议题 */
        data object ClickAgendas : Intent
        /** 会议投票 */
        data object ClickVoting : Intent
        /** 会议记录 */
        data object ClickRecords : Intent
        
        // ===== 弹窗控制 =====
        data object DismissInfoDialog : Intent
        data object ShowParticipantsDialog : Intent
        data object DismissParticipantsDialog : Intent
        
        // ===== 退出相关 =====
        /** 返回会议列表 */
        data object NavigateBack : Intent
        /** 退出会议（显示确认弹窗） */
        data object ShowExitConfirmDialog : Intent
        data object DismissExitConfirmDialog : Intent
        /** 确认退出会议 */
        data object ConfirmExit : Intent
        
        // ===== 文件操作 =====
        data class OpenFile(val fileId: String) : Intent
    }
    
    sealed interface Effect : UiEffect {
        data class ShowError(val message: String) : Effect
        data class ShowToast(val message: String) : Effect
        data class OpenFileViewer(val fileId: String, val filePath: String, val fileName: String) : Effect
        /** 返回会议列表 */
        data object NavigateBack : Effect
        /** 跳转到议程页面 */
        data class NavigateToAgendas(val meetingId: String) : Effect
        /** 退出到登录页 */
        data object NavigateToLogin : Effect
    }
}
