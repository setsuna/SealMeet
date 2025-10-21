package com.xunyidi.sealmeet.presentation.screen.login

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页面ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    // TODO: 注入 AuthRepository
) : BaseViewModel<LoginContract.State, LoginContract.Intent, LoginContract.Effect>(
    initialState = LoginContract.State()
) {

    override fun handleIntent(intent: LoginContract.Intent) {
        when (intent) {
            is LoginContract.Intent.UsernameChanged -> onUsernameChanged(intent.username)
            is LoginContract.Intent.PasswordChanged -> onPasswordChanged(intent.password)
            is LoginContract.Intent.TogglePasswordVisibility -> togglePasswordVisibility()
            is LoginContract.Intent.Login -> login()
        }
    }

    private fun onUsernameChanged(username: String) {
        updateState {
            copy(
                username = username,
                usernameError = null
            )
        }
    }

    private fun onPasswordChanged(password: String) {
        updateState {
            copy(
                password = password,
                passwordError = null
            )
        }
    }

    private fun togglePasswordVisibility() {
        updateState {
            copy(isPasswordVisible = !isPasswordVisible)
        }
    }

    private fun login() {
        // 表单验证
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            // 模拟登录请求（TODO: 调用真实的登录接口）
            delay(1500)

            // 模拟登录逻辑
            val success = currentState.username == "admin" && currentState.password == "123456"

            updateState { copy(isLoading = false) }

            if (success) {
                sendEffect(LoginContract.Effect.NavigateToHome)
            } else {
                sendEffect(LoginContract.Effect.ShowError("用户名或密码错误"))
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // 验证用户名
        if (currentState.username.isBlank()) {
            updateState { copy(usernameError = "请输入用户名") }
            isValid = false
        }

        // 验证密码
        if (currentState.password.isBlank()) {
            updateState { copy(passwordError = "请输入密码") }
            isValid = false
        } else if (currentState.password.length < 6) {
            updateState { copy(passwordError = "密码至少6位") }
            isValid = false
        }

        return isValid
    }
}
