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
