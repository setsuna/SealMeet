package com.xunyidi.sealmeet.presentation.screen.meetinglist

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
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

    private var currentMeetingType: String = "tablet"
    private var observeJob: Job? = null

    init {
        // 初始化时立即开始监听
        observeMeetings()
    }

    override fun handleIntent(intent: MeetingListContract.Intent) {
        when (intent) {
            is MeetingListContract.Intent.LoadMeetings -> {
                // 由于已经在 observeMeetings() 中持续监听，这里只需重置状态
                updateState { copy(isLoading = true, errorMessage = null) }
            }
            is MeetingListContract.Intent.SetMeetingType -> {
                setMeetingType(intent.meetingType)
            }
            is MeetingListContract.Intent.Refresh -> refresh()
            is MeetingListContract.Intent.SelectMeeting -> selectMeeting(intent.meetingId)
        }
    }

    /**
     * 设置会议类型并开始监听
     */
    private fun setMeetingType(meetingType: String) {
        if (currentMeetingType != meetingType) {
            Timber.d("切换会议类型: $currentMeetingType -> $meetingType")
            currentMeetingType = meetingType
            updateState { copy(meetingType = meetingType) }
            
            // 取消之前的监听
            observeJob?.cancel()
            
            // 重新开始监听
            observeMeetings()
        }
    }

    /**
     * 持续监听会议列表
     * 根据会议类型查询：
     * - standard: 标准会议（账号密码登录）
     * - tablet: 平板会议（快速会议）
     */
    private fun observeMeetings() {
        observeJob = viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            val meetingsFlow = when (currentMeetingType) {
                "standard" -> {
                    Timber.d("监听标准会议列表")
                    meetingDao.getStandardMeetingsFlow()
                }
                "tablet" -> {
                    Timber.d("监听平板会议列表")
                    meetingDao.getTabletMeetingsFlow()
                }
                else -> {
                    Timber.w("未知会议类型: $currentMeetingType，使用平板会议")
                    meetingDao.getTabletMeetingsFlow()
                }
            }
            
            meetingsFlow
                .catch { e ->
                    Timber.e(e, "监听会议列表失败")
                    updateState { 
                        copy(
                            isLoading = false,
                            errorMessage = "加载失败: ${e.message}"
                        )
                    }
                    sendEffect(MeetingListContract.Effect.ShowError("加载会议失败"))
                }
                .collect { meetings ->
                    Timber.d("会议列表更新: ${meetings.size}个 (类型: $currentMeetingType)")
                    updateState { 
                        copy(
                            isLoading = false,
                            meetings = meetings,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * 刷新列表（Flow 会自动更新，这里只是提供手动触发的入口）
     */
    private fun refresh() {
        Timber.d("刷新会议列表（Flow 会自动更新）")
        // Flow 会自动检测数据库变化，无需手动刷新
        // 如果需要显示刷新动画，可以短暂设置 isLoading
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            // 延迟一下让用户看到刷新效果
            kotlinx.coroutines.delay(300)
        }
    }

    /**
     * 选择会议
     */
    private fun selectMeeting(meetingId: String) {
        Timber.d("选择会议: $meetingId")
        sendEffect(MeetingListContract.Effect.NavigateToMeetingDetail(meetingId))
    }
}
