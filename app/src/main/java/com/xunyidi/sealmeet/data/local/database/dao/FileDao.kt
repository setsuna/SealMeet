package com.xunyidi.sealmeet.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import kotlinx.coroutines.flow.Flow

/**
 * 会议文件数据访问对象
 */
@Dao
interface FileDao {
    
    /**
     * 插入文件
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: MeetingFileEntity)
    
    /**
     * 批量插入文件
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<MeetingFileEntity>)
    
    /**
     * 更新文件
     */
    @Update
    suspend fun update(file: MeetingFileEntity)
    
    /**
     * 删除文件
     */
    @Delete
    suspend fun delete(file: MeetingFileEntity)
    
    /**
     * 根据ID查询文件
     */
    @Query("SELECT * FROM meeting_files WHERE id = :fileId")
    suspend fun getById(fileId: String): MeetingFileEntity?
    
    /**
     * 根据会议ID查询所有文件
     */
    @Query("SELECT * FROM meeting_files WHERE meeting_id = :meetingId ORDER BY original_name")
    fun getByMeetingIdFlow(meetingId: String): Flow<List<MeetingFileEntity>>
    
    /**
     * 根据会议ID查询所有文件（非Flow）
     */
    @Query("SELECT * FROM meeting_files WHERE meeting_id = :meetingId ORDER BY original_name")
    suspend fun getByMeetingId(meetingId: String): List<MeetingFileEntity>
    
    /**
     * 根据议程ID查询文件
     */
    @Query("SELECT * FROM meeting_files WHERE agenda_id = :agendaId ORDER BY original_name")
    fun getByAgendaIdFlow(agendaId: String): Flow<List<MeetingFileEntity>>
    
    /**
     * 根据议程ID查询文件（非Flow）
     */
    @Query("SELECT * FROM meeting_files WHERE agenda_id = :agendaId ORDER BY original_name")
    suspend fun getByAgendaId(agendaId: String): List<MeetingFileEntity>
    
    /**
     * 查询会议的无议程文件（agenda_id为null）
     */
    @Query("SELECT * FROM meeting_files WHERE meeting_id = :meetingId AND agenda_id IS NULL ORDER BY original_name")
    fun getOrphanFilesByMeetingIdFlow(meetingId: String): Flow<List<MeetingFileEntity>>
    
    /**
     * 删除会议的所有文件
     */
    @Query("DELETE FROM meeting_files WHERE meeting_id = :meetingId")
    suspend fun deleteByMeetingId(meetingId: String)
    
    /**
     * 删除议程的所有文件
     */
    @Query("DELETE FROM meeting_files WHERE agenda_id = :agendaId")
    suspend fun deleteByAgendaId(agendaId: String)
    
    /**
     * 获取会议的文件总数
     */
    @Query("SELECT COUNT(*) FROM meeting_files WHERE meeting_id = :meetingId")
    fun getCountByMeetingIdFlow(meetingId: String): Flow<Int>
    
    /**
     * 获取会议的文件总大小
     */
    @Query("SELECT SUM(file_size) FROM meeting_files WHERE meeting_id = :meetingId")
    suspend fun getTotalSizeByMeetingId(meetingId: String): Long?
    
    /**
     * 根据本地路径查询文件
     */
    @Query("SELECT * FROM meeting_files WHERE local_path = :localPath")
    suspend fun getByLocalPath(localPath: String): MeetingFileEntity?
}
