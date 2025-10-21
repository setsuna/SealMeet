package com.xunyidi.sealmeet.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xunyidi.sealmeet.data.local.database.dao.AgendaDao
import com.xunyidi.sealmeet.data.local.database.dao.FileDao
import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import com.xunyidi.sealmeet.data.local.database.dao.ParticipantDao
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity

/**
 * SealMeet 应用数据库
 * 
 * 包含会议、参会人员、议程、文件等数据表
 */
@Database(
    entities = [
        MeetingEntity::class,
        MeetingParticipantEntity::class,
        MeetingAgendaEntity::class,
        MeetingFileEntity::class
    ],
    version = 2, // 增加版本号
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * 会议数据访问对象
     */
    abstract fun meetingDao(): MeetingDao
    
    /**
     * 参会人员数据访问对象
     */
    abstract fun participantDao(): ParticipantDao
    
    /**
     * 议程数据访问对象
     */
    abstract fun agendaDao(): AgendaDao
    
    /**
     * 文件数据访问对象
     */
    abstract fun fileDao(): FileDao
    
    companion object {
        const val DATABASE_NAME = "sealmeet.db"
    }
}
