package com.xunyidi.sealmeet.presentation.screen.meetinglist

import com.xunyidi.sealmeet.core.mvi.UiEffect
import com.xunyidi.sealmeet.core.mvi.UiIntent
import com.xunyidi.sealmeet.core.mvi.UiState
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity

/**
 * 会议列表页面的Contract
 */
object MeetingListContract {

    /**
     * UI状态
     */
    data class State(
        val isLoading: Boolean = false,
        val meetings: List<MeetingEntity> = emptyList(),
        val errorMessage: String? = null,
        val meetingType: String = "tablet" // standard 或 tablet
    ) : UiState

    /**
     * 用户意图
     */
    sealed interface Intent : UiIntent {
        data object LoadMeetings : Intent
        data class SetMeetingType(val meetingType: String) : Intent
        data object Refresh : Intent
        data class SelectMeeting(val meetingId: String) : Intent
    }

    /**
     * 副作用（一次性事件）
     */
    sealed interface Effect : UiEffect {
        data class NavigateToMeetingDetail(val meetingId: String) : Effect
        data class ShowError(val message: String) : Effect
    }
}
