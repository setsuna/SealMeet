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
 * 
 * 版本历史：
 * - v1: 初始版本
 * - v2: 添加参会人员、议程等扩展功能
 * - v3: 会议表新增字段：password, expiry_type, expiry_date, sign_in_type, 
 *       organizer, organizer_name, host, host_name
 * - v4: 文件表新增字段：security_level
 */
@Database(
    entities = [
        MeetingEntity::class,
        MeetingParticipantEntity::class,
        MeetingAgendaEntity::class,
        MeetingFileEntity::class
    ],
    version = 4,
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
