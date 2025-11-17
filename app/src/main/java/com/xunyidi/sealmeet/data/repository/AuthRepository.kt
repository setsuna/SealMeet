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
     * @param userName 用户名
     * @param password 明文密码
     * @return 验证成功返回参会人员信息，失败返回null
     */
    suspend fun authenticateParticipant(
        userName: String,
        password: String
    ): AuthResult {
        try {
            // 1. 获取第一个会议ID（简化处理，实际应该让用户选择）
            val meetings = meetingDao.getAll()
            if (meetings.isEmpty()) {
                Timber.w("数据库中没有会议数据")
                return AuthResult.Error("未找到会议信息，请先同步数据")
            }
            
            val meetingId = meetings.first().id
            Timber.d("使用会议ID: $meetingId 进行登录验证")
            
            // 2. 根据会议ID和用户名查询参会人员
            val participant = participantDao.getByMeetingAndUserName(meetingId, userName)
            
            if (participant == null) {
                Timber.w("用户名不存在: $userName")
                return AuthResult.Error("用户名或密码错误")
            }
            
            // 3. 验证密码
            val storedPassword = participant.password
            if (storedPassword.isNullOrBlank()) {
                Timber.w("该用户未设置密码")
                return AuthResult.Error("该用户未设置密码，请联系管理员")
            }
            
            // 使用 BCrypt 验证密码
            val isPasswordValid = try {
                BCrypt.checkpw(password, storedPassword)
            } catch (e: Exception) {
                Timber.e(e, "密码验证异常")
                false
            }
            
            if (!isPasswordValid) {
                Timber.w("密码验证失败")
                return AuthResult.Error("用户名或密码错误")
            }
            
            // 4. 验证成功
            Timber.i("用户 $userName 登录成功")
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
