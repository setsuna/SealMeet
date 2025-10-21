package com.xunyidi.sealmeet.presentation.screen.splash

import com.xunyidi.sealmeet.core.mvi.UiEffect
import com.xunyidi.sealmeet.core.mvi.UiIntent
import com.xunyidi.sealmeet.core.mvi.UiState

/**
 * 启动页面的Contract
 * 集中定义State、Intent、Effect
 */
object SplashContract {

    /**
     * UI状态
     */
    data class State(
        val isLoading: Boolean = true,
        val hasKey: Boolean = false,
        val errorMessage: String? = null
    ) : UiState

    /**
     * 用户意图
     */
    sealed interface Intent : UiIntent {
        data object CheckKeyStatus : Intent
        data object NavigateToMain : Intent
    }

    /**
     * 副作用（一次性事件）
     */
    sealed interface Effect : UiEffect {
        data object NavigateToMeetingList : Effect
        data class ShowError(val message: String) : Effect
    }
}
