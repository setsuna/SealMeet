package com.xunyidi.sealmeet.di

import android.content.Context
import androidx.room.Room
import com.xunyidi.sealmeet.data.local.database.AppDatabase
import com.xunyidi.sealmeet.data.local.database.dao.AgendaDao
import com.xunyidi.sealmeet.data.local.database.dao.FileDao
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import com.xunyidi.sealmeet.data.local.database.dao.ParticipantDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // 开发阶段使用，生产环境需要正确的迁移策略
            .build()
    }
    
    @Provides
    @Singleton
    fun provideMeetingDao(database: AppDatabase): MeetingDao {
        return database.meetingDao()
    }
    
    @Provides
    @Singleton
    fun provideParticipantDao(database: AppDatabase): ParticipantDao {
        return database.participantDao()
    }
    
    @Provides
    @Singleton
    fun provideAgendaDao(database: AppDatabase): AgendaDao {
        return database.agendaDao()
    }
    
    @Provides
    @Singleton
    fun provideFileDao(database: AppDatabase): FileDao {
        return database.fileDao()
    }
}
