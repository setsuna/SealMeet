package com.xunyidi.sealmeet.data.repository

import com.xunyidi.sealmeet.data.local.database.dao.MeetingDao
import com.xunyidi.sealmeet.data.local.database.dao.ParticipantDao
import com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity
import org.mindrot.jbcrypt.BCrypt
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证仓库
 * 处理用户登录验证逻辑
 */
@Singleton
class AuthRepository @Inject constructor(
    private val participantDao: ParticipantDao,
    private val meetingDao: MeetingDao
) {
    
    /**
     * 验证参会人员登录
     * 
     * 逻辑：
     * 1. 根据用户名跨所有会议查找参会记录
     * 2. 过滤出有密码的记录
     * 3. 依次验证密码，找到匹配的就返回成功
     * 
     * @param userName 用户名
     * @param password 明文密码
     * @return 验证结果
     */
    suspend fun authenticateParticipant(
        userName: String,
        password: String
    ): AuthResult {
        try {
            // 1. 根据用户名查找所有会议中的参会记录
            val participants = participantDao.getByUserName(userName)
            
            if (participants.isEmpty()) {
                Timber.w("用户名不存在: $userName")
                return AuthResult.Error("用户名或密码错误")
            }
            
            Timber.d("找到 ${participants.size} 条参会记录，用户名: $userName")
            
            // 2. 过滤出有密码的记录
            val participantsWithPassword = participants.filter { !it.password.isNullOrBlank() }
            
            if (participantsWithPassword.isEmpty()) {
                Timber.w("该用户在所有会议中都未设置密码: $userName")
                return AuthResult.Error("该用户未设置密码，请联系管理员")
            }
            
            Timber.d("其中 ${participantsWithPassword.size} 条记录有密码")
            
            // 3. 依次验证密码
            for (participant in participantsWithPassword) {
                val isPasswordValid = try {
                    BCrypt.checkpw(password, participant.password)
                } catch (e: Exception) {
                    Timber.e(e, "密码验证异常，会议ID: ${participant.meetingId}")
                    false
                }
                
                if (isPasswordValid) {
                    // 4. 验证成功，返回该参会记录
                    Timber.i("用户 $userName 登录成功，会议ID: ${participant.meetingId}")
                    return AuthResult.Success(participant, participant.meetingId)
                }
            }
            
            // 所有密码都不匹配
            Timber.w("密码验证失败，用户名: $userName")
            return AuthResult.Error("用户名或密码错误")
            
        } catch (e: Exception) {
            Timber.e(e, "登录验证异常")
            return AuthResult.Error("登录失败: ${e.message}")
        }
    }
    
    /**
     * 验证指定会议的参会人员登录
     * 
     * @param meetingId 会议ID
     * @param userName 用户名
     * @param password 明文密码
     * @return 验证结果
     */
    suspend fun authenticateParticipantForMeeting(
        meetingId: String,
        userName: String,
        password: String
    ): AuthResult {
        try {
            // 1. 检查会议是否存在
            val meeting = meetingDao.getById(meetingId)
            if (meeting == null) {
                Timber.w("会议不存在: $meetingId")
                return AuthResult.Error("会议不存在")
            }
            
            // 2. 根据会议ID和用户名查询参会人员
            val participant = participantDao.getByMeetingAndUserName(meetingId, userName)
            
            if (participant == null) {
                Timber.w("用户不在该会议中: $userName, 会议ID: $meetingId")
                return AuthResult.Error("用户名或密码错误")
            }
            
            // 3. 验证密码
            val storedPassword = participant.password
            if (storedPassword.isNullOrBlank()) {
                Timber.w("该用户未设置密码: $userName")
                return AuthResult.Error("该用户未设置密码，请联系管理员")
            }
            
            val isPasswordValid = try {
                BCrypt.checkpw(password, storedPassword)
            } catch (e: Exception) {
                Timber.e(e, "密码验证异常")
                false
            }
            
            if (!isPasswordValid) {
                Timber.w("密码验证失败: $userName")
                return AuthResult.Error("用户名或密码错误")
            }
            
            // 4. 验证成功
            Timber.i("用户 $userName 登录成功，会议ID: $meetingId")
            return AuthResult.Success(participant, meetingId)
            
        } catch (e: Exception) {
            Timber.e(e, "登录验证异常")
            return AuthResult.Error("登录失败: ${e.message}")
        }
    }
}

/**
 * 认证结果
 */
sealed class AuthResult {
    data class Success(
        val participant: MeetingParticipantEntity,
        val meetingId: String
    ) : AuthResult()
    
    data class Error(val message: String) : AuthResult()
}
