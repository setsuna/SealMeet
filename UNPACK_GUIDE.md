# 会议解包模块使用指南

## 📦 模块概述

本模块负责解包从后台同步的会议加密包文件，包含以下功能：

- ✅ AES-GCM解密
- ✅ ZIP解压
- ✅ 文件完整性校验
- ✅ Room数据库存储
- ✅ 全局同步锁机制

## 🏗️ 架构设计

### 数据层 (data/)

#### 数据库 Entity
- `MeetingEntity` - 会议信息
- `MeetingParticipantEntity` - 参会人员（包含password字段用于验证）
- `MeetingAgendaEntity` - 会议议程
- `MeetingFileEntity` - 文件信息

#### DAO
- `MeetingDao` - 会议CRUD操作
- `ParticipantDao` - 参会人员操作（包含密码验证方法）
- `AgendaDao` - 议程操作
- `FileDao` - 文件操作

#### 同步工具 (data/sync/)
- `AesGcmDecryptor` - AES-256-GCM解密器
- `FileUnzipper` - ZIP解压工具
- `ChecksumVerifier` - SHA-256校验和验证
- `SyncFileManager` - 同步文件管理器（含全局锁机制）

### 领域层 (domain/)

#### UseCase
- `UnpackMeetingUseCase` - 核心解包逻辑

## 🚀 快速开始

### 1. 在Application中初始化Hilt

```kotlin
@HiltAndroidApp
class SealMeetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

### 2. 在需要解包的地方注入UseCase

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val unpackMeetingUseCase: UnpackMeetingUseCase
) : BaseViewModel<State, Intent, Effect>() {
    
    // 在App启动时检查并解包
    fun checkAndUnpackOnStart() {
        viewModelScope.launch {
            val results = unpackMeetingUseCase.unpackAllPendingPackages()
            
            results.forEach { result ->
                when (result) {
                    is UnpackResult.Success -> {
                        Timber.i("解包成功: ${result.meetingId}")
                    }
                    is UnpackResult.Failure -> {
                        Timber.e("解包失败: ${result.meetingId}, ${result.error}")
                    }
                }
            }
        }
    }
}
```

### 3. 请求存储权限

在AndroidManifest.xml中添加：

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

在代码中请求权限：

```kotlin
// Android 13+ 使用新的权限
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
    ))
} else {
    requestPermissions(arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    ))
}
```

## 📂 文件存储结构

### 同步目录（Download目录）

```
/sdcard/Download/
  ├── .sync_lock                      # 全局同步锁文件（传输中）
  ├── meeting_{meetingID}.zip.enc     # 待解包文件1
  ├── meeting_{meetingID}.zip.enc     # 待解包文件2
  └── manifest.json                   # 同步清单（可选）
```

### 应用内部存储

```
/data/data/com.xunyidi.sealmeet/files/meetings/
  └── {meetingID}/
      ├── file1.pdf
      ├── file2.docx
      └── ...
```

## 🔐 加密与解密

### 密钥管理

- **统一密钥**: `package-encryption-key-32-chars!!`
- **算法**: AES-256-GCM
- **密钥派生**: `SHA256(masterKey + meetingID + salt)`

### 解密流程

1. 读取 `.zip.enc` 文件
2. 使用AES-GCM解密，得到ZIP数据
3. 解压ZIP，得到原始文件和manifest

## 🔒 全局同步锁机制

### 工作原理

1. **传输开始**: 在Download目录创建 `.sync_lock` 文件
2. **App检测**: 发现锁文件时跳过所有解包操作
3. **传输完成**: 删除 `.sync_lock` 锁文件
4. **App解包**: 下次检查时进行批量解包

### 实现建议（文件传输端）

```bash
#!/bin/bash
DOWNLOAD_DIR="/sdcard/Download"

# 1. 创建全局同步锁
adb shell "touch ${DOWNLOAD_DIR}/.sync_lock"
echo "开始同步..."

# 2. 传输所有会议包文件
adb push meeting_xxx.zip.enc ${DOWNLOAD_DIR}/
adb push meeting_yyy.zip.enc ${DOWNLOAD_DIR}/
adb push meeting_zzz.zip.enc ${DOWNLOAD_DIR}/

# 3. 传输manifest.json（可选）
adb push manifest.json ${DOWNLOAD_DIR}/

# 4. 删除同步锁
adb shell "rm ${DOWNLOAD_DIR}/.sync_lock"
echo "同步完成"
```

### Python传输脚本示例

```python
import os
import subprocess
import time

DOWNLOAD_DIR = "/sdcard/Download"
LOCK_FILE = f"{DOWNLOAD_DIR}/.sync_lock"

def sync_files(files_to_sync):
    """同步文件到设备"""
    
    # 1. 创建锁文件
    subprocess.run(['adb', 'shell', 'touch', LOCK_FILE])
    print("🔒 创建同步锁")
    
    try:
        # 2. 传输文件
        for file_path in files_to_sync:
            print(f"📤 传输: {os.path.basename(file_path)}")
            subprocess.run(['adb', 'push', file_path, DOWNLOAD_DIR])
        
        print("✅ 所有文件传输完成")
        
    finally:
        # 3. 删除锁文件（确保总是执行）
        time.sleep(1)  # 等待文件写入完成
        subprocess.run(['adb', 'shell', 'rm', LOCK_FILE])
        print("🔓 删除同步锁")

# 使用示例
files = [
    'meeting_xxx.zip.enc',
    'meeting_yyy.zip.enc',
    'manifest.json'
]
sync_files(files)
```

## 📋 manifest.json 结构

包内的manifest.json包含会议元数据：

```json
{
  "meeting_id": "uuid",
  "meeting_name": "会议名称",
  "package_time": "2025-10-20T...",
  "file_count": 5,
  "total_file_size": 12345678,
  "files": [
    {
      "id": "file-uuid",
      "original_name": "文件.pdf",
      "file_size": 123456,
      "mime_type": "application/pdf",
      "checksum": "sha256...",
      "agenda_id": "agenda-uuid"
    }
  ],
  "agendas": {
    "agenda-uuid": ["file-id-1", "file-id-2"]
  }
}
```

## ✅ 校验和验证

### checksum.json 结构

```json
{
  "package_checksum": "sha256...",
  "file_checksums": {
    "file-id-1": "sha256...",
    "file-id-2": "sha256..."
  },
  "created_at": "2025-10-20T..."
}
```

### 验证流程

1. 验证包的整体校验和（可选）
2. 验证每个文件的SHA-256校验和
3. 校验失败时删除损坏的包

## 🎯 会议类型与权限

### 会议类型 (Meeting.type)

- **standard**: 标准会议，需要账密验证
- **tablet**: 平板会议（快速会议），无需验证

### 权限验证

```kotlin
// 查询标准会议的参会人员（未来实现）
val participant = participantDao.verifyPassword(
    meetingId = "xxx",
    userName = "张三",
    passwordHash = hashPassword("123456") // 需要先hash密码
)

if (participant != null) {
    // 验证成功，允许进入
} else {
    // 验证失败
}
```

## 📊 参会人员和议程数据

### 扩展数据解析

在 `UnpackMeetingUseCase` 中预留了两个扩展方法：

```kotlin
/**
 * 解析参会人员数据
 * 需要根据实际数据格式实现
 */
private fun parseParticipantsData(
    zipData: ByteArray,
    meetingId: String
): List<MeetingParticipantEntity>

/**
 * 解析议程数据
 * 需要根据实际数据格式实现
 */
private fun parseAgendasData(
    zipData: ByteArray,
    meetingId: String
): List<MeetingAgendaEntity>
```

### 实现示例

如果后台在包中添加了 `participants.json` 和 `agendas.json`：

```kotlin
// 定义数据模型
data class ParticipantsData(
    val participants: List<ParticipantInfo>
)

data class ParticipantInfo(
    val id: String,
    val userId: String,
    val userName: String,
    val email: String?,
    val department: String?,
    val role: String,
    val password: String? // 已加密的密码
)

// 实现解析
private fun parseParticipantsData(
    zipData: ByteArray,
    meetingId: String
): List<MeetingParticipantEntity> {
    val participantsJson = unzipper.readFileFromZip(zipData, "participants.json")
        ?: return emptyList()
    
    val data = gson.fromJson(String(participantsJson), ParticipantsData::class.java)
    
    return data.participants.map { participant ->
        MeetingParticipantEntity(
            id = participant.id,
            meetingId = meetingId,
            userId = participant.userId,
            userName = participant.userName,
            email = participant.email,
            department = participant.department,
            role = participant.role,
            password = participant.password,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
```

## 🔄 增量更新机制

### 检测更新

```kotlin
// 通过checksum判断是否需要更新
val existingMeeting = meetingDao.getByIdAndChecksum(
    meetingId = "xxx",
    checksum = "new-checksum"
)

if (existingMeeting == null) {
    // checksum不匹配，需要更新
    // 删除旧数据，导入新数据
}
```

## 🐛 错误处理

### 解包错误类型

```kotlin
sealed class UnpackError {
    data class DecryptionFailed(val message: String)
    data class UnzipFailed(val message: String)
    data class ManifestInvalid(val message: String)
    data class ChecksumMismatch(val fileIds: List<String>)
    data class DatabaseError(val message: String)
    data class IOError(val message: String)
    data class Unknown(val message: String)
}
```

### 错误处理策略

- **解密失败**: 删除损坏包，记录日志
- **解压失败**: 删除损坏包，记录日志
- **校验失败**: 删除损坏包，记录日志
- **数据库错误**: 保留包文件，记录详细日志

## 📝 日志记录

使用Timber记录详细日志：

```kotlin
Timber.i("解包成功: meetingId=$meetingId, fileCount=$fileCount")
Timber.w("文件校验失败: fileId=$fileId")
Timber.e(exception, "解包异常: meetingId=$meetingId")
```

## ⚠️ 注意事项

1. **权限**: 确保已获取存储权限
2. **锁文件**: 传输文件时必须使用全局 `.sync_lock` 锁机制
3. **密钥**: 密钥必须与后台保持一致
4. **清理**: 解包失败后会自动删除损坏的包
5. **线程**: 所有解包操作都在IO线程执行
6. **事务**: 数据库操作使用事务保证一致性
7. **临时文件**: 解包过程中会在cache目录创建临时文件，完成后自动清理
8. **锁机制**: `.sync_lock` 存在时，App会跳过所有解包操作

## 🚧 待完善功能

1. ❌ **完整会议信息**: 当前manifest只包含基本信息，完整的会议信息需要从数据源获取或扩展manifest
2. ❌ **参会人员数据**: 需要实现 `parseParticipantsData` 方法，从包中解析参会人员信息
3. ❌ **议程数据**: 需要实现 `parseAgendasData` 方法，从包中解析议程信息
4. ❌ **密码加密**: 参会人员密码需要使用加密存储（标准会议开发时实现）
5. ❌ **进度回调**: 可以添加解包进度回调
6. ❌ **重试机制**: 解包失败时的重试策略
7. ❌ **manifest.json解析**: 全局manifest.json的解析和使用
8. ❌ **异常恢复**: 解包中断时的恢复机制

## 📚 相关文档

- [Room数据库官方文档](https://developer.android.com/training/data-storage/room)
- [Hilt依赖注入](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin协程](https://kotlinlang.org/docs/coroutines-overview.html)

---

**最后更新**: 2025年10月21日
