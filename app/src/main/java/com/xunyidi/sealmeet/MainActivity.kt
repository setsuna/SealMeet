package com.xunyidi.sealmeet

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.xunyidi.sealmeet.presentation.screen.login.LoginScreen
import com.xunyidi.sealmeet.presentation.screen.splash.SplashScreen
import com.xunyidi.sealmeet.presentation.theme.SealMeetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SealMeetTheme {
                LoginScreen(
                    onNavigateToHome = {
                        // TODO: 导航到主页
                        Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
