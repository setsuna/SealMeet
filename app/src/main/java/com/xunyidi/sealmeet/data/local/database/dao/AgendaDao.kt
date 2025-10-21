package com.xunyidi.sealmeet.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import kotlinx.coroutines.flow.Flow

/**
 * 会议议程数据访问对象
 */
@Dao
interface AgendaDao {
    
    /**
     * 插入议程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agenda: MeetingAgendaEntity)
    
    /**
     * 批量插入议程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(agendas: List<MeetingAgendaEntity>)
    
    /**
     * 更新议程
     */
    @Update
    suspend fun update(agenda: MeetingAgendaEntity)
    
    /**
     * 删除议程
     */
    @Delete
    suspend fun delete(agenda: MeetingAgendaEntity)
    
    /**
     * 根据ID查询议程
     */
    @Query("SELECT * FROM meeting_agendas WHERE id = :agendaId")
    suspend fun getById(agendaId: String): MeetingAgendaEntity?
    
    /**
     * 根据会议ID查询所有议程（按顺序）
     */
    @Query("SELECT * FROM meeting_agendas WHERE meeting_id = :meetingId ORDER BY order_num")
    fun getByMeetingIdFlow(meetingId: String): Flow<List<MeetingAgendaEntity>>
    
    /**
     * 根据会议ID查询所有议程（非Flow）
     */
    @Query("SELECT * FROM meeting_agendas WHERE meeting_id = :meetingId ORDER BY order_num")
    suspend fun getByMeetingId(meetingId: String): List<MeetingAgendaEntity>
    
    /**
     * 根据会议ID和状态查询议程
     */
    @Query("SELECT * FROM meeting_agendas WHERE meeting_id = :meetingId AND status = :status ORDER BY order_num")
    fun getByMeetingAndStatusFlow(meetingId: String, status: String): Flow<List<MeetingAgendaEntity>>
    
    /**
     * 删除会议的所有议程
     */
    @Query("DELETE FROM meeting_agendas WHERE meeting_id = :meetingId")
    suspend fun deleteByMeetingId(meetingId: String)
    
    /**
     * 获取会议的议程总数
     */
    @Query("SELECT COUNT(*) FROM meeting_agendas WHERE meeting_id = :meetingId")
    fun getCountByMeetingIdFlow(meetingId: String): Flow<Int>
    
    /**
     * 更新议程状态
     */
    @Query("UPDATE meeting_agendas SET status = :status, updated_at = :updatedAt WHERE id = :agendaId")
    suspend fun updateStatus(agendaId: String, status: String, updatedAt: Long)
}
