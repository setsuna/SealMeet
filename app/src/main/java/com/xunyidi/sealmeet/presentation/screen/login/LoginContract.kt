package com.xunyidi.sealmeet.presentation.screen.login

import com.xunyidi.sealmeet.core.mvi.UiEffect
import com.xunyidi.sealmeet.core.mvi.UiIntent
import com.xunyidi.sealmeet.core.mvi.UiState

/**
 * 登录页面的Contract
 */
object LoginContract {

    /**
     * UI状态
     */
    data class State(
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val usernameError: String? = null,
        val passwordError: String? = null,
        val isPasswordVisible: Boolean = false
    ) : UiState

    /**
     * 用户意图
     */
    sealed interface Intent : UiIntent {
        data class UsernameChanged(val username: String) : Intent
        data class PasswordChanged(val password: String) : Intent
        data object TogglePasswordVisibility : Intent
        data object Login : Intent
    }

    /**
     * 副作用（一次性事件）
     */
    sealed interface Effect : UiEffect {
        data object NavigateToHome : Effect
        data class ShowError(val message: String) : Effect
    }
}
