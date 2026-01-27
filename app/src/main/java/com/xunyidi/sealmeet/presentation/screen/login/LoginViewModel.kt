package com.xunyidi.sealmeet.presentation.screen.login

import androidx.lifecycle.viewModelScope
import com.xunyidi.sealmeet.core.mvi.BaseViewModel
import com.xunyidi.sealmeet.data.audit.AuditLogger
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.data.repository.AuthRepository
import com.xunyidi.sealmeet.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 登录页面ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appPreferences: AppPreferences,
    private val auditLogger: AuditLogger
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

            try {
                // 调用认证仓库进行登录验证
                val result = authRepository.authenticateParticipant(
                    userName = currentState.username,
                    password = currentState.password
                )

                updateState { copy(isLoading = false) }

                when (result) {
                    is AuthResult.Success -> {
                        val participant = result.participant
                        Timber.i("登录成功，用户: ${participant.userName}, 会议ID: ${result.meetingId}")
                        
                        // 记录审计日志
                        auditLogger.logUserLogin(participant.userId, participant.userName)
                        
                        // 保存当前登录用户信息
                        appPreferences.setCurrentUser(
                            userId = participant.userId,
                            userName = participant.userName,
                            meetingId = result.meetingId,
                            role = participant.role
                        )
                        
                        sendEffect(LoginContract.Effect.NavigateToHome)
                    }
                    is AuthResult.Error -> {
                        Timber.w("登录失败: ${result.message}")
                        sendEffect(LoginContract.Effect.ShowError(result.message))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "登录异常")
                updateState { copy(isLoading = false) }
                sendEffect(LoginContract.Effect.ShowError("登录失败: ${e.message}"))
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
