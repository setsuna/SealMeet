# SealMeet - 离线会议平板应用

## 项目简介

SealMeet 是一款用于涉密场所的离线会议平板应用，支持会议材料离线浏览、多会议管理、用户权限控制、操作审计等功能。

## 技术栈

- **架构模式**: MVI（Model-View-Intent）
- **UI框架**: Jetpack Compose + Material3
- **依赖注入**: Hilt
- **数据库**: Room
- **异步处理**: Kotlin Coroutines + Flow
- **主题系统**: 动态换肤支持

## 项目结构

详见 [ARCHITECTURE.md](ARCHITECTURE.md)

## 当前进度

✅ **已完成**
- [x] MVI基础框架搭建
- [x] 主题系统（按照换肤规范实现）
- [x] 依赖注入配置
- [x] 项目基础目录结构
- [x] 示例代码（SplashScreen）

⏳ **待完成**
- [ ] 配色方案应用（等待提供）
- [ ] Room数据库设计
- [ ] 具体业务页面
- [ ] 文件管理模块
- [ ] 加密模块
- [ ] 审计日志模块
- [ ] Key认证器集成

## 如何提供配色方案

请按照以下格式提供您的配色方案：

### 1. 主品牌色（必填）

```
浅色模式：
- 主色-默认态: #BE0110
- 主色-悬停态: #9C0D0E
- 主色-点击态: #7A0A0B
- 主色-禁用态: #E6989D

深色模式：
- 主色-默认态: #FF4D5A
- 主色-悬停态: #FF6B75
- 主色-点击态: #FF8A93
- 主色-禁用态: #5A2A2D
```

### 2. 辅助品牌色（可选）

```
浅色模式：
- 辅助色-默认态: #1890FF
- 辅助色-悬停态: #0B7DD6
- 辅助色-点击态: #0960A8

深色模式：
- 辅助色-默认态: #3AA0FF
- 辅助色-悬停态: #5BB0FF
- 辅助色-点击态: #7CC0FF
```

### 3. 背景色（可选，不提供则使用默认）

```
浅色模式：
- 页面主背景: #FAFAFA
- 容器次级背景: #EFF2F5
- 卡片背景: #FFFFFF
- 悬浮层背景: #FFFFFF

深色模式：
- 页面主背景: #121212
- 容器次级背景: #1E1E1E
- 卡片背景: #2C2C2C
- 悬浮层背景: #383838
```

### 注意事项

- **颜色格式**: 请使用十六进制格式（如 #BE0110）
- **对比度**: 请确保颜色对比度符合可读性要求
- **固定颜色**: 功能语义色（成功/警告/错误/信息）和文字颜色已预设，无需提供
- **自动计算**: 遮罩层、浅色背景等衍生颜色会自动计算，无需提供

## 编译运行

```bash
# 同步Gradle依赖
./gradlew build

# 运行应用
./gradlew installDebug
```

## 开发规范

1. **不要硬编码颜色**: 使用 `AppColors` 或 `MaterialTheme.colorScheme`
2. **遵循MVI架构**: 每个Screen定义Contract（State/Intent/Effect）
3. **使用Hilt注入**: ViewModel、Repository等使用依赖注入
4. **Compose最佳实践**: 使用Modifier、remember、LaunchedEffect等

详见 [ARCHITECTURE.md](ARCHITECTURE.md)

## 联系方式

如有问题，请联系开发团队。
