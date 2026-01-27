package com.xunyidi.sealmeet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.xunyidi.sealmeet.core.permission.PermissionRequester
import com.xunyidi.sealmeet.core.permission.StoragePermissionHelper
import com.xunyidi.sealmeet.data.sync.model.UnpackResult
import com.xunyidi.sealmeet.domain.usecase.UnpackMeetingUseCase
import com.xunyidi.sealmeet.presentation.screen.login.LoginScreen
import com.xunyidi.sealmeet.presentation.screen.meetingdetail.MeetingDetailScreen
import com.xunyidi.sealmeet.presentation.screen.meetinglist.MeetingListScreen
import com.xunyidi.sealmeet.presentation.theme.SealMeetTheme
import com.xunyidi.sealmeet.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var unpackMeetingUseCase: UnpackMeetingUseCase
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    private lateinit var permissionRequester: PermissionRequester
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Timber.i("通知权限已授予")
        } else {
            Timber.w("通知权限被拒绝")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 设置全屏显示
        setupFullScreen()
        
        permissionRequester = PermissionRequester(this) { granted ->
            if (granted) {
                Timber.i("存储权限已授予")
                checkAndUnpackOnStart()
                requestNotificationPermission()
            } else {
                Timber.w("存储权限被拒绝")
                Toast.makeText(this, "需要完整存储访问权限才能同步会议数据", Toast.LENGTH_LONG).show()
            }
        }
        
        // 检查权限
        if (StoragePermissionHelper.hasStoragePermission(this)) {
            Timber.i("已有存储权限，开始解包")
            checkAndUnpackOnStart()
            requestNotificationPermission()
        } else {
            Timber.w("无存储权限，请求授权")
            permissionRequester.requestPermissions()
        }

        setContent {
            SealMeetTheme {
                var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { 
                    mutableStateOf<Screen>(Screen.Login) 
                }

                when (val screen = currentScreen) {
                    is Screen.Login -> {
                        LoginScreen(
                            onNavigateToHome = {
                                // 账号密码登录 -> 显示所有会议（standard + tablet）
                                currentScreen = Screen.MeetingList(meetingType = "all")
                            },
                            onNavigateToQuickMeeting = {
                                // 快速会议 -> 只显示平板会议
                                currentScreen = Screen.MeetingList(meetingType = "tablet")
                            }
                        )
                    }
                    is Screen.MeetingList -> {
                        key(screen.meetingType) {
                            MeetingListScreen(
                                meetingType = screen.meetingType,
                                onNavigateBack = {
                                    currentScreen = Screen.Login
                                },
                                onNavigateToDetail = { meetingId ->
                                    currentScreen = Screen.MeetingDetail(meetingId, screen.meetingType)
                                }
                            )
                        }
                    }
                    is Screen.MeetingDetail -> {
                        MeetingDetailScreen(
                            meetingId = screen.meetingId,
                            onNavigateBack = {
                                // 返回到之前的会议列表类型
                                currentScreen = Screen.MeetingList(meetingType = screen.meetingType)
                            },
                            onNavigateToAgendas = { meetingId ->
                                // TODO: 跳转到议程页面
                                // currentScreen = Screen.MeetingAgendas(meetingId)
                            },
                            onNavigateToLogin = {
                                // 退出会议，返回登录页
                                currentScreen = Screen.Login
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun checkAndUnpackOnStart() {
        lifecycleScope.launch {
            try {
                Timber.i("开始检查待解包的会议文件...")
                val results = unpackMeetingUseCase.unpackAllPendingPackages()

                var successCount = 0
                val successMeetings = mutableListOf<String>()
                
                results.forEach { result ->
                    when (result) {
                        is UnpackResult.Success -> {
                            successCount++
                            successMeetings.add(result.meetingId)
                            Timber.i("✅ 解包成功: ${result.meetingId}, 文件数: ${result.fileCount}")
                        }
                        is UnpackResult.Failure -> {
                            Timber.e("❌ 解包失败: ${result.meetingId}, 原因: ${result.error}")
                        }
                    }
                }

                if (results.isNotEmpty()) {
                    Timber.i("解包完成，共处理 ${results.size} 个会议包")
                    
                    // 发送解包成功通知
                    if (successCount > 0) {
                        notificationHelper.showUnpackSuccessNotification(successCount, successMeetings)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "解包过程异常")
            }
        }
    }
}

sealed class Screen {
    data object Login : Screen()
    data class MeetingList(val meetingType: String) : Screen()
    data class MeetingDetail(val meetingId: String, val meetingType: String = "tablet") : Screen()
}

/**
 * Screen状态保存器，用于在Activity重建时保持状态
 */
val ScreenSaver = androidx.compose.runtime.saveable.Saver<Screen, String>(
    save = { screen ->
        when (screen) {
            is Screen.Login -> "login"
            is Screen.MeetingList -> "meeting_list:${screen.meetingType}"
            is Screen.MeetingDetail -> "meeting_detail:${screen.meetingId}:${screen.meetingType}"
        }
    },
    restore = { savedValue ->
        when {
            savedValue == "login" -> Screen.Login
            savedValue.startsWith("meeting_list:") -> {
                val meetingType = savedValue.substringAfter("meeting_list:")
                Screen.MeetingList(meetingType)
            }
            savedValue.startsWith("meeting_detail:") -> {
                val parts = savedValue.substringAfter("meeting_detail:").split(":")
                val meetingId = parts[0]
                val meetingType = parts.getOrNull(1) ?: "tablet"
                Screen.MeetingDetail(meetingId, meetingType)
            }
            else -> Screen.Login
        }
    }
)
