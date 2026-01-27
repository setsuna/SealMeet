package com.xunyidi.sealmeet.presentation.screen.meetingagendas

import com.xunyidi.sealmeet.core.mvi.UiEffect
import com.xunyidi.sealmeet.core.mvi.UiIntent
import com.xunyidi.sealmeet.core.mvi.UiState
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity

/**
 * 会议议题页面 - Contract
 */
object MeetingAgendasContract {
    
    data class State(
        val isLoading: Boolean = true,
        val meeting: MeetingEntity? = null,
        val agendas: List<AgendaWithFiles> = emptyList(),
        /** 展开的议程ID集合 */
        val expandedAgendaIds: Set<String> = emptySet(),
        val errorMessage: String? = null
    ) : UiState
    
    /**
     * 议程及其关联文件
     */
    data class AgendaWithFiles(
        val agenda: MeetingAgendaEntity,
        val files: List<MeetingFileEntity>
    )
    
    sealed interface Intent : UiIntent {
        /** 加载议程列表 */
        data class LoadAgendas(val meetingId: String) : Intent
        /** 切换议程展开/折叠 */
        data class ToggleAgendaExpanded(val agendaId: String) : Intent
        /** 全部展开 */
        data object ExpandAll : Intent
        /** 全部折叠 */
        data object CollapseAll : Intent
        /** 打开文件 */
        data class OpenFile(val file: MeetingFileEntity) : Intent
        /** 返回 */
        data object NavigateBack : Intent
    }
    
    sealed interface Effect : UiEffect {
        data class ShowError(val message: String) : Effect
        data class ShowToast(val message: String) : Effect
        /** 打开文件 */
        data class OpenFileViewer(
            val fileId: String,
            val filePath: String,
            val fileName: String,
            val mimeType: String
        ) : Effect
        /** 返回 */
        data object NavigateBack : Effect
    }
}
