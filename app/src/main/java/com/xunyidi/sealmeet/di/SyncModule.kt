package com.xunyidi.sealmeet.di

import com.xunyidi.sealmeet.data.sync.SyncFileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 同步模块依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    
    @Provides
    @Singleton
    fun provideSyncFileManager(): SyncFileManager {
        return SyncFileManager()
    }
}
