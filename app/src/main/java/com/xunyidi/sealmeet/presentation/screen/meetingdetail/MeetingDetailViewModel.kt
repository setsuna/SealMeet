package com.xunyidi.sealmeet.presentation.screen.meetingdetail

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.local.database.dao.AgendaDao
import com.xunyidi.sealmeet.data.local.database.dao.FileDao
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import com.xunyidi.sealmeet.data.local.database.dao.ParticipantDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 会议详情ViewModel
 */
@HiltViewModel
class MeetingDetailViewModel @Inject constructor(
    private val meetingDao: MeetingDao,
    private val agendaDao: AgendaDao,
    private val fileDao: FileDao,
    private val participantDao: ParticipantDao
) : BaseViewModel<MeetingDetailContract.State, MeetingDetailContract.Intent, MeetingDetailContract.Effect>(
    initialState = MeetingDetailContract.State()
) {

    override fun handleIntent(intent: MeetingDetailContract.Intent) {
        when (intent) {
            is MeetingDetailContract.Intent.LoadMeetingDetail -> loadMeetingDetail(intent.meetingId)
            is MeetingDetailContract.Intent.ShowParticipantsDialog -> showParticipantsDialog()
            is MeetingDetailContract.Intent.DismissParticipantsDialog -> dismissParticipantsDialog()
            is MeetingDetailContract.Intent.ShowExitConfirmDialog -> showExitConfirmDialog()
            is MeetingDetailContract.Intent.DismissExitConfirmDialog -> dismissExitConfirmDialog()
            is MeetingDetailContract.Intent.ConfirmExit -> confirmExit()
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

                // 如果是快速会议，加载参会人员
                val participants = if (meeting.type == "tablet") {
                    participantDao.getByMeetingId(meetingId)
                } else {
                    emptyList()
                }

                Timber.d("加载会议详情成功: ${meeting.name}, 议程数: ${agendas.size}, 参会人员数: ${participants.size}")

                updateState {
                    copy(
                        isLoading = false,
                        meeting = meeting,
                        agendas = agendasWithFiles,
                        participants = participants,
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
     * 确认退出
     */
    private fun confirmExit() {
        updateState { copy(showExitConfirmDialog = false) }
        sendEffect(MeetingDetailContract.Effect.NavigateBack)
    }

    /**
     * 打开文件
     */
    private fun openFile(fileId: String) {
        viewModelScope.launch {
            try {
                val file = fileDao.getById(fileId)
                if (file != null) {
                    Timber.d("打开文件: ${file.originalName}")
                    sendEffect(
                        MeetingDetailContract.Effect.OpenFileViewer(
                            fileId = file.id,
                            filePath = file.localPath
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

}
