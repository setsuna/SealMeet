package com.xunyidi.sealmeet.presentation.screen.meetingdetail

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.audit.AuditLogger
import com.xunyidi.sealmeet.data.local.database.dao.AgendaDao
import com.xunyidi.sealmeet.data.local.database.dao.FileDao
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import com.xunyidi.sealmeet.data.local.database.dao.ParticipantDao
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.util.StoragePathManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * 会议详情ViewModel
 */
@HiltViewModel
class MeetingDetailViewModel @Inject constructor(
    private val meetingDao: MeetingDao,
    private val agendaDao: AgendaDao,
    private val fileDao: FileDao,
    private val participantDao: ParticipantDao,
    private val auditLogger: AuditLogger,
    private val appPreferences: AppPreferences
) : BaseViewModel<MeetingDetailContract.State, MeetingDetailContract.Intent, MeetingDetailContract.Effect>(
    initialState = MeetingDetailContract.State()
) {
    
    // 记录会议打开时间，用于计算持续时间
    private var meetingOpenTime: Long = 0L
    private var currentMeetingId: String? = null

    override fun handleIntent(intent: MeetingDetailContract.Intent) {
        when (intent) {
            is MeetingDetailContract.Intent.LoadMeetingDetail -> loadMeetingDetail(intent.meetingId)
            
            // 签到相关
            is MeetingDetailContract.Intent.ClickSignIn -> onClickSignIn()
            is MeetingDetailContract.Intent.UpdateSignInPassword -> updateSignInPassword(intent.password)
            is MeetingDetailContract.Intent.SubmitPasswordSignIn -> submitPasswordSignIn()
            is MeetingDetailContract.Intent.DismissPasswordSignInDialog -> dismissPasswordSignInDialog()
            is MeetingDetailContract.Intent.SubmitManualSignIn -> submitManualSignIn(intent.signatureBitmap)
            is MeetingDetailContract.Intent.DismissManualSignInDialog -> dismissManualSignInDialog()
            
            // 功能按钮
            is MeetingDetailContract.Intent.ClickMeetingInfo -> showInfoDialog()
            is MeetingDetailContract.Intent.ClickAgendas -> navigateToAgendas()
            is MeetingDetailContract.Intent.ClickVoting -> showComingSoon("会议投票")
            is MeetingDetailContract.Intent.ClickRecords -> showComingSoon("会议记录")
            
            // 弹窗控制
            is MeetingDetailContract.Intent.DismissInfoDialog -> dismissInfoDialog()
            is MeetingDetailContract.Intent.ShowParticipantsDialog -> showParticipantsDialog()
            is MeetingDetailContract.Intent.DismissParticipantsDialog -> dismissParticipantsDialog()
            
            // 退出相关
            is MeetingDetailContract.Intent.NavigateBack -> navigateBack()
            is MeetingDetailContract.Intent.ShowExitConfirmDialog -> showExitConfirmDialog()
            is MeetingDetailContract.Intent.DismissExitConfirmDialog -> dismissExitConfirmDialog()
            is MeetingDetailContract.Intent.ConfirmExit -> confirmExit()
            
            // 文件操作
            is MeetingDetailContract.Intent.OpenFile -> openFile(intent.fileId)
        }
    }

    /**
     * 加载会议详情
     */
    private fun loadMeetingDetail(meetingId: String) {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true, errorMessage = null) }

                // 获取当前登录用户名
                val userName = appPreferences.currentUserName.first()

                // 加载会议基本信息
                val meeting = meetingDao.getById(meetingId)
                if (meeting == null) {
                    updateState { 
                        copy(
                            isLoading = false,
                            errorMessage = "会议不存在"
                        )
                    }
                    sendEffect(MeetingDetailContract.Effect.ShowError("会议不存在"))
                    return@launch
                }

                // 加载议程列表
                val agendas = agendaDao.getByMeetingId(meetingId)

                // 为每个议程加载文件
                val agendasWithFiles = agendas.map { agenda ->
                    val files = fileDao.getByAgendaId(agenda.id)
                    MeetingDetailContract.AgendaWithFiles(
                        agenda = agenda,
                        files = files
                    )
                }

                // 加载参会人员
                val participants = participantDao.getByMeetingId(meetingId)

                Timber.d("加载会议详情成功: ${meeting.name}, 签到类型: ${meeting.signInType}")

                // 记录审计日志 - 会议打开
                meetingOpenTime = System.currentTimeMillis()
                currentMeetingId = meetingId
                logMeetingOpen(meetingId)

                // 免签模式自动签到
                val isSignedIn = meeting.signInType == "none"

                updateState {
                    copy(
                        isLoading = false,
                        meeting = meeting,
                        agendas = agendasWithFiles,
                        participants = participants,
                        currentUserName = userName,
                        isSignedIn = isSignedIn,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "加载会议详情失败")
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
                sendEffect(MeetingDetailContract.Effect.ShowError("加载会议详情失败"))
            }
        }
    }

    // ========== 签到相关 ==========

    /**
     * 点击签到按钮
     */
    private fun onClickSignIn() {
        val signInType = currentState.signInType
        when (signInType) {
            "password" -> {
                updateState { 
                    copy(
                        showPasswordSignInDialog = true,
                        signInPassword = "",
                        signInPasswordError = null
                    )
                }
            }
            "manual" -> {
                updateState { copy(showManualSignInDialog = true) }
            }
        }
    }

    /**
     * 更新密码输入
     */
    private fun updateSignInPassword(password: String) {
        // 只允许输入6位数字
        if (password.length <= 6 && password.all { it.isDigit() }) {
            updateState { 
                copy(
                    signInPassword = password,
                    signInPasswordError = null
                )
            }
        }
    }

    /**
     * 提交密码签到
     */
    private fun submitPasswordSignIn() {
        val password = currentState.signInPassword
        val meetingPassword = currentState.meeting?.password
        
        if (password.length != 6) {
            updateState { copy(signInPasswordError = "请输入6位密码") }
            return
        }
        
        if (password != meetingPassword) {
            updateState { copy(signInPasswordError = "密码错误") }
            return
        }
        
        // 密码正确，签到成功
        viewModelScope.launch {
            val meetingId = currentMeetingId ?: return@launch
            val userId = appPreferences.currentUserId.first() ?: "guest"
            val userName = appPreferences.currentUserName.first() ?: "访客"
            
            // 记录签到日志
            auditLogger.logSignIn(meetingId, userId, userName, "password")
            
            updateState { 
                copy(
                    isSignedIn = true,
                    showPasswordSignInDialog = false,
                    signInPassword = "",
                    signInPasswordError = null
                )
            }
            
            sendEffect(MeetingDetailContract.Effect.ShowToast("签到成功"))
        }
    }

    /**
     * 关闭密码签到弹窗
     */
    private fun dismissPasswordSignInDialog() {
        updateState { 
            copy(
                showPasswordSignInDialog = false,
                signInPassword = "",
                signInPasswordError = null
            )
        }
    }

    /**
     * 提交手写签到
     */
    private fun submitManualSignIn(signatureBitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val meetingId = currentMeetingId ?: return@launch
                val userId = appPreferences.currentUserId.first() ?: "guest"
                val userName = appPreferences.currentUserName.first() ?: "访客"
                val isDeveloperMode = appPreferences.developerModeEnabled.first()
                
                // 生成签名文件名
                val timestamp = System.currentTimeMillis() / 1000
                val fileName = "signature_${meetingId}_${userId}_$timestamp.png"
                
                // 保存签名文件
                val signaturesDir = StoragePathManager.getSignaturesDirectory(isDeveloperMode)
                if (!signaturesDir.exists()) {
                    signaturesDir.mkdirs()
                }
                
                val signatureFile = File(signaturesDir, fileName)
                
                withContext(Dispatchers.IO) {
                    FileOutputStream(signatureFile).use { out ->
                        signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                
                Timber.i("签名文件保存成功: ${signatureFile.absolutePath}")
                
                // 记录签到日志
                auditLogger.logSignIn(meetingId, userId, userName, "manual", fileName)
                
                updateState { 
                    copy(
                        isSignedIn = true,
                        showManualSignInDialog = false
                    )
                }
                
                sendEffect(MeetingDetailContract.Effect.ShowToast("签到成功"))
                
            } catch (e: Exception) {
                Timber.e(e, "保存签名失败")
                sendEffect(MeetingDetailContract.Effect.ShowError("签到失败: ${e.message}"))
            }
        }
    }

    /**
     * 关闭手写签到弹窗
     */
    private fun dismissManualSignInDialog() {
        updateState { copy(showManualSignInDialog = false) }
    }

    // ========== 功能按钮 ==========

    /**
     * 显示会议介绍弹窗
     */
    private fun showInfoDialog() {
        if (!currentState.isFunctionEnabled) {
            sendEffect(MeetingDetailContract.Effect.ShowToast("请先签到"))
            return
        }
        updateState { copy(showInfoDialog = true) }
    }

    /**
     * 关闭会议介绍弹窗
     */
    private fun dismissInfoDialog() {
        updateState { copy(showInfoDialog = false) }
    }

    /**
     * 跳转到议程页面
     */
    private fun navigateToAgendas() {
        if (!currentState.isFunctionEnabled) {
            sendEffect(MeetingDetailContract.Effect.ShowToast("请先签到"))
            return
        }
        val meetingId = currentMeetingId ?: return
        sendEffect(MeetingDetailContract.Effect.NavigateToAgendas(meetingId))
    }

    /**
     * 显示功能开发中提示
     */
    private fun showComingSoon(featureName: String) {
        if (!currentState.isFunctionEnabled) {
            sendEffect(MeetingDetailContract.Effect.ShowToast("请先签到"))
            return
        }
        sendEffect(MeetingDetailContract.Effect.ShowToast("$featureName 功能开发中"))
    }

    // ========== 弹窗控制 ==========

    /**
     * 显示参会人员对话框
     */
    private fun showParticipantsDialog() {
        updateState { copy(showParticipantsDialog = true) }
    }

    /**
     * 关闭参会人员对话框
     */
    private fun dismissParticipantsDialog() {
        updateState { copy(showParticipantsDialog = false) }
    }

    // ========== 退出相关 ==========

    /**
     * 返回会议列表（不退出登录）
     */
    private fun navigateBack() {
        // 记录审计日志 - 会议关闭
        logMeetingClose()
        
        sendEffect(MeetingDetailContract.Effect.NavigateBack)
    }

    /**
     * 显示退出确认对话框
     */
    private fun showExitConfirmDialog() {
        updateState { copy(showExitConfirmDialog = true) }
    }

    /**
     * 关闭退出确认对话框
     */
    private fun dismissExitConfirmDialog() {
        updateState { copy(showExitConfirmDialog = false) }
    }

    /**
     * 确认退出（退出到登录页）
     */
    private fun confirmExit() {
        viewModelScope.launch {
            // 记录审计日志 - 会议关闭
            logMeetingClose()
            
            // 记录用户登出
            val userId = appPreferences.currentUserId.first()
            val userName = appPreferences.currentUserName.first()
            if (userId != null && userName != null) {
                auditLogger.logUserLogout(userId, userName)
            }
            
            // 清除登录状态
            appPreferences.clearCurrentUser()
            
            updateState { copy(showExitConfirmDialog = false) }
            sendEffect(MeetingDetailContract.Effect.NavigateToLogin)
        }
    }

    // ========== 文件操作 ==========

    /**
     * 打开文件
     */
    private fun openFile(fileId: String) {
        if (!currentState.isFunctionEnabled) {
            sendEffect(MeetingDetailContract.Effect.ShowToast("请先签到"))
            return
        }
        
        viewModelScope.launch {
            try {
                val file = fileDao.getById(fileId)
                if (file != null) {
                    Timber.d("打开文件: ${file.originalName}")
                    
                    // 记录审计日志 - 文件打开
                    logFileOpen(file.meetingId, file.id, file.originalName)
                    
                    sendEffect(
                        MeetingDetailContract.Effect.OpenFileViewer(
                            fileId = file.id,
                            filePath = file.localPath,
                            fileName = file.originalName
                        )
                    )
                } else {
                    sendEffect(MeetingDetailContract.Effect.ShowError("文件不存在"))
                }
            } catch (e: Exception) {
                Timber.e(e, "打开文件失败")
                sendEffect(MeetingDetailContract.Effect.ShowError("打开文件失败"))
            }
        }
    }
    
    // ========== 审计日志辅助方法 ==========
    
    /**
     * 记录会议打开日志
     */
    private fun logMeetingOpen(meetingId: String) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: "guest"
            val userName = appPreferences.currentUserName.first() ?: "访客"
            auditLogger.logMeetingOpen(meetingId, userId, userName)
        }
    }
    
    /**
     * 记录会议关闭日志
     */
    private fun logMeetingClose() {
        val meetingId = currentMeetingId ?: return
        val durationSec = if (meetingOpenTime > 0) {
            (System.currentTimeMillis() - meetingOpenTime) / 1000
        } else {
            0L
        }
        
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: "guest"
            val userName = appPreferences.currentUserName.first() ?: "访客"
            auditLogger.logMeetingClose(meetingId, durationSec, userId, userName)
        }
    }
    
    /**
     * 记录文件打开日志
     */
    private fun logFileOpen(meetingId: String, fileId: String, fileName: String) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: "guest"
            val userName = appPreferences.currentUserName.first() ?: "访客"
            auditLogger.logFileOpen(meetingId, fileId, fileName, userId, userName)
        }
    }

}
