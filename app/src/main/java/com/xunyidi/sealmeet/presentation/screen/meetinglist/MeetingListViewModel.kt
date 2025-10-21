package com.xunyidi.sealmeet.presentation.screen.meetinglist

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 会议列表ViewModel
 */
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val meetingDao: MeetingDao
) : BaseViewModel<MeetingListContract.State, MeetingListContract.Intent, MeetingListContract.Effect>(
    initialState = MeetingListContract.State()
) {

    init {
        // 初始化时加载会议列表
        handleIntent(MeetingListContract.Intent.LoadMeetings)
    }

    override fun handleIntent(intent: MeetingListContract.Intent) {
        when (intent) {
            is MeetingListContract.Intent.LoadMeetings -> loadMeetings()
            is MeetingListContract.Intent.Refresh -> refresh()
            is MeetingListContract.Intent.SelectMeeting -> selectMeeting(intent.meetingId)
        }
    }

    /**
     * 加载平板会议列表（type = tablet）
     */
    private fun loadMeetings() {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true, errorMessage = null) }
                
                // 查询type为tablet的会议
                val meetings = meetingDao.getMeetingsByType("tablet")
                
                Timber.d("加载平板会议: ${meetings.size}个")
                
                updateState { 
                    copy(
                        isLoading = false,
                        meetings = meetings,
                        errorMessage = null
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "加载会议列表失败")
                updateState { 
                    copy(
                        isLoading = false,
                        errorMessage = "加载失败: ${e.message}"
                    )
                }
                sendEffect(MeetingListContract.Effect.ShowError("加载会议失败"))
            }
        }
    }

    /**
     * 刷新列表
     */
    private fun refresh() {
        loadMeetings()
    }

    /**
     * 选择会议
     */
    private fun selectMeeting(meetingId: String) {
        Timber.d("选择会议: $meetingId")
        sendEffect(MeetingListContract.Effect.NavigateToMeetingDetail(meetingId))
    }
}
