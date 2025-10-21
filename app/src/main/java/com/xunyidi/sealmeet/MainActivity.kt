package com.xunyidi.sealmeet

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.xunyidi.sealmeet.data.sync.model.UnpackResult
import com.xunyidi.sealmeet.domain.usecase.UnpackMeetingUseCase
import com.xunyidi.sealmeet.presentation.screen.login.LoginScreen
import com.xunyidi.sealmeet.presentation.screen.meetinglist.MeetingListScreen
import com.xunyidi.sealmeet.presentation.theme.SealMeetTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var unpackMeetingUseCase: UnpackMeetingUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // App启动时检查并解包
        checkAndUnpackOnStart()

        setContent {
            SealMeetTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

                when (currentScreen) {
                    is Screen.Login -> {
                        LoginScreen(
                            onNavigateToHome = {
                                Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateToQuickMeeting = {
                                currentScreen = Screen.MeetingList
                            }
                        )
                    }
                    is Screen.MeetingList -> {
                        MeetingListScreen(
                            onNavigateBack = {
                                currentScreen = Screen.Login
                            },
                            onNavigateToDetail = { meetingId ->
                                Toast.makeText(
                                    this,
                                    "打开会议: $meetingId",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * App启动时检查并解包待处理的会议包
     */
    private fun checkAndUnpackOnStart() {
        lifecycleScope.launch {
            try {
                Timber.i("开始检查待解包的会议文件...")
                val results = unpackMeetingUseCase.unpackAllPendingPackages()

                results.forEach { result ->
                    when (result) {
                        is UnpackResult.Success -> {
                            Timber.i("✅ 解包成功: ${result.meetingId}, 文件数: ${result.fileCount}")
                        }
                        is UnpackResult.Failure -> {
                            Timber.e("❌ 解包失败: ${result.meetingId}, 原因: ${result.error}")
                        }
                    }
                }

                if (results.isNotEmpty()) {
                    Timber.i("解包完成，共处理 ${results.size} 个会议包")
                }
            } catch (e: Exception) {
                Timber.e(e, "解包过程异常")
            }
        }
    }
}

/**
 * 简单的Screen状态管理
 * 后续可替换为Navigation Compose
 */
sealed class Screen {
    data object Login : Screen()
    data object MeetingList : Screen()
}
