# SealMeet - 离线会议平板应用

## 📌 项目简介

SealMeet 是一款用于涉密场所的离线会议平板应用，支持会议材料离线浏览、多会议管理、用户权限控制、操作审计等功能。

## 🚀 快速开始

### 新会话快速了解项目
- 📖 **[QUICKSTART.md](./QUICKSTART.md)** - 5分钟快速上手指南
- 📋 **[DEVELOPMENT.md](./DEVELOPMENT.md)** - 完整开发规范
- 🏗️ **[ARCHITECTURE.md](./ARCHITECTURE.md)** - 架构详细说明

### 编译运行

```bash
# 克隆项目
git clone <repository-url>

# 同步依赖
./gradlew build

# 运行应用
./gradlew installDebug
```

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material3
- **架构模式**: MVI (Model-View-Intent)
- **依赖注入**: Hilt
- **数据库**: Room
- **异步处理**: Coroutines + Flow
- **主题系统**: 动态换肤

## 📋 核心功能

- ✅ 离线会议材料浏览
- ✅ Key认证器身份验证
- ✅ 多会议权限隔离
- ✅ 会议材料加密存储
- ✅ 用户操作审计日志
- ✅ 动态主题换肤（浅色/深色模式）

## 📂 项目结构

```
app/src/main/java/com/xunyidi/sealmeet/
├── core/mvi/              # 自建MVI框架
├── data/                  # 数据层（Repository、Database、Model）
├── domain/                # 业务逻辑层（UseCase）
├── presentation/          # UI层（Screen、ViewModel、Theme）
├── di/                    # 依赖注入模块
└── MainActivity.kt        # 应用入口
```

详细目录说明请查看 [DEVELOPMENT.md](./DEVELOPMENT.md)

## 🎨 当前进度

### ✅ 已完成

- [x] MVI基础框架搭建
- [x] 主题系统（按换肤规范实现）
- [x] 依赖注入配置（Hilt）
- [x] 项目基础目录结构
- [x] 示例代码（SplashScreen）
- [x] 开发规范文档

### ⏳ 待开发

- [ ] Room数据库实体定义
- [ ] Repository层实现
- [ ] 会议列表页面
- [ ] 会议详情页面
- [ ] 文件管理模块
- [ ] 加密模块
- [ ] 审计日志模块
- [ ] Key认证器集成
- [ ] 材料预览功能（永中Office SDK）

## 📖 开发指南

### MVI架构核心

每个Screen需要定义：
1. **Contract** - 集中定义State、Intent、Effect
2. **ViewModel** - 继承BaseViewModel，处理业务逻辑
3. **Screen** - Composable UI，响应State变化

示例：
```kotlin
// 1. Contract
object MyScreenContract {
    data class State(...) : UiState
    sealed interface Intent : UiIntent { ... }
    sealed interface Effect : UiEffect { ... }
}

// 2. ViewModel
@HiltViewModel
class MyViewModel @Inject constructor(...) 
    : BaseViewModel<State, Intent, Effect>(...) {
    override fun handleIntent(intent: Intent) { ... }
}

// 3. Screen
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    // UI内容
}
```

### 主题系统使用

```kotlin
// 使用可配置颜色（随主题变化）
Box(modifier = Modifier.background(AppColors.primaryDefault))
Text(text = "标题", color = AppColors.textPrimary)

// 使用固定颜色（不随主题变化）
Text(text = "成功", color = AppColors.success)

// 切换主题
ThemeManager.toggleTheme()
```

⚠️ **严禁硬编码颜色值！**

详细规范请查看 [DEVELOPMENT.md](./DEVELOPMENT.md)

## 🔐 涉密相关

### Key认证器
- 平板通过USB Key认证用户身份
- 一个平板对应一个Key
- Key拔出后清空内存中的敏感数据

### 会议隔离
- 不同会议材料严格隔离
- 基于用户权限过滤可见会议
- 会议材料独立加密存储

### 审计日志
- 记录所有材料访问操作
- 包含用户ID、会议ID、操作时间
- 充电时加密回传服务器

## 🎯 开发规范要点

### 必须遵守
- ✅ 使用MVI架构（Contract + ViewModel + Screen）
- ✅ 使用`AppColors`或`MaterialTheme.colorScheme`获取颜色
- ✅ ViewModel使用`@HiltViewModel`注解
- ✅ State必须是不可变的`data class`
- ✅ 使用Timber记录日志

### 严禁事项
- ❌ 硬编码颜色值
- ❌ 在UI层直接调用Repository
- ❌ 使用`!!`（非空断言）
- ❌ 空catch块（捕获异常不处理）
- ❌ 在ViewModel中直接操作UI

完整规范请查看 [DEVELOPMENT.md](./DEVELOPMENT.md)

## 📚 文档索引

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| [QUICKSTART.md](./QUICKSTART.md) | 快速参考指南 | 新会话快速上手 |
| [DEVELOPMENT.md](./DEVELOPMENT.md) | 完整开发规范 | 详细开发指导 |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 架构详细说明 | 理解项目架构 |

## 🤝 配色方案

当前使用默认配色：
- **浅色模式**: 红色系主色 + 蓝色系辅助色
- **深色模式**: 高亮红色系主色 + 高亮蓝色系辅助色

如需自定义配色，请在 `presentation/theme/Color.kt` 中修改颜色定义。

## 📞 联系方式

如有问题，请联系开发团队。

---

**最后更新**: 2025年10月21日
