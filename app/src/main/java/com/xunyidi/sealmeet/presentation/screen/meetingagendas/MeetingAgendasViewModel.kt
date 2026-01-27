package com.xunyidi.sealmeet.presentation.screen.meetingagendas

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.audit.AuditLogger
import com.xunyidi.sealmeet.data.local.database.dao.AgendaDao
import com.xunyidi.sealmeet.data.local.database.dao.FileDao
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 会议议题ViewModel
 */
@HiltViewModel
class MeetingAgendasViewModel @Inject constructor(
    private val meetingDao: MeetingDao,
    private val agendaDao: AgendaDao,
    private val fileDao: FileDao,
    private val auditLogger: AuditLogger,
    private val appPreferences: AppPreferences
) : BaseViewModel<MeetingAgendasContract.State, MeetingAgendasContract.Intent, MeetingAgendasContract.Effect>(
    initialState = MeetingAgendasContract.State()
) {
    
    private var currentMeetingId: String? = null

    override fun handleIntent(intent: MeetingAgendasContract.Intent) {
        when (intent) {
            is MeetingAgendasContract.Intent.LoadAgendas -> loadAgendas(intent.meetingId)
            is MeetingAgendasContract.Intent.ToggleAgendaExpanded -> toggleAgendaExpanded(intent.agendaId)
            is MeetingAgendasContract.Intent.ExpandAll -> expandAll()
            is MeetingAgendasContract.Intent.CollapseAll -> collapseAll()
            is MeetingAgendasContract.Intent.OpenFile -> openFile(intent.file)
            is MeetingAgendasContract.Intent.NavigateBack -> navigateBack()
        }
    }

    /**
     * 加载议程列表
     */
    private fun loadAgendas(meetingId: String) {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true, errorMessage = null) }
                currentMeetingId = meetingId

                // 加载会议基本信息
                val meeting = meetingDao.getById(meetingId)
                if (meeting == null) {
                    updateState { 
                        copy(isLoading = false, errorMessage = "会议不存在")
                    }
                    return@launch
                }

                // 加载议程列表（按orderNum排序）
                val agendas = agendaDao.getByMeetingId(meetingId)

                // 为每个议程加载文件
                val agendasWithFiles = agendas.map { agenda ->
                    val files = fileDao.getByAgendaId(agenda.id)
                    MeetingAgendasContract.AgendaWithFiles(
                        agenda = agenda,
                        files = files
                    )
                }

                // 默认展开第一个议程
                val initialExpanded = if (agendasWithFiles.isNotEmpty()) {
                    setOf(agendasWithFiles.first().agenda.id)
                } else {
                    emptySet()
                }

                Timber.d("加载议程成功: ${meeting.name}, 议程数: ${agendas.size}")

                updateState {
                    copy(
                        isLoading = false,
                        meeting = meeting,
                        agendas = agendasWithFiles,
                        expandedAgendaIds = initialExpanded,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "加载议程失败")
                updateState {
                    copy(isLoading = false, errorMessage = "加载失败: ${e.message}")
                }
                sendEffect(MeetingAgendasContract.Effect.ShowError("加载议程失败"))
            }
        }
    }

    /**
     * 切换议程展开/折叠
     */
    private fun toggleAgendaExpanded(agendaId: String) {
        val currentExpanded = currentState.expandedAgendaIds
        val newExpanded = if (currentExpanded.contains(agendaId)) {
            currentExpanded - agendaId
        } else {
            currentExpanded + agendaId
        }
        updateState { copy(expandedAgendaIds = newExpanded) }
    }

    /**
     * 全部展开
     */
    private fun expandAll() {
        val allIds = currentState.agendas.map { it.agenda.id }.toSet()
        updateState { copy(expandedAgendaIds = allIds) }
    }

    /**
     * 全部折叠
     */
    private fun collapseAll() {
        updateState { copy(expandedAgendaIds = emptySet()) }
    }

    /**
     * 打开文件
     */
    private fun openFile(file: MeetingFileEntity) {
        viewModelScope.launch {
            try {
                Timber.d("打开文件: ${file.originalName}")
                
                // 记录审计日志
                val meetingId = currentMeetingId ?: return@launch
                val userId = appPreferences.currentUserId.first() ?: "guest"
                val userName = appPreferences.currentUserName.first() ?: "访客"
                auditLogger.logFileOpen(meetingId, file.id, file.originalName, userId, userName)
                
                sendEffect(
                    MeetingAgendasContract.Effect.OpenFileViewer(
                        fileId = file.id,
                        filePath = file.localPath,
                        fileName = file.originalName,
                        mimeType = file.mimeType
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "打开文件失败")
                sendEffect(MeetingAgendasContract.Effect.ShowError("打开文件失败"))
            }
        }
    }

    /**
     * 返回
     */
    private fun navigateBack() {
        sendEffect(MeetingAgendasContract.Effect.NavigateBack)
    }
}
