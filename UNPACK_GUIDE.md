# 会议解包模块使用指南

## 📦 模块概述

本模块负责解包从后台同步的会议加密包文件，包含以下功能：

- ✅ AES-GCM解密
- ✅ ZIP解压
- ✅ 文件完整性校验
- ✅ Room数据库存储
- ✅ 全局同步锁机制
- ✅ 完整存储访问（MANAGE_EXTERNAL_STORAGE）

## 🔐 权限说明

本应用使用 `MANAGE_EXTERNAL_STORAGE` 权限，可以访问整个外部存储空间。

**适用场景**：
- ✅ 内部企业应用
- ✅ 涉密场所专用设备
- ❌ 不适合上架 Google Play（会被拒）

**首次启动**：
- App会自动跳转到设置页面
- 用户需手动开启"允许管理所有文件"
- 授权后自动开始解包

## 📂 文件存储结构

### 同步目录（Download目录）

```
/sdcard/Download/
  ├── .sync_lock                      # 全局同步锁文件（传输中）
  ├── meeting_{meetingID}.zip.enc     # 待解包文件
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

## 🚀 快速开始

### 1. 传输文件到设备

```bash
# 方式1: 使用锁机制的完整流程
DOWNLOAD_DIR="/sdcard/Download"

# 1. 创建同步锁
adb shell "touch $DOWNLOAD_DIR/.sync_lock"

# 2. 传输文件
adb push meeting_9b2da0a7-3716-4aeb-9873-dcdd42d03db9.zip.enc $DOWNLOAD_DIR/
adb push manifest.json $DOWNLOAD_DIR/

# 3. 删除锁文件
adb shell "rm $DOWNLOAD_DIR/.sync_lock"
```

```bash
# 方式2: 快速测试（无锁）
adb push meeting_xxx.zip.enc /sdcard/Download/
```

### 2. 启动App

- 首次启动会请求存储权限
- 在设置中开启"允许管理所有文件"
- 返回App自动开始解包

### 3. 查看日志

```bash
# 查看解包日志
adb logcat | grep -E "UnpackMeeting|SyncFileManager"

# 查看权限日志
adb logcat | grep -i "permission\|storage"
```

## 🔒 同步锁机制

### 工作原理

1. **传输开始**：创建 `/sdcard/Download/.sync_lock`
2. **App检测**：发现锁文件时跳过所有解包
3. **传输完成**：删除 `.sync_lock`
4. **App解包**：下次检查时批量解包

### Python传输脚本

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
        time.sleep(1)
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

## 🔐 加密与解密

### 密钥管理

- **统一密钥**: `package-encryption-key-32-chars!!`
- **算法**: AES-256-GCM
- **密钥派生**: `SHA256(masterKey + meetingID + salt)`

### 解密流程

1. 读取 `.zip.enc` 文件
2. 使用AES-GCM解密，得到ZIP数据
3. 解压ZIP，得到原始文件和manifest

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

## ⚠️ 注意事项

1. **权限**: 需要 `MANAGE_EXTERNAL_STORAGE` 权限
2. **锁机制**: `.sync_lock` 存在时，App跳过所有解包
3. **密钥**: 密钥必须与后台保持一致
4. **清理**: 解包失败后自动删除损坏的包
5. **线程**: 所有解包操作在IO线程执行
6. **事务**: 数据库操作使用事务保证一致性
7. **适用场景**: 仅用于内部部署，不可上架应用商店

## 📝 日志记录

使用Timber记录详细日志：

```kotlin
Timber.i("解包成功: meetingId=$meetingId, fileCount=$fileCount")
Timber.w("文件校验失败: fileId=$fileId")
Timber.e(exception, "解包异常: meetingId=$meetingId")
```

## 🚧 待完善功能

1. ❌ **完整会议信息**: manifest只包含基本信息
2. ❌ **参会人员数据**: 需实现 `parseParticipantsData` 方法
3. ❌ **议程数据**: 需实现 `parseAgendasData` 方法
4. ❌ **密码加密**: 参会人员密码加密存储
5. ❌ **进度回调**: 解包进度回调
6. ❌ **重试机制**: 解包失败重试策略

## 📚 相关文档

- [Room数据库官方文档](https://developer.android.com/training/data-storage/room)
- [Hilt依赖注入](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin协程](https://kotlinlang.org/docs/coroutines-overview.html)

---

**最后更新**: 2025年10月21日
