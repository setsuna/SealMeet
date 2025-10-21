package com.xunyidi.sealmeet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                SplashScreen()
            }
        }
    }
}
