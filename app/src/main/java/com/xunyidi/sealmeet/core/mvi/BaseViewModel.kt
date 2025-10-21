package com.xunyidi.sealmeet.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * MVI架构的ViewModel基类
 * 
 * @param STATE UI状态类型
 * @param INTENT 用户意图类型
 * @param EFFECT 副作用类型
 * 
 * 使用方式：
 * ```
 * class MyViewModel : BaseViewModel<MyState, MyIntent, MyEffect>(MyState()) {
 *     override fun handleIntent(intent: MyIntent) {
 *         when(intent) {
 *             is MyIntent.LoadData -> loadData()
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseViewModel<STATE : UiState, INTENT : UiIntent, EFFECT : UiEffect>(
    initialState: STATE
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<STATE> = _uiState.asStateFlow()

    // 副作用（一次性事件）
    private val _effect = Channel<EFFECT>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /**
     * 当前状态值
     */
    protected val currentState: STATE
        get() = _uiState.value

    /**
     * 处理用户意图
     * 子类必须实现此方法来处理具体的Intent
     */
    abstract fun handleIntent(intent: INTENT)

    /**
     * 更新UI状态
     */
    protected fun updateState(reducer: STATE.() -> STATE) {
        val newState = currentState.reducer()
        _uiState.value = newState
        logStateChange(newState)
    }

    /**
     * 发送副作用（一次性事件）
     */
    protected fun sendEffect(effect: EFFECT) {
        viewModelScope.launch {
            _effect.send(effect)
            logEffect(effect)
        }
    }

    /**
     * 日志记录 - 状态变更
     */
    private fun logStateChange(newState: STATE) {
        Timber.d("State changed: ${newState::class.simpleName}")
    }

    /**
     * 日志记录 - 副作用
     */
    private fun logEffect(effect: EFFECT) {
        Timber.d("Effect sent: ${effect::class.simpleName}")
    }
}
