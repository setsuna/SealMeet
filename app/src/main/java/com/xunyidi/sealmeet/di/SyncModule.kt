package com.xunyidi.sealmeet.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 同步模块依赖注入
 * 
 * 注意：SyncFileManager 已使用 @Inject 构造函数，Hilt 会自动提供实例，
 * 因此不需要在此 Module 中使用 @Provides 方法
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    // SyncFileManager 使用 @Inject 构造函数，不需要 @Provides
}
