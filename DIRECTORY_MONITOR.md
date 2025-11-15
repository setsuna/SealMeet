# 目录监控功能使用说明

## 功能概述

自动监控同步目录（Download 或 `/data/userdata/meetings`），当检测到新的会议包文件（`.zip.enc`）时，自动触发解包任务。

## 核心组件

### 1. SyncDirectoryObserver
- 基于 Android 原生 `FileObserver` 实现
- 监控文件事件：CREATE、MOVED_TO、CLOSE_WRITE
- 自动过滤非会议包文件和锁文件

### 2. DirectoryMonitorManager
- 管理监控器生命周期
- 实现防抖机制（2秒延迟）
- 文件稳定性检查（避免解包未完成的文件）
- 检查同步锁（避免在同步时解包）

### 3. SealMeetApp
- 在 Application 启动时自动开启监控
- 监控回调触发解包任务
- 在 Application 终止时清理资源

## 工作流程

```
1. App 启动
   ↓
2. SealMeetApp.onCreate()
   ↓
3. 启动目录监控
   ↓
4. FileObserver 检测到 .zip.enc 文件
   ↓
5. 防抖延迟 2 秒
   ↓
6. 检查文件稳定性（大小不变）
   ↓
7. 检查是否有同步锁
   ↓
8. 触发 unpackAllPendingPackages()
   ↓
9. 解包完成
```

## 监控的事件类型

| 事件 | 说明 | 使用场景 |
|------|------|---------|
| CREATE | 文件创建 | 直接在目录中创建文件 |
| MOVED_TO | 文件移入 | 从其他位置移动/复制到监控目录 |
| CLOSE_WRITE | 文件写入完成 | 文件写入完成后关闭 |

## 防抖机制

### 为什么需要防抖？
- 文件写入可能触发多个事件
- 大文件可能需要时间完成写入
- 避免解包不完整的文件

### 防抖策略
1. **延迟触发**：检测到文件后等待 2 秒
2. **文件稳定性检查**：间隔 0.5 秒检查文件大小是否变化
3. **重复事件取消**：同一文件的后续事件会取消之前的任务

## 同步锁检测

当检测到 `.sync_lock` 文件时：
- 跳过解包任务
- 避免在同步过程中解包
- 确保数据完整性

## 日志示例

### 启动监控
```
I/SealMeetApp: ========== 启动目录监控 ==========
I/SyncDirectoryObserver: 📂 初始化目录监控器
I/SyncDirectoryObserver:    监控目录: /storage/emulated/0/Download
I/SyncDirectoryObserver:    监控事件: CREATE | MOVED_TO | CLOSE_WRITE
I/SyncDirectoryObserver: 🚀 目录监控已启动
I/SealMeetApp: ========== 目录监控启动完成 ==========
```

### 检测到文件
```
D/SyncDirectoryObserver: 📝 目录事件: MOVED_TO -> meeting_abc123.zip.enc
I/SyncDirectoryObserver: ✅ 检测到会议包文件: meeting_abc123.zip.enc
I/SyncDirectoryObserver:    大小: 1048576 bytes
I/DirectoryMonitorManager: 🔍 检测到文件变化: meeting_abc123.zip.enc
D/DirectoryMonitorManager: ⏱️  等待文件稳定: meeting_abc123.zip.enc
```

### 触发解包
```
I/DirectoryMonitorManager: ✅ 文件稳定，触发解包: meeting_abc123.zip.enc
I/SealMeetApp: ========== 触发自动解包 ==========
I/UnpackMeetingUseCase: ✅ 解包成功: abc123, 文件数: 10
I/SealMeetApp: ========== 自动解包完成，共处理 1 个会议包 ==========
```

### 检测到同步锁
```
I/DirectoryMonitorManager: 🔒 检测到同步锁，跳过解包
```

## 性能优化

### 资源占用
- FileObserver 基于 inotify，系统级支持，资源占用极低
- 只监控目标目录，不递归监控子目录
- 使用协程处理，不阻塞主线程

### 内存管理
- 使用 SupervisorJob 确保单个任务失败不影响整体
- 防抖任务可以被取消，避免内存泄漏
- Application 终止时自动清理资源

## 测试方法

### 开发者模式测试
1. 确保开启开发者模式（使用 Download 目录）
2. 将测试文件复制到 Download 目录
   ```bash
   adb push meeting_test001.zip.enc /sdcard/Download/
   ```
3. 查看 Logcat 日志，确认监控器检测到文件
4. 等待 2 秒后，应该触发解包

### 生产模式测试
1. 关闭开发者模式
2. 使用 root 权限将文件复制到 `/data/userdata/meetings`
   ```bash
   adb shell
   su
   cp /sdcard/Download/meeting_test001.zip.enc /data/userdata/meetings/
   ```
3. 查看日志确认

## 故障排查

### 问题：监控未启动
- **检查**：查看日志是否有 "启动目录监控" 消息
- **原因**：可能是同步目录不存在或无权限
- **解决**：确保目录存在且有读写权限

### 问题：检测不到文件
- **检查**：确认文件扩展名是 `.zip.enc`
- **检查**：确认文件在监控目录的根目录（不在子目录）
- **检查**：查看 Logcat 是否有 "目录事件" 日志

### 问题：解包未触发
- **检查**：是否有 `.sync_lock` 文件
- **检查**：文件是否稳定（大小不再变化）
- **检查**：是否有异常日志

### 问题：重复解包
- **检查**：防抖机制是否正常工作
- **检查**：是否有多个监控器实例

## 注意事项

1. **权限要求**
   - 需要存储权限（在 MainActivity 中已处理）
   - 生产模式需要 root 权限访问 `/data/userdata/meetings`

2. **文件命名规范**
   - 必须以 `.zip.enc` 结尾
   - 推荐格式：`meeting_{meetingId}.zip.enc`

3. **同步锁机制**
   - 创建 `.sync_lock` 文件可以暂停解包
   - 删除锁文件后恢复监控

4. **生命周期**
   - 监控在 Application 启动时开启
   - 在 Application 终止时关闭
   - 不依赖 Activity 生命周期

## 扩展性

### 添加新的监控事件
在 `SyncDirectoryObserver.kt` 中修改 `EVENTS` 常量：
```kotlin
private const val EVENTS = CREATE or MOVED_TO or CLOSE_WRITE or DELETE
```

### 调整防抖延迟
在 `DirectoryMonitorManager.kt` 中修改 `DEBOUNCE_DELAY`：
```kotlin
private const val DEBOUNCE_DELAY = 3000L  // 改为 3 秒
```

### 添加更多文件类型
修改 `PACKAGE_EXTENSION`：
```kotlin
private val SUPPORTED_EXTENSIONS = listOf(".zip.enc", ".tar.enc")
```

## 参考资料

- [Android FileObserver 官方文档](https://developer.android.com/reference/android/os/FileObserver)
- [Kotlin Coroutines 防抖示例](https://kotlinlang.org/docs/coroutines-guide.html)
- SealMeet 开发规范：`DEVELOPMENT.md`
