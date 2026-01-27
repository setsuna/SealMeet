package com.xunyidi.sealmeet.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity
import kotlinx.coroutines.flow.Flow

/**
 * 参会人员数据访问对象
 */
@Dao
interface ParticipantDao {
    
    /**
     * 插入参会人员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(participant: MeetingParticipantEntity)
    
    /**
     * 批量插入参会人员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participants: List<MeetingParticipantEntity>)
    
    /**
     * 更新参会人员
     */
    @Update
    suspend fun update(participant: MeetingParticipantEntity)
    
    /**
     * 删除参会人员
     */
    @Delete
    suspend fun delete(participant: MeetingParticipantEntity)
    
    /**
     * 根据会议ID查询所有参会人员
     */
    @Query("SELECT * FROM meeting_participants WHERE meeting_id = :meetingId ORDER BY role, user_name")
    fun getByMeetingIdFlow(meetingId: String): Flow<List<MeetingParticipantEntity>>
    
    /**
     * 根据会议ID查询所有参会人员（非Flow）
     */
    @Query("SELECT * FROM meeting_participants WHERE meeting_id = :meetingId ORDER BY role, user_name")
    suspend fun getByMeetingId(meetingId: String): List<MeetingParticipantEntity>
    
    /**
     * 根据会议ID和用户ID查询参会人员
     */
    @Query("SELECT * FROM meeting_participants WHERE meeting_id = :meetingId AND user_id = :userId")
    suspend fun getByMeetingAndUser(meetingId: String, userId: String): MeetingParticipantEntity?
    
    /**
     * 根据会议ID和用户名查询参会人员（用于密码验证）
     */
    @Query("SELECT * FROM meeting_participants WHERE meeting_id = :meetingId AND user_name = :userName")
    suspend fun getByMeetingAndUserName(meetingId: String, userName: String): MeetingParticipantEntity?
    
    /**
     * 根据会议ID和角色查询参会人员
     */
    @Query("SELECT * FROM meeting_participants WHERE meeting_id = :meetingId AND role = :role")
    fun getByMeetingAndRoleFlow(meetingId: String, role: String): Flow<List<MeetingParticipantEntity>>
    
    /**
     * 查询会议的主持人
     */
    @Query("SELECT * FROM meeting_participants WHERE meeting_id = :meetingId AND role = 'host'")
    suspend fun getHostsByMeetingId(meetingId: String): List<MeetingParticipantEntity>
    
    /**
     * 删除会议的所有参会人员
     */
    @Query("DELETE FROM meeting_participants WHERE meeting_id = :meetingId")
    suspend fun deleteByMeetingId(meetingId: String)
    
    /**
     * 获取会议的参会人数
     */
    @Query("SELECT COUNT(*) FROM meeting_participants WHERE meeting_id = :meetingId")
    fun getCountByMeetingIdFlow(meetingId: String): Flow<Int>
    
    /**
     * 根据用户名查询所有会议中的参会记录（用于跨会议登录）
     */
    @Query("SELECT * FROM meeting_participants WHERE user_name = :userName")
    suspend fun getByUserName(userName: String): List<MeetingParticipantEntity>
    
    /**
     * 验证参会人员密码（用于标准会议）
     * 返回匹配的参会人员信息
     */
    @Query("""
        SELECT * FROM meeting_participants 
        WHERE meeting_id = :meetingId 
        AND user_name = :userName 
        AND password = :passwordHash
    """)
    suspend fun verifyPassword(
        meetingId: String,
        userName: String,
        passwordHash: String
    ): MeetingParticipantEntity?
}
