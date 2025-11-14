# SealMeet 开发规范

## 📌 项目概述

**SealMeet** 是一款用于涉密场所的离线会议平板应用，核心特性：
- 离线会议材料浏览
- 多会议管理与权限隔离
- Key认证器身份验证
- 会议材料加密存储
- 用户操作审计日志
- 动态主题换肤

**技术栈**
- Kotlin + Jetpack Compose
- MVI架构模式
- Hilt依赖注入
- Room数据库
- Coroutines + Flow

---

## 📁 目录结构详解

```
app/src/main/java/com/xunyidi/sealmeet/
│
├── core/                          # 核心基础模块（框架层）
│   └── mvi/                       # 自建MVI框架
│       ├── BaseViewModel.kt       # ViewModel基类，处理Intent→State转换
│       ├── UiState.kt            # 状态标记接口
│       ├── UiIntent.kt           # 意图标记接口
│       └── UiEffect.kt           # 副作用标记接口
│
├── data/                          # 数据层（Data Layer）
│   ├── local/                     # 本地数据源
│   │   ├── database/             # Room数据库
│   │   │   ├── AppDatabase.kt    # 数据库实例
│   │   │   ├── dao/              # 数据访问对象
│   │   │   │   ├── MeetingDao.kt
│   │   │   │   ├── UserDao.kt
│   │   │   │   └── AuditLogDao.kt
│   │   │   └── entity/           # 数据库实体
│   │   │       ├── MeetingEntity.kt
│   │   │       ├── UserEntity.kt
│   │   │       └── AuditLogEntity.kt
│   │   │
│   │   ├── datastore/            # DataStore（配置存储）
│   │   │   └── PreferencesManager.kt
│   │   │
│   │   └── file/                 # 文件操作
│   │       ├── FileManager.kt
│   │       └── MeetingFileParser.kt
│   │
│   ├── model/                     # 数据模型（领域对象）
│   │   ├── User.kt
│   │   ├── Meeting.kt
│   │   ├── Material.kt
│   │   └── AuditLog.kt
│   │
│   └── repository/                # 仓库层（数据聚合）
│       ├── MeetingRepository.kt
│       ├── UserRepository.kt
│       └── AuditRepository.kt
│
├── domain/                        # 业务逻辑层（Domain Layer）
│   └── usecase/                   # 用例（业务逻辑封装）
│       ├── meeting/
│       │   ├── GetMeetingListUseCase.kt
│       │   ├── LoadMeetingDetailUseCase.kt
│       │   └── SyncMeetingDataUseCase.kt
│       ├── auth/
│       │   ├── ValidateKeyUseCase.kt
│       │   └── GetCurrentUserUseCase.kt
│       └── audit/
│           └── LogMaterialAccessUseCase.kt
│
├── presentation/                  # 表示层（UI Layer）
│   ├── screen/                    # 页面
│   │   ├── splash/               # 启动页
│   │   │   ├── SplashScreen.kt
│   │   │   ├── SplashViewModel.kt
│   │   │   └── SplashContract.kt
│   │   │
│   │   ├── meetinglist/          # 会议列表页
│   │   │   ├── MeetingListScreen.kt
│   │   │   ├── MeetingListViewModel.kt
│   │   │   └── MeetingListContract.kt
│   │   │
│   │   ├── meetingdetail/        # 会议详情页
│   │   └── materialviewer/       # 材料查看器
│   │
│   ├── component/                 # 可复用UI组件
│   │   ├── LoadingDialog.kt
│   │   ├── ErrorDialog.kt
│   │   └── MeetingCard.kt
│   │
│   ├── navigation/                # 导航
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   │
│   └── theme/                     # 主题系统
│       ├── Color.kt              # 颜色定义
│       ├── ColorUtils.kt         # 颜色工具
│       ├── Type.kt               # 字体
│       ├── Theme.kt              # 主题入口
│       ├── ThemeConfig.kt        # 主题配置
│       ├── ThemeManager.kt       # 主题管理器
│       └── AppColors.kt          # 颜色扩展
│
├── di/                            # 依赖注入（Hilt Modules）
│   ├── AppModule.kt              # 应用级依赖
│   ├── DatabaseModule.kt         # 数据库依赖
│   ├── RepositoryModule.kt       # 仓库依赖
│   └── UseCaseModule.kt          # 用例依赖
│
├── util/                          # 工具类（可选）
│   ├── DateUtil.kt
│   ├── FileUtil.kt
│   └── SecurityUtil.kt
│
├── MainActivity.kt                # 主Activity
└── SealMeetApp.kt                # Application类
```

---

## 🏗️ MVI架构开发规范

### 1. Contract定义规范

每个Screen必须有一个Contract文件，集中定义State、Intent、Effect：

```kotlin
// 文件命名：{ScreenName}Contract.kt
object MeetingListContract {
    
    // 1. State：UI状态（必须是data class，保证不可变）
    data class State(
        val isLoading: Boolean = false,
        val meetings: List<Meeting> = emptyList(),
        val currentUser: User? = null,
        val errorMessage: String? = null
    ) : UiState
    
    // 2. Intent：用户意图（必须是sealed interface）
    sealed interface Intent : UiIntent {
        data object LoadMeetings : Intent
        data class SelectMeeting(val meetingId: String) : Intent
        data object RefreshData : Intent
    }
    
    // 3. Effect：副作用/一次性事件（必须是sealed interface）
    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class NavigateToDetail(val meetingId: String) : Effect
        data object NavigateBack : Effect
    }
}
```

**命名规范**
- State字段：使用`is/has`前缀表示布尔值，如`isLoading`、`hasError`
- Intent：使用动词开头，如`LoadData`、`DeleteItem`、`UpdateTitle`
- Effect：使用动词开头，描述动作，如`ShowToast`、`NavigateToDetail`

### 2. ViewModel开发规范

```kotlin
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val getMeetingListUseCase: GetMeetingListUseCase,
    private val auditRepository: AuditRepository
) : BaseViewModel<MeetingListContract.State, MeetingListContract.Intent, MeetingListContract.Effect>(
    initialState = MeetingListContract.State()
) {
    
    // 初始化数据加载
    init {
        handleIntent(MeetingListContract.Intent.LoadMeetings)
    }
    
    // 处理Intent
    override fun handleIntent(intent: MeetingListContract.Intent) {
        when (intent) {
            is MeetingListContract.Intent.LoadMeetings -> loadMeetings()
            is MeetingListContract.Intent.SelectMeeting -> selectMeeting(intent.meetingId)
            is MeetingListContract.Intent.RefreshData -> refreshData()
        }
    }
    
    // 私有方法：具体业务逻辑
    private fun loadMeetings() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                val meetings = getMeetingListUseCase()
                updateState { copy(isLoading = false, meetings = meetings) }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, errorMessage = e.message) }
                sendEffect(MeetingListContract.Effect.ShowToast("加载失败"))
            }
        }
    }
    
    private fun selectMeeting(meetingId: String) {
        viewModelScope.launch {
            // 记录审计日志
            auditRepository.logAccess(meetingId)
            // 发送导航事件
            sendEffect(MeetingListContract.Effect.NavigateToDetail(meetingId))
        }
    }
}
```

**要点**
- ✅ 使用`@HiltViewModel`注解
- ✅ 构造函数注入依赖
- ✅ 必须实现`handleIntent()`方法
- ✅ 使用`updateState { }`更新状态
- ✅ 使用`sendEffect()`发送副作用
- ✅ 业务逻辑在private方法中实现
- ❌ 不要在ViewModel中直接操作UI

### 3. Screen开发规范

```kotlin
@Composable
fun MeetingListScreen(
    viewModel: MeetingListViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    // 收集状态
    val state by viewModel.uiState.collectAsState()
    
    // 监听副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MeetingListContract.Effect.ShowToast -> {
                    // 显示Toast
                }
                is MeetingListContract.Effect.NavigateToDetail -> {
                    onNavigateToDetail(effect.meetingId)
                }
                is MeetingListContract.Effect.NavigateBack -> {
                    // 返回
                }
            }
        }
    }
    
    // UI内容
    MeetingListContent(
        state = state,
        onIntent = viewModel::handleIntent
    )
}

// 将UI内容分离为独立的Composable（便于预览）
@Composable
private fun MeetingListContent(
    state: MeetingListContract.State,
    onIntent: (MeetingListContract.Intent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(AppColors.bgPage)) {
        when {
            state.isLoading -> LoadingIndicator()
            state.meetings.isEmpty() -> EmptyView()
            else -> MeetingList(
                meetings = state.meetings,
                onMeetingClick = { meetingId ->
                    onIntent(MeetingListContract.Intent.SelectMeeting(meetingId))
                }
            )
        }
    }
}
```

**要点**
- ✅ 使用`hiltViewModel()`注入ViewModel
- ✅ 使用`collectAsState()`收集状态
- ✅ 使用`LaunchedEffect`监听副作用
- ✅ 将UI内容分离为独立Composable
- ✅ 通过`onIntent`回调发送Intent
- ❌ 不要在Screen中直接调用UseCase或Repository

---

## 🎨 主题系统使用规范

### 颜色使用规范

#### 1. 可配置颜色（随主题变化）

```kotlin
// ✅ 正确：使用AppColors
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = AppColors.primaryDefault,
        disabledContainerColor = AppColors.primaryDisabled
    )
)

// ✅ 正确：使用MaterialTheme
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
)
```

#### 2. 固定颜色（不随主题变化）

```kotlin
// ✅ 正确：功能语义色使用固定颜色
Text(
    text = "操作成功",
    color = AppColors.success  // 固定的绿色
)

Text(
    text = "发生错误",
    color = AppColors.error  // 固定的红色
)
```

#### 3. 严禁硬编码

```kotlin
// ❌ 错误：硬编码颜色
Box(modifier = Modifier.background(Color(0xFFBE0110)))
Text(text = "标题", color = Color(0xFF404040))

// ✅ 正确：使用主题颜色
Box(modifier = Modifier.background(AppColors.primaryDefault))
Text(text = "标题", color = AppColors.textPrimary)
```

### 主题切换

```kotlin
// 切换浅色/深色模式
ThemeManager.toggleTheme()

// 切换到指定主题
ThemeManager.switchTheme(ThemeType.DARK)

// 获取当前主题配置
val config = ThemeManager.getCurrentConfig()
```

---

## 📝 命名规范

### 1. 文件命名

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| Activity | `{Name}Activity.kt` | `MainActivity.kt` |
| ViewModel | `{Name}ViewModel.kt` | `MeetingListViewModel.kt` |
| Screen | `{Name}Screen.kt` | `MeetingListScreen.kt` |
| Contract | `{Name}Contract.kt` | `MeetingListContract.kt` |
| Repository | `{Name}Repository.kt` | `MeetingRepository.kt` |
| UseCase | `{Action}{Entity}UseCase.kt` | `GetMeetingListUseCase.kt` |
| Entity | `{Name}Entity.kt` | `MeetingEntity.kt` |
| Dao | `{Name}Dao.kt` | `MeetingDao.kt` |
| Component | `{Name}.kt` | `LoadingDialog.kt` |

### 2. 包命名

- 全小写
- 使用`.`分隔
- 不使用下划线或连字符

```kotlin
com.xunyidi.sealmeet.presentation.screen.meetinglist
com.xunyidi.sealmeet.data.repository
```

### 3. 类命名

- 大驼峰（PascalCase）
- 名词或名词短语
- 避免缩写

```kotlin
class MeetingListViewModel
class MeetingRepository
data class User
```

### 4. 函数命名

- 小驼峰（camelCase）
- 动词开头

```kotlin
fun loadMeetings()
fun getMeetingById(id: String)
fun updateState()
fun sendEffect()
```

### 5. 变量命名

- 小驼峰（camelCase）
- 布尔值使用`is/has/should`前缀

```kotlin
val meetingList: List<Meeting>
val isLoading: Boolean
val hasError: Boolean
val shouldRefresh: Boolean
```

### 6. 常量命名

- 全大写，下划线分隔

```kotlin
const val MAX_RETRY_COUNT = 3
const val DEFAULT_TIMEOUT = 5000L
```

---

## 🔧 代码规范

### 1. Kotlin编码规范

遵循 [Kotlin官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)

**要点**
- ✅ 使用4个空格缩进
- ✅ 每行最多120字符
- ✅ 优先使用`val`而不是`var`
- ✅ 使用表达式函数简化单行函数
- ✅ 使用`when`替代多个`if-else`
- ✅ 使用命名参数提高可读性

```kotlin
// ✅ 表达式函数
fun sum(a: Int, b: Int) = a + b

// ✅ when表达式
val message = when (status) {
    Status.SUCCESS -> "成功"
    Status.ERROR -> "失败"
    Status.LOADING -> "加载中"
}

// ✅ 命名参数
createUser(
    name = "张三",
    age = 30,
    role = "管理员"
)
```

### 2. Compose最佳实践

```kotlin
// ✅ 使用remember缓存计算结果
val filteredList = remember(meetings, query) {
    meetings.filter { it.title.contains(query) }
}

// ✅ 使用derivedStateOf避免不必要的重组
val isScrolledToTop by remember {
    derivedStateOf { scrollState.value == 0 }
}

// ✅ 使用LaunchedEffect处理副作用
LaunchedEffect(key1 = meetingId) {
    viewModel.loadMeetingDetail(meetingId)
}

// ✅ 使用Modifier.semantics提升无障碍性
Icon(
    imageVector = Icons.Default.Delete,
    contentDescription = "删除",
    modifier = Modifier.semantics { role = Role.Button }
)
```

### 3. 空安全

```kotlin
// ✅ 使用安全调用
val length = text?.length

// ✅ 使用Elvis操作符提供默认值
val name = user?.name ?: "未知用户"

// ✅ 使用let避免重复判空
user?.let { 
    updateUserInfo(it)
}

// ❌ 避免使用!!（除非绝对确定不为null）
val name = user!!.name  // 不推荐
```

### 4. 协程使用

```kotlin
// ✅ 在ViewModel中使用viewModelScope
viewModelScope.launch {
    val data = repository.getData()
    updateState { copy(data = data) }
}

// ✅ 使用withContext切换调度器
suspend fun loadFile(): String = withContext(Dispatchers.IO) {
    File("path").readText()
}

// ✅ 处理异常
viewModelScope.launch {
    try {
        val data = repository.getData()
    } catch (e: Exception) {
        Timber.e(e, "加载失败")
        sendEffect(ShowError(e.message))
    }
}
```

---

## 📦 依赖注入规范

### Hilt 依赖注入最佳实践

#### 1. 优先使用 @Inject 构造函数

对于自己编写的类，优先使用 `@Inject` 构造函数，Hilt 会自动提供依赖：

```kotlin
// ✅ 推荐：使用 @Inject 构造函数
@Singleton
class SyncFileManager @Inject constructor(
    private val appPreferences: AppPreferences
) {
    // ...
}

// ✅ 推荐：ViewModel 使用 @HiltViewModel
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val getMeetingListUseCase: GetMeetingListUseCase
) : BaseViewModel<...>(...) {
    // ...
}
```

**关键点**：
- 使用 `@Inject` 构造函数后，**不需要**在 Module 中使用 `@Provides` 方法
- Hilt 会自动识别并提供这些类的实例
- `@Singleton` 注解确保单例模式

#### 2. 使用 @Provides 方法的场景

只在以下情况使用 `@Provides` 方法：

**场景 1：提供接口实现**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideMeetingRepository(
        meetingDao: MeetingDao,
        fileManager: FileManager
    ): MeetingRepository {  // 返回接口类型
        return MeetingRepositoryImpl(meetingDao, fileManager)  // 返回具体实现
    }
}
```

**场景 2：提供第三方库的类**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sealmeet_database"
        ).build()
    }
}
```

**场景 3：需要复杂的构造逻辑**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor())
            .build()
    }
}
```

#### 3. Module 定义规范

```kotlin
// ✅ 正确：只提供必要的依赖
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    // 如果所有类都使用 @Inject 构造函数，Module 可以为空
    // 或者只提供接口、第三方库等无法使用 @Inject 的依赖
}

// ❌ 错误：重复提供已有 @Inject 的类
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    
    @Provides
    @Singleton
    fun provideSyncFileManager(...): SyncFileManager {  // 不需要！
        return SyncFileManager(...)  // SyncFileManager 已经有 @Inject
    }
}
```

#### 4. 作用域选择

- `@Singleton`：应用级单例，整个应用只有一个实例
- `@ActivityScoped`：Activity 级作用域
- `@ViewModelScoped`：ViewModel 级作用域

```kotlin
// 应用级单例
@Singleton
class AppPreferences @Inject constructor(...) { }

// ViewModel 级作用域
@HiltViewModel
class MeetingListViewModel @Inject constructor(...) : BaseViewModel<...>(...)
```

### ViewModel注入

```kotlin
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val getMeetingListUseCase: GetMeetingListUseCase
) : BaseViewModel<...>(...)
```

---

## 🐛 日志规范

使用Timber记录日志：

```kotlin
// Debug日志
Timber.d("加载会议列表")

// Info日志
Timber.i("用户登录成功: $userId")

// Warning日志
Timber.w("缓存过期，重新加载")

// Error日志
Timber.e(exception, "加载会议失败")

// 带标签的日志
Timber.tag("MeetingList").d("刷新数据")
```

**要点**
- ✅ 生产环境自动关闭Debug日志
- ✅ Error日志必须包含异常对象
- ✅ 不要记录敏感信息（密码、密钥等）
- ❌ 不要使用`println()`或`Log`

---

## 🚫 禁止事项

### 1. 架构相关
- ❌ 不要在UI层直接调用Repository
- ❌ 不要在ViewModel中直接操作UI
- ❌ 不要在Contract外部定义State/Intent/Effect
- ❌ 不要跳过UseCase直接在ViewModel中写复杂业务逻辑

### 2. 主题相关
- ❌ 不要硬编码颜色值
- ❌ 不要在业务代码中直接使用`Color(0xFF...)`
- ❌ 固定语义色不要使用可配置颜色

### 3. 代码质量
- ❌ 不要使用`!!`（非空断言）
- ❌ 不要捕获异常后不处理（空catch块）
- ❌ 不要在循环中创建对象（性能问题）
- ❌ 不要在Composable中执行耗时操作

---

## ✅ 开发检查清单

### 新增Screen时

- [ ] 创建Contract文件（State/Intent/Effect）
- [ ] 创建ViewModel（继承BaseViewModel）
- [ ] 创建Screen Composable
- [ ] 添加导航路由
- [ ] 使用主题颜色（不要硬编码）
- [ ] 处理加载状态和错误状态
- [ ] 添加审计日志（如需要）

### 代码提交前

- [ ] 代码格式化（Android Studio: Ctrl+Alt+L）
- [ ] 移除无用的import
- [ ] 移除注释掉的代码
- [ ] 检查是否有硬编码的颜色
- [ ] 检查是否有`!!`使用
- [ ] 运行编译确保无错误

---

## 📚 参考资料

- [Kotlin官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- [Jetpack Compose文档](https://developer.android.com/jetpack/compose)
- [Android架构指南](https://developer.android.com/topic/architecture)
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 项目架构详细说明
- [README.md](./README.md) - 项目说明

---

**最后更新**: 2025年10月
