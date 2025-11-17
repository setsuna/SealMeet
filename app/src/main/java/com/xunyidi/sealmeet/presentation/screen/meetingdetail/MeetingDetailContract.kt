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
        val showParticipantsDialog: Boolean = false,
        val showExitConfirmDialog: Boolean = false
    ) : UiState
    
    /**
     * 议程及其关联文件
     */
    data class AgendaWithFiles(
        val agenda: MeetingAgendaEntity,
        val files: List<MeetingFileEntity>
    )
    
    sealed interface Intent : UiIntent {
        data class LoadMeetingDetail(val meetingId: String) : Intent
        data object ShowParticipantsDialog : Intent
        data object DismissParticipantsDialog : Intent
        data object ShowExitConfirmDialog : Intent
        data object DismissExitConfirmDialog : Intent
        data object ConfirmExit : Intent
        data class OpenFile(val fileId: String) : Intent
    }
    
    sealed interface Effect : UiEffect {
        data class ShowError(val message: String) : Effect
        data class OpenFileViewer(val fileId: String, val filePath: String, val fileName: String) : Effect
        data object NavigateBack : Effect
    }
}
