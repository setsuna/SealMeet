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
    
    // v3 -> v4: 文件表新增 security_level 字段
    private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE meeting_files ADD COLUMN security_level TEXT NOT NULL DEFAULT 'internal'")
        }
    }
    
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
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration() // 迁移失败时才清空
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
