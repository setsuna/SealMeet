# SealMeet - 架构说明

## 项目结构

```
app/src/main/java/com/xunyidi/sealmeet/
├── core/                          # 核心基础模块
│   └── mvi/                       # 自建MVI框架
│       ├── BaseViewModel.kt       # ViewModel基类
│       ├── UiState.kt            # State接口
│       ├── UiIntent.kt           # Intent接口
│       └── UiEffect.kt           # Effect接口
│
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   └── database/             # Room数据库
│   │       ├── dao/
│   │       └── entity/
│   ├── model/                     # 数据模型
│   │   ├── User.kt
│   │   └── Meeting.kt
│   └── repository/                # 仓库层
│
├── presentation/                  # UI层
│   ├── screen/                    # 页面
│   │   └── splash/               # 启动页示例
│   │       └── SplashContract.kt # Contract示例
│   └── theme/                     # 主题系统
│       ├── Color.kt              # 颜色定义（可配置+固定）
│       ├── ColorUtils.kt         # 颜色计算工具
│       ├── Type.kt               # 字体定义
│       ├── Theme.kt              # Compose主题入口
│       ├── ThemeConfig.kt        # 主题配置数据类
│       ├── ThemeManager.kt       # 主题管理器
│       └── AppColors.kt          # 颜色扩展属性
│
├── di/                            # 依赖注入
│   └── AppModule.kt
│
├── MainActivity.kt
└── SealMeetApp.kt                # Application类
```

---

## MVI架构使用说明

### 1. 定义Contract

每个Screen创建一个Contract文件，集中定义State、Intent、Effect：

```kotlin
object MyScreenContract {
    data class State(
        val isLoading: Boolean = false,
        val data: List<Item> = emptyList()
    ) : UiState
    
    sealed interface Intent : UiIntent {
        data object LoadData : Intent
        data class DeleteItem(val id: String) : Intent
    }
    
    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateBack : Effect
    }
}
```

### 2. 创建ViewModel

继承BaseViewModel并实现handleIntent：

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : BaseViewModel<MyScreenContract.State, MyScreenContract.Intent, MyScreenContract.Effect>(
    initialState = MyScreenContract.State()
) {
    
    override fun handleIntent(intent: MyScreenContract.Intent) {
        when (intent) {
            is MyScreenContract.Intent.LoadData -> loadData()
            is MyScreenContract.Intent.DeleteItem -> deleteItem(intent.id)
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val data = repository.getData()
            updateState { copy(isLoading = false, data = data) }
        }
    }
    
    private fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(id)
            sendEffect(MyScreenContract.Effect.ShowToast("删除成功"))
        }
    }
}
```

### 3. 创建Composable Screen

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    
    // 监听副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MyScreenContract.Effect.ShowToast -> {
                    // 显示Toast
                }
                is MyScreenContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }
    
    // UI内容
    if (state.isLoading) {
        LoadingIndicator()
    } else {
        ItemList(
            items = state.data,
            onItemClick = { viewModel.handleIntent(MyScreenContract.Intent.DeleteItem(it)) }
        )
    }
}
```

---

## 主题系统使用说明

### 颜色分类

根据换肤规范，颜色分为三类：

#### 1. 可配置颜色（随主题变化）
- **主品牌色**：default、hover、active、disabled 四种状态
- **辅助品牌色**：default、hover、active 三种状态
- **背景色**：page、container、card、elevated 四种场景

#### 2. 固定颜色（不随主题变化）
- **功能语义色**：success（绿）、warning（黄）、error（红）、info（蓝）
- **文字颜色**：primary、secondary、regular、tertiary、inverse
- **边框与分割线**：border、divider

#### 3. 自动计算颜色
- 基于主色自动生成：overlay（遮罩）、lightBg（浅色背景）、borderActive（高亮边框）、ripple（涟漪）

### 使用方式

#### 方式一：使用 AppColors（推荐）

```kotlin
@Composable
fun MyButton() {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.primaryDefault,
            disabledContainerColor = AppColors.primaryDisabled
        ),
        onClick = { }
    ) {
        Text("点击", color = TextInverse)
    }
}
```

#### 方式二：使用 MaterialTheme

```kotlin
@Composable
fun MyCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = "标题",
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

#### 方式三：直接使用固定颜色常量

```kotlin
@Composable
fun SuccessMessage() {
    Text(
        text = "操作成功",
        color = AppColors.success  // 固定的成功色，不随主题变化
    )
}
```

### 主题切换

```kotlin
// 切换到深色模式
ThemeManager.switchToDarkMode()

// 切换到浅色模式
ThemeManager.switchToLightMode()

// 切换主题（浅色⇄深色）
ThemeManager.toggleTheme()

// 应用自定义主题
val customConfig = ThemeConfig(
    themeName = "蓝色主题",
    themeId = "blue",
    colors = ThemeColors(...)
)
ThemeManager.applyCustomTheme(customConfig)
```

### 监听主题变化

```kotlin
@Composable
fun MyScreen() {
    val themeConfig by ThemeManager.currentConfig.collectAsState()
    
    // UI会自动响应主题变化
    Text(
        text = "当前主题：${themeConfig.themeName}",
        color = AppColors.textPrimary
    )
}
```

### 颜色工具类

```kotlin
// 生成半透明遮罩
val overlayColor = ColorUtils.getOverlayColor(AppColors.primaryDefault)

// 生成浅色背景（用于选中态）
val lightBg = ColorUtils.getLightBgColor(AppColors.primaryDefault)

// 根据背景色自动选择文字颜色
val textColor = ColorUtils.getContrastTextColor(backgroundColor)

// 颜色变暗/变亮
val darkerColor = ColorUtils.darken(AppColors.primaryDefault, 0.2f)
val lighterColor = ColorUtils.lighten(AppColors.primaryDefault, 0.2f)
```

---

## 依赖注入

项目使用Hilt进行依赖注入。

### 在ViewModel中注入

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : BaseViewModel<...>(...) { }
```

### 在Activity中

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() { }
```

### 定义Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(...)
    }
}
```

---

## 开发注意事项

### 1. 不要硬编码颜色

```kotlin
// ❌ 错误
Box(modifier = Modifier.background(Color(0xFFBE0110)))

// ✅ 正确
Box(modifier = Modifier.background(AppColors.primaryDefault))
```

### 2. 使用正确的颜色类型

- **可配置颜色**：使用 `AppColors.primaryDefault` 等
- **固定颜色**：使用 `AppColors.success`、`TextPrimary` 等
- **自动计算**：使用 `ColorUtils` 工具类

### 3. 响应主题变化

所有使用 `AppColors` 的Composable函数会自动响应主题变化，无需手动刷新。

### 4. 状态色的使用

对于按钮、输入框等有交互状态的组件，使用对应的状态色：

```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = AppColors.primaryDefault,        // 默认态
        disabledContainerColor = AppColors.primaryDisabled // 禁用态
    )
)
```

---

## 待完成

- [ ] 用户提供的具体配色方案
- [ ] 主题持久化（DataStore）
- [ ] Room数据库实体定义
- [ ] Repository层实现
- [ ] 具体业务页面
- [ ] 文件管理模块
- [ ] 加密模块
- [ ] 审计日志模块
