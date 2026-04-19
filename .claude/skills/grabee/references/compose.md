# Compose Conventions — Grabee

All patterns verified against actual feature screens.

## GrabeeTheme

Located in `core/ui/src/commonMain/.../theme/Theme.kt`. Uses **Material3 Expressive** with `kolor` dynamic color:

```kotlin
@Composable
fun GrabeeTheme(
    appSetting: AppSetting = AppSetting.DEFAULT,
    appConfig: AppConfig = koinInject(),
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberColorScheme(
        useDynamicColor = appSetting.useDynamicColor,
        seedColor = appSetting.seedColor,
        isDark = shouldUseDarkTheme(appSetting.theme),
    )
    CompositionLocalProvider(
        LocalAppSetting provides appSetting,
        LocalAppConfig provides appConfig,
    ) {
        MaterialExpressiveTheme(colorScheme = colorScheme) {
            Surface(color = MaterialTheme.colorScheme.surface, content = content)
        }
    }
}
```

## CompositionLocals

Provided by `GrabeeTheme` — access anywhere in the tree:

```kotlin
val appSetting = LocalAppSetting.current    // AppSetting (theme, seedColor, plusMode…)
val appConfig = LocalAppConfig.current      // AppConfig (AdMob IDs, API keys, version)
val navBackStack = LocalNavBackStack.current  // MutableList<NavKey> for navigation
```

## Screen Pattern

Screens get ViewModel via `koinViewModel()` and collect state, then delegate to stateless content composables:

```kotlin
// Pattern from SettingScreen.kt
@Composable
internal fun SettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val navBackStack = LocalNavBackStack.current
    val setting by viewModel.setting.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier, ...) {
        LazyColumn {
            item {
                SettingThemeSection(
                    setting = setting,
                    onThemeChanged = viewModel::setTheme,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
```

## AsyncLoadContents

For screens with `ScreenState<T>`. Pattern from `PaywallScreen.kt`:

```kotlin
AsyncLoadContents(
    modifier = modifier,
    screenState = screenState,
    retryAction = viewModel::fetch,
) { state ->
    PaywallContent(
        products = state.products,
        selectedPlan = selectedPlan,
        purchaseState = purchaseState,
        onPurchaseClicked = viewModel::purchase,
        onBackClicked = { navBackStack.removeLastOrNull() },
        modifier = Modifier.fillMaxSize(),
    )
}
```

Use `LaunchedEffect` to react to one-time action states (e.g., navigate on success, show snackbar on error):

```kotlin
LaunchedEffect(purchaseState) {
    when (purchaseState) {
        is PurchaseUiState.Success -> navBackStack.removeLastOrNull()
        is PurchaseUiState.PurchaseFailed -> snackbarHostState.showSnackbar(message)
        else -> {}
    }
}
```

## Navigation from Composables

```kotlin
val navBackStack = LocalNavBackStack.current

// Push a destination
navBackStack.add(Destination.Paywall("setting"))
navBackStack.add(Destination.Setting.License)

// Pop (two equivalent patterns used in the codebase)
navBackStack.removeLastOrNull()
navBackStack.removeAt(navBackStack.size - 1)
```

## Modifier Convention

`modifier: Modifier = Modifier` is always the **last non-lambda** parameter, applied to the root element:

```kotlin
@Composable
fun MyComponent(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,  // last before lambdas
) {
    Column(modifier = modifier) { ... }
}
```

## Collecting StateFlow

Always `collectAsStateWithLifecycle()` — never `collectAsState()`:

```kotlin
val screenState by viewModel.screenState.collectAsStateWithLifecycle()
val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
val selectedPlan by viewModel.selectedPlan.collectAsStateWithLifecycle()
```

## Shared UI Components (core:ui)

| Component | Purpose |
|---|---|
| `AsyncLoadContents` | Wraps ScreenState — shows Loading/Error/content |
| `LoadingView` | Standalone loading indicator |
| `ErrorView` | Error message with retry/close actions |
| `EmptyView` | Empty-state placeholder |
| `AsyncImageWithPlaceholder` | Coil3 image with placeholder |
| `SegmentedTabRow` | Segmented control |
| `ColorSlider` | Color picker slider |

## String Resources

All user-facing strings are in `core:resource`. Access in composables:

```kotlin
import org.jetbrains.compose.resources.stringResource
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.common_close

Text(text = stringResource(Res.string.common_close))
```

In ViewModels (non-composable), pass `StringResource` directly to `ScreenState.Error`:

```kotlin
ScreenState.Error(Res.string.error_network)
```

## Material Colors

Use `MaterialTheme` tokens — never hardcode:

```kotlin
// GOOD
containerColor = MaterialTheme.colorScheme.surface
Text(color = MaterialTheme.colorScheme.onBackground)

// BAD
containerColor = Color(0xFFFFFFFF)
```

## Edge-to-edge & window insets — 🚫 MUST

`MainActivity.onCreate` gọi `enableEdgeToEdge()` — toàn app vẽ dưới status bar + navigation bar. Hệ quả: **mọi top/bottom bar custom phải tự consume inset**, nếu không nội dung bị status bar hoặc nav bar che.

### Pattern copy-paste (để không phải suy nghĩ)

```kotlin
// ❌ SAI — status bar đè content
Row(Modifier.fillMaxWidth().padding(16.dp)) {
    Icon(...); Text(...)
}

// ✅ ĐÚNG — option 1 (recommended): M3 TopAppBar auto insets
@OptIn(ExperimentalMaterial3Api::class)
CenterAlignedTopAppBar(
    title = { Text(...) },
    navigationIcon = { IconButton(onClick = ...) { Icon(...) } },
)

// ✅ ĐÚNG — option 2: custom bar, MUST có statusBarsPadding()
Row(
    modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()       // ← bắt buộc
        .padding(16.dp),
) { ... }
```

Tham chiếu file đã áp dụng đúng:
- [HomeTopBar (HomeScreen.kt:108-140)](../../../feature/home/src/commonMain/kotlin/me/matsumo/grabee/feature/home/HomeScreen.kt#L108-L140) — M3 `CenterAlignedTopAppBar`.
- [SettingTopAppBar](../../../feature/setting/src/commonMain/kotlin/me/matsumo/grabee/feature/setting/components/SettingTopAppBar.kt) — M3 `CenterAlignedTopAppBar`.
- [UnitTopBar (UnitSelectionScreen.kt)](../../../feature/learningpath/src/commonMain/kotlin/me/matsumo/grabee/feature/learningpath/UnitSelectionScreen.kt) — M3 `CenterAlignedTopAppBar` (sau khi sửa từ custom Row gây lỗi status bar overlay).
- [OnboardingTopBar](../../../feature/onboarding/src/commonMain/kotlin/me/matsumo/grabee/feature/onboarding/components/OnboardingPage.kt) — custom `Box` + `.statusBarsPadding()`.

### Khi nào an toàn (tự động handle)

- **`Scaffold` + Material3 `TopAppBar` / `CenterAlignedTopAppBar`**: top app bar đã có `windowInsets = TopAppBarDefaults.windowInsets` mặc định → tự pad status bar.
- **`Scaffold` body** (`innerPadding`): nếu không có `bottomBar`, nav-bar inset được đưa vào `innerPadding` → `.padding(innerPadding)` trên body là đủ.

### Khi nào phải tự thêm inset padding

Tự viết top/bottom bar không dùng Material3 `TopAppBar` (vd custom `Row`, `Box` cho onboarding, splash, landing) — PHẢI áp dụng inset padding:

```kotlin
@Composable
fun CustomTopBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()          // ← bắt buộc khi custom
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) { ... }
}

@Composable
fun CustomBottomBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()      // ← bắt buộc khi custom
            .padding(16.dp),
    ) { ... }
}
```

Imports:
```kotlin
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding        // cho form có bàn phím
```

### Bottom-aligned action button (không có bottomBar slot)

Nếu đặt Button ở cuối Column trong Scaffold body, mà `innerPadding` đã chứa nav-bar inset (Scaffold không có `bottomBar`), **không cần** `navigationBarsPadding()` thêm — chỉ cần `.padding(innerPadding)` trên Column:

```kotlin
Scaffold(topBar = { CustomTopBar() }) { innerPadding ->
    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        Spacer(Modifier.weight(1f))
        Button(...)       // nav-bar inset đã được tôn trọng qua innerPadding
    }
}
```

### Full-screen content không Scaffold

Nếu không dùng Scaffold (vd dialog full-screen, splash custom) → áp dụng trực tiếp:

```kotlin
Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) { ... }
```

### Checklist trước khi commit UI mới

- [ ] Nếu có custom top bar (không phải M3 `TopAppBar`) → `.statusBarsPadding()` áp trên root modifier.
- [ ] Nếu có custom bottom bar → `.navigationBarsPadding()`.
- [ ] Nếu có form có `TextField` → `.imePadding()` ở content hoặc parent.
- [ ] Scaffold body luôn `.padding(innerPadding)` — đừng bỏ qua.
- [ ] Test trên device/emulator có gesture nav bar (Pixel) + 3-button nav (Samsung) để chắc không bị cut.
