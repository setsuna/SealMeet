package com.xunyidi.sealmeet.domain.usecase

import androidx.room.withTransaction
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.xunyidi.sealmeet.data.local.database.AppDatabase
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity
import com.xunyidi.sealmeet.data.preferences.AppPreferences
import com.xunyidi.sealmeet.data.sync.AesGcmDecryptor
import com.xunyidi.sealmeet.data.sync.ChecksumVerifier
import com.xunyidi.sealmeet.data.sync.DecryptionException
import com.xunyidi.sealmeet.data.sync.FileUnzipper
import com.xunyidi.sealmeet.data.sync.PackageFile
import com.xunyidi.sealmeet.data.sync.SyncFileManager
import com.xunyidi.sealmeet.data.sync.UnzipException
import com.xunyidi.sealmeet.data.sync.model.PackageChecksum
import com.xunyidi.sealmeet.data.sync.model.PackageManifest
import com.xunyidi.sealmeet.data.sync.model.UnpackError
import com.xunyidi.sealmeet.data.sync.model.UnpackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会议解包 UseCase
 * 
 * 负责完整的解包流程：
 * 1. 解密加密包
 * 2. 解压ZIP
 * 3. 验证文件完整性
 * 4. 保存文件到本地
 * 5. 存入Room数据库
 */
@Singleton
class UnpackMeetingUseCase @Inject constructor(
    private val database: AppDatabase,
    private val syncFileManager: SyncFileManager,
    private val appPreferences: AppPreferences,
    private val configApplyUseCase: ConfigApplyUseCase,
    private val context: android.content.Context
) {
    
    private val decryptor = AesGcmDecryptor(AesGcmDecryptor.DEFAULT_PACKAGE_KEY)
    private val unzipper = FileUnzipper()
    private val checksumVerifier = ChecksumVerifier()
    
    // 初始化Moshi
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * 解包单个会议包
     */
    suspend fun unpackMeeting(packageFile: PackageFile): UnpackResult = withContext(Dispatchers.IO) {
        Timber.i("开始解包会议: ${packageFile.meetingId}")
        
        // 读取配置
        val incrementalUpdateEnabled = appPreferences.incrementalUpdateEnabled.first()
        val keepTempFilesEnabled = appPreferences.keepTempFilesEnabled.first()
        
        Timber.i("配置: 增量更新=$incrementalUpdateEnabled, 保留临时文件=$keepTempFilesEnabled")
        
        try {
            // 1. 读取加密文件
            val encryptedData = packageFile.file.readBytes()
            Timber.d("读取加密文件完成，大小: ${encryptedData.size} bytes")
            
            // 2. 解密
            val zipData = try {
                decryptor.decrypt(encryptedData, packageFile.meetingId)
            } catch (e: DecryptionException) {
                Timber.e(e, "解密失败")
                syncFileManager.cleanupCorruptedPackage(packageFile, "解密失败")
                return@withContext UnpackResult.Failure(
                    packageFile.meetingId,
                    UnpackError.DecryptionFailed(e.message ?: "Unknown")
                )
            }
            Timber.d("解密完成，ZIP大小: ${zipData.size} bytes")
            
            // 3. 创建临时解压目录
            val tempDir = File(context.cacheDir, "unpack_${packageFile.meetingId}")
            tempDir.mkdirs()
            
            try {
                // 4. 解压ZIP
                val extractedFiles = try {
                    unzipper.unzip(zipData, tempDir)
                } catch (e: UnzipException) {
                    Timber.e(e, "解压失败")
                    syncFileManager.cleanupCorruptedPackage(packageFile, "解压失败")
                    return@withContext UnpackResult.Failure(
                        packageFile.meetingId,
                        UnpackError.UnzipFailed(e.message ?: "Unknown")
                    )
                }
                Timber.d("解压完成，文件数: ${extractedFiles.size}")
                
                // 5. 读取manifest.json
                val manifestData = unzipper.readFileFromZip(zipData, "manifest.json")
                if (manifestData == null) {
                    Timber.e("manifest.json 不存在")
                    syncFileManager.cleanupCorruptedPackage(packageFile, "manifest.json缺失")
                    return@withContext UnpackResult.Failure(
                        packageFile.meetingId,
                        UnpackError.ManifestInvalid("manifest.json不存在")
                    )
                }
                
                val manifest = try {
                    val adapter = moshi.adapter(PackageManifest::class.java)
                    adapter.fromJson(String(manifestData))
                } catch (e: JsonDataException) {
                    Timber.e(e, "manifest.json 解析失败")
                    syncFileManager.cleanupCorruptedPackage(packageFile, "manifest.json格式错误")
                    return@withContext UnpackResult.Failure(
                        packageFile.meetingId,
                        UnpackError.ManifestInvalid("JSON解析失败: ${e.message}")
                    )
                }
                
                if (manifest == null) {
                    Timber.e("manifest.json 解析结果为null")
                    syncFileManager.cleanupCorruptedPackage(packageFile, "manifest.json解析失败")
                    return@withContext UnpackResult.Failure(
                        packageFile.meetingId,
                        UnpackError.ManifestInvalid("JSON解析结果为null")
                    )
                }
                
                Timber.d("manifest.json 解析成功: ${manifest.meetingName}")
                
                // 6. 读取checksum.json（可选）
                val checksumData = unzipper.readFileFromZip(zipData, "checksum.json")
                val checksum = checksumData?.let {
                    try {
                        val adapter = moshi.adapter(PackageChecksum::class.java)
                        adapter.fromJson(String(it))
                    } catch (e: Exception) {
                        Timber.w(e, "checksum.json 解析失败，跳过校验")
                        null
                    }
                }
                
                // 7. 尝试解析会议完整信息
                val meetingData = parseMeetingData(zipData)
                
                // 8. 尝试解析扩展数据（参会人员和议程）
                val participants = parseParticipantsData(zipData, packageFile.meetingId)
                val agendas = parseAgendasData(zipData, packageFile.meetingId)
                
                // 8. 保存文件到永久存储
                val meetingDir = getMeetingDirectory(packageFile.meetingId)
                meetingDir.mkdirs()
                
                val fileEntityList = mutableListOf<MeetingFileEntity>()
                
                manifest.files.forEach { fileInfo ->
                    // 查找对应的文件
                    val sourceFile = findFileInTemp(tempDir, fileInfo.originalName)
                    
                    if (sourceFile != null && sourceFile.exists()) {
                        // 复制到永久存储
                        val destFile = File(meetingDir, fileInfo.originalName)
                        sourceFile.copyTo(destFile, overwrite = true)
                        
                        // 创建文件实体
                        fileEntityList.add(
                            MeetingFileEntity(
                                id = fileInfo.id,
                                meetingId = packageFile.meetingId,
                                agendaId = fileInfo.agendaId,
                                originalName = fileInfo.originalName,
                                localPath = destFile.absolutePath,
                                fileSize = fileInfo.fileSize,
                                mimeType = fileInfo.mimeType,
                                checksum = fileInfo.checksum,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        
                        Timber.d("保存文件: ${fileInfo.originalName}")
                    } else {
                        Timber.w("文件未找到: ${fileInfo.originalName}")
                    }
                }
                
                // 9. 存入数据库（事务）
                database.withTransaction {
                    // 如果关闭了增量更新，先清空所有数据
                    if (!incrementalUpdateEnabled) {
                        Timber.i("增量更新已关闭，清空所有数据...")
                        database.clearAllTables()
                        
                        // 删除所有会议文件
                        val meetingsRootDir = File(context.filesDir, "meetings")
                        if (meetingsRootDir.exists()) {
                            meetingsRootDir.deleteRecursively()
                            Timber.i("已删除所有会议文件")
                        }
                    } else {
                        // 增量更新模式：检查是否已存在该会议
                        val existingMeeting = database.meetingDao().getById(packageFile.meetingId)
                        if (existingMeeting != null) {
                            Timber.i("会议已存在，更新数据: ${packageFile.meetingId}")
                            // 删除旧的关联数据（会自动级联删除）
                            database.meetingDao().deleteById(packageFile.meetingId)
                            
                            // 删除旧的文件
                            val oldFiles = database.fileDao().getByMeetingId(packageFile.meetingId)
                            oldFiles.forEach { oldFile ->
                                File(oldFile.localPath).delete()
                            }
                        }
                    }
                    
                    // 插入会议信息
                    // 优先使用 meeting.json 的完整数据，如果没有则使用 manifest 的基本信息
                    val meetingEntity = if (meetingData != null) {
                        MeetingEntity(
                            id = meetingData.id,
                            name = meetingData.name,
                            startTime = parseIsoTimestamp(meetingData.startTime),
                            endTime = parseIsoTimestamp(meetingData.endTime),
                            type = meetingData.type,
                            status = meetingData.status,
                            securityLevel = meetingData.securityLevel,
                            isDraft = meetingData.isDraft,
                            maxParticipants = meetingData.maxParticipants,
                            location = meetingData.location,
                            description = meetingData.description,
                            category = meetingData.category,
                            createdBy = meetingData.createdBy,
                            createdByName = meetingData.createdByName,
                            createdAt = parseIsoTimestamp(meetingData.createdAt),
                            updatedAt = parseIsoTimestamp(meetingData.updatedAt),
                            packageChecksum = checksum?.packageChecksum
                        )
                    } else {
                        // 向后兼容：使用 manifest 的基本信息
                        MeetingEntity(
                            id = manifest.meetingId,
                            name = manifest.meetingName,
                            startTime = 0,
                            endTime = 0,
                            type = "tablet",
                            status = "preparation",
                            securityLevel = "internal",
                            createdBy = "",
                            createdByName = "",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            packageChecksum = checksum?.packageChecksum
                        )
                    }
                    
                    database.meetingDao().insert(meetingEntity)
                    
                    // 插入文件信息
                    database.fileDao().insertAll(fileEntityList)
                    
                    // 插入参会人员信息（如果有）
                    if (participants.isNotEmpty()) {
                        database.participantDao().insertAll(participants)
                        Timber.d("插入参会人员: ${participants.size} 人")
                    }
                    
                    // 插入议程信息（如果有）
                    if (agendas.isNotEmpty()) {
                        database.agendaDao().insertAll(agendas)
                        Timber.d("插入议程: ${agendas.size} 个")
                    }
                }
                
                Timber.i("解包成功: ${packageFile.meetingId}, 文件=${fileEntityList.size}, 参会人员=${participants.size}, 议程=${agendas.size}")
                
                // 10. 根据配置决定是否清理临时文件
                if (keepTempFilesEnabled) {
                    Timber.i("保留临时文件: ${tempDir.absolutePath}")
                } else {
                    tempDir.deleteRecursively()
                    Timber.d("已清理临时文件")
                }
                
                // 11. 删除原包文件
                syncFileManager.deletePackageFile(packageFile)
                
                UnpackResult.Success(
                    meetingId = packageFile.meetingId,
                    fileCount = fileEntityList.size
                )
                
            } finally {
                // 如果不保留临时文件，确保临时目录被清理
                if (!keepTempFilesEnabled && tempDir.exists()) {
                    tempDir.deleteRecursively()
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "解包异常")
            syncFileManager.cleanupCorruptedPackage(packageFile, "未知错误: ${e.message}")
            UnpackResult.Failure(
                packageFile.meetingId,
                UnpackError.Unknown(e.message ?: "Unknown")
            )
        }
    }
    
    /**
     * 批量解包所有待处理的包文件
     */
    suspend fun unpackAllPendingPackages(): List<UnpackResult> = withContext(Dispatchers.IO) {
        // 在开始解包前，先检查并应用服务器配置
        val configApplied = try {
            configApplyUseCase.checkAndApplyConfig()
        } catch (e: Exception) {
            Timber.e(e, "检查服务器配置失败")
            false
        }
        
        if (configApplied) {
            Timber.i("已应用服务器配置")
        }
        
        val packageFiles = syncFileManager.scanPackageFiles()
        
        if (packageFiles.isEmpty()) {
            Timber.i("没有待解包的文件（可能正在同步中或没有文件）")
            return@withContext emptyList()
        }
        
        Timber.i("发现 ${packageFiles.size} 个待解包文件")
        
        val results = mutableListOf<UnpackResult>()
        packageFiles.forEach { packageFile ->
            val result = unpackMeeting(packageFile)
            results.add(result)
        }
        
        val successCount = results.count { it is UnpackResult.Success }
        val failureCount = results.count { it is UnpackResult.Failure }
        
        Timber.i("批量解包完成: 成功=$successCount, 失败=$failureCount")
        
        results
    }
    
    /**
     * 解析会议完整信息
     * 从 ZIP 中读取 meeting.json
     */
    private fun parseMeetingData(zipData: ByteArray): com.xunyidi.sealmeet.data.sync.model.MeetingData? {
        return try {
            val meetingJson = unzipper.readFileFromZip(zipData, "meeting.json")
            if (meetingJson != null) {
                val adapter = moshi.adapter(com.xunyidi.sealmeet.data.sync.model.MeetingData::class.java)
                val data = adapter.fromJson(String(meetingJson))
                if (data != null) {
                    Timber.d("解析 meeting.json 成功: ${data.name}")
                } else {
                    Timber.w("meeting.json 解析结果为null")
                }
                data
            } else {
                Timber.d("meeting.json 不存在，使用 manifest 的基本信息")
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "meeting.json 解析失败，使用 manifest 的基本信息")
            null
        }
    }
    
    /**
     * 解析参会人员数据
     * 从 ZIP 中读取 participants.json
     */
    private fun parseParticipantsData(
        zipData: ByteArray,
        meetingId: String
    ): List<MeetingParticipantEntity> {
        return try {
            val participantsJson = unzipper.readFileFromZip(zipData, "participants.json")
            if (participantsJson != null) {
                val adapter = moshi.adapter(com.xunyidi.sealmeet.data.sync.model.ParticipantsWrapper::class.java)
                val data = adapter.fromJson(String(participantsJson))
                if (data != null) {
                    Timber.d("解析 participants.json 成功: ${data.participants.size} 人")
                    // 转换为 Entity
                    data.participants.map { participant ->
                        MeetingParticipantEntity(
                            id = participant.id,
                            meetingId = meetingId,
                            userId = participant.userId,
                            userName = participant.userName,
                            email = participant.email,
                            department = participant.department,
                            role = participant.role,
                            status = participant.status,
                            password = participant.password, // TODO: 如需要可加密存储
                            joinedAt = parseIsoTimestampOrNull(participant.joinedAt),
                            leftAt = parseIsoTimestampOrNull(participant.leftAt),
                            createdAt = parseIsoTimestamp(participant.createdAt),
                            updatedAt = parseIsoTimestamp(participant.updatedAt)
                        )
                    }
                } else {
                    Timber.w("participants.json 解析结果为null")
                    emptyList()
                }
            } else {
                Timber.d("participants.json 不存在")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.w(e, "participants.json 解析失败")
            emptyList()
        }
    }
    
    /**
     * 解析议程数据
     * 从 ZIP 中读取 agendas.json
     */
    private fun parseAgendasData(
        zipData: ByteArray,
        meetingId: String
    ): List<MeetingAgendaEntity> {
        return try {
            val agendasJson = unzipper.readFileFromZip(zipData, "agendas.json")
            if (agendasJson != null) {
                val adapter = moshi.adapter(com.xunyidi.sealmeet.data.sync.model.AgendasWrapper::class.java)
                val data = adapter.fromJson(String(agendasJson))
                if (data != null) {
                    Timber.d("解析 agendas.json 成功: ${data.agendas.size} 个议程")
                    // 转换为 Entity
                    data.agendas.map { agenda ->
                        MeetingAgendaEntity(
                            id = agenda.id,
                            meetingId = meetingId,
                            title = agenda.title,
                            description = agenda.description,
                            duration = agenda.duration,
                            presenter = agenda.presenter,
                            orderNum = agenda.orderNum,
                            status = agenda.status,
                            startedAt = parseIsoTimestampOrNull(agenda.startedAt),
                            completedAt = parseIsoTimestampOrNull(agenda.completedAt),
                            createdAt = parseIsoTimestamp(agenda.createdAt),
                            updatedAt = parseIsoTimestamp(agenda.updatedAt)
                        )
                    }
                } else {
                    Timber.w("agendas.json 解析结果为null")
                    emptyList()
                }
            } else {
                Timber.d("agendas.json 不存在")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.w(e, "agendas.json 解析失败")
            emptyList()
        }
    }
    
    /**
     * 解析 ISO8601 时间戳为毫秒
     */
    private fun parseIsoTimestamp(isoString: String): Long {
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            Timber.w(e, "时间戳解析失败: $isoString")
            System.currentTimeMillis()
        }
    }
    
    /**
     * 解析可空的 ISO8601 时间戳
     */
    private fun parseIsoTimestampOrNull(isoString: String?): Long? {
        return if (isoString != null) {
            try {
                java.time.Instant.parse(isoString).toEpochMilli()
            } catch (e: Exception) {
                Timber.w(e, "时间戳解析失败: $isoString")
                null
            }
        } else {
            null
        }
    }
    
    /**
     * 获取会议的本地存储目录
     */
    private fun getMeetingDirectory(meetingId: String): File {
        return File(context.filesDir, "meetings/$meetingId")
    }
    
    /**
     * 在临时目录中递归查找文件
     */
    private fun findFileInTemp(tempDir: File, fileName: String): File? {
        tempDir.walk().forEach { file ->
            if (file.isFile && file.name == fileName) {
                return file
            }
        }
        return null
    }
}
