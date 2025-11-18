package com.xunyidi.sealmeet.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow

/**
 * 会议数据访问对象
 */
@Dao
interface MeetingDao {
    
    /**
     * 插入会议（冲突时替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: MeetingEntity)
    
    /**
     * 批量插入会议
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meetings: List<MeetingEntity>)
    
    /**
     * 更新会议
     */
    @Update
    suspend fun update(meeting: MeetingEntity)
    
    /**
     * 删除会议
     */
    @Delete
    suspend fun delete(meeting: MeetingEntity)
    
    /**
     * 根据ID删除会议
     */
    @Query("DELETE FROM meetings WHERE id = :meetingId")
    suspend fun deleteById(meetingId: String)
    
    /**
     * 根据ID查询会议
     */
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getById(meetingId: String): MeetingEntity?
    
    /**
     * 根据ID查询会议（Flow）
     */
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    fun getByIdFlow(meetingId: String): Flow<MeetingEntity?>
    
    /**
     * 查询所有会议（非Flow）
     */
    @Query("SELECT * FROM meetings ORDER BY start_time DESC")
    suspend fun getAll(): List<MeetingEntity>
    
    /**
     * 查询所有会议（Flow）
     */
    @Query("SELECT * FROM meetings ORDER BY start_time DESC")
    fun getAllFlow(): Flow<List<MeetingEntity>>
    
    /**
     * 根据类型查询会议
     */
    @Query("SELECT * FROM meetings WHERE type = :type ORDER BY start_time DESC")
    fun getByTypeFlow(type: String): Flow<List<MeetingEntity>>
    
    /**
     * 根据类型查询会议（非Flow）
     */
    @Query("SELECT * FROM meetings WHERE type = :type ORDER BY start_time DESC")
    suspend fun getMeetingsByType(type: String): List<MeetingEntity>
    
    /**
     * 根据状态查询会议
     */
    @Query("SELECT * FROM meetings WHERE status = :status ORDER BY start_time DESC")
    fun getByStatusFlow(status: String): Flow<List<MeetingEntity>>
    
    /**
     * 查询标准会议（需要密码验证）
     */
    @Query("SELECT * FROM meetings WHERE type = 'standard' ORDER BY start_time DESC")
    fun getStandardMeetingsFlow(): Flow<List<MeetingEntity>>
    
    /**
     * 查询平板会议（快速会议）
     */
    @Query("SELECT * FROM meetings WHERE type = 'tablet' ORDER BY start_time DESC")
    fun getTabletMeetingsFlow(): Flow<List<MeetingEntity>>
    
    /**
     * 检查会议是否存在
     */
    @Query("SELECT EXISTS(SELECT 1 FROM meetings WHERE id = :meetingId)")
    suspend fun exists(meetingId: String): Boolean
    
    /**
     * 根据校验和查询会议（用于检测更新）
     */
    @Query("SELECT * FROM meetings WHERE id = :meetingId AND package_checksum = :checksum")
    suspend fun getByIdAndChecksum(meetingId: String, checksum: String): MeetingEntity?
    
    /**
     * 更新会议校验和
     */
    @Query("UPDATE meetings SET package_checksum = :checksum, updated_at = :updatedAt WHERE id = :meetingId")
    suspend fun updateChecksum(meetingId: String, checksum: String, updatedAt: Long)
    
    /**
     * 删除所有会议
     */
    @Query("DELETE FROM meetings")
    suspend fun deleteAll()
    
    /**
     * 获取会议总数
     */
    @Query("SELECT COUNT(*) FROM meetings")
    fun getCountFlow(): Flow<Int>
}
