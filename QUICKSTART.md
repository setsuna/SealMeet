# SealMeet 项目快速参考

> 这是一份精简版项目指南，帮助新会话快速了解项目。详细规范请查看 [DEVELOPMENT.md](./DEVELOPMENT.md)

---

## 🎯 项目简介

**SealMeet** - 涉密场所离线会议平板应用

**核心功能**
- 离线会议材料浏览
- Key认证器身份验证
- 多会议权限隔离
- 材料加密存储
- 操作审计日志
- 动态主题换肤

**技术栈**: Kotlin + Compose + MVI + Hilt + Room

---

## 📂 核心目录结构

```
app/src/main/java/com/xunyidi/sealmeet/
├── core/mvi/              # MVI框架（BaseViewModel、UiState、UiIntent、UiEffect）
├── data/                  # 数据层（model、repository、local）
├── domain/                # 业务层（usecase）
├── presentation/          # UI层（screen、component、theme）
├── di/                    # 依赖注入
└── MainActivity.kt        # 主入口
```

---

## 🏗️ MVI架构速查

### 1. 每个Screen必须有Contract

```kotlin
object MyScreenContract {
    data class State(...) : UiState           // UI状态
    sealed interface Intent : UiIntent {...}  // 用户意图
    sealed interface Effect : UiEffect {...}  // 副作用
}
```

### 2. ViewModel模板

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val useCase: MyUseCase
) : BaseViewModel<State, Intent, Effect>(initialState = State()) {
    
    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.Action -> doAction()
        }
    }
    
    private fun doAction() {
        viewModelScope.launch {
            updateState { copy(loading = true) }
            // 业务逻辑
            sendEffect(Effect.ShowToast("成功"))
        }
    }
}
```

### 3. Screen模板

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Effect.ShowToast -> { /* 处理 */ }
            }
        }
    }
    
    MyScreenContent(state, onIntent = viewModel::handleIntent)
}
```

---

## 🎨 主题系统速查

### 颜色使用原则

**可配置颜色**（随主题变化）
```kotlin
AppColors.primaryDefault     // 主色
AppColors.bgPage            // 背景色
AppColors.textPrimary       // 文字色
```

**固定颜色**（不变）
```kotlin
AppColors.success  // 成功-绿
AppColors.warning  // 警告-黄
AppColors.error    // 错误-红
AppColors.info     // 信息-蓝
```

### 切换主题

```kotlin
ThemeManager.toggleTheme()           // 切换浅色/深色
ThemeManager.switchToDarkMode()      // 切换到深色
ThemeManager.switchToLightMode()     // 切换到浅色
```

### ⚠️ 严禁硬编码颜色

```kotlin
// ❌ 错误
Box(modifier = Modifier.background(Color(0xFFBE0110)))

// ✅ 正确
Box(modifier = Modifier.background(AppColors.primaryDefault))
```

---

## 📝 命名速查

| 类型 | 格式 | 示例 |
|------|------|------|
| Screen | `{Name}Screen.kt` | `MeetingListScreen.kt` |
| ViewModel | `{Name}ViewModel.kt` | `MeetingListViewModel.kt` |
| Contract | `{Name}Contract.kt` | `MeetingListContract.kt` |
| Repository | `{Name}Repository.kt` | `MeetingRepository.kt` |
| UseCase | `{Action}{Entity}UseCase.kt` | `GetMeetingListUseCase.kt` |
| Component | `{Name}.kt` | `LoadingDialog.kt` |

---

## 🚀 开发流程

### 新增Screen的标准步骤

1. **创建Contract** - 定义State/Intent/Effect
2. **创建ViewModel** - 继承BaseViewModel，实现handleIntent
3. **创建Screen** - Composable UI
4. **添加导航** - 在NavGraph中注册
5. **使用主题颜色** - 不要硬编码
6. **添加审计日志** - 记录用户操作（如需要）

### 代码检查

- [ ] 使用`AppColors`而非硬编码颜色
- [ ] ViewModel使用`@HiltViewModel`
- [ ] State是`data class`且字段不可变
- [ ] Intent和Effect是`sealed interface`
- [ ] 没有使用`!!`（非空断言）
- [ ] 使用Timber而非println

---

## 🔧 常用依赖

```kotlin
// ViewModel
androidx.lifecycle.viewModelScope

// State收集
collectAsState()

// 副作用
LaunchedEffect(key) { }

// 日志
Timber.d() / Timber.e()

// 协程
withContext(Dispatchers.IO) { }
```

---

## ⚠️ 核心禁止事项

1. ❌ 不要硬编码颜色
2. ❌ 不要在UI层直接调用Repository
3. ❌ 不要在ViewModel中操作UI
4. ❌ 不要使用`!!`
5. ❌ 不要跳过Contract定义State/Intent/Effect

---

## 📚 详细文档

- [DEVELOPMENT.md](./DEVELOPMENT.md) - 完整开发规范
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 架构详细说明
- [README.md](./README.md) - 项目说明

---

## 💡 关键点记忆

**架构**: MVI = State + Intent + Effect
**颜色**: 用`AppColors`，不要硬编码
**数据流**: Intent → ViewModel → State → UI
**副作用**: Effect（Toast、导航等一次性事件）
**依赖注入**: 用Hilt，ViewModel用`@HiltViewModel`

---

**快速上手**: 参考 `presentation/screen/splash/` 目录的示例代码
