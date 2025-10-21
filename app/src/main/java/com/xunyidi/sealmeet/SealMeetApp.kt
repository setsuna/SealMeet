package com.xunyidi.sealmeet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * SealMeet应用程序类
 */
@HiltAndroidApp
class SealMeetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 初始化Timber日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("SealMeet Application started")
    }
}
