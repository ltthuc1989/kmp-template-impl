# iOS Interop Reference — Grabee

## Entry Point

iOS entry point is `MainViewController()` in `composeApp/src/iosMain/.../Application.kt`:

```kotlin
@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController {
    val settingRepository = koinInject<AppSettingRepository>()
    val billingRepository = koinInject<BillingRepository>()
    val userData by settingRepository.setting.collectAsStateWithLifecycle(null)

    LaunchedEffect(Unit) {
        billingRepository.configure()
        settingRepository.initializeIdIfNeeded()
    }

    userData?.let {
        GrabeeApp(modifier = Modifier.fillMaxSize(), setting = it)
    }
}
```

Key differences from Android (`MainActivity`):
- No `ViewModel` — repositories are injected directly via `koinInject()`
- No splash screen — `userData?.let { }` naturally delays rendering until data loads
- `billingRepository.configure()` and `settingRepository.initializeIdIfNeeded()` run in `LaunchedEffect(Unit)`
- No `FileKit.init()` — not needed on iOS
- No `MobileAds.initialize()` — handled by iOS-specific ad setup

---

## Initialization (called from Swift)

Two functions in `composeApp/src/iosMain/.../InitHelper.kt` are called from Swift before the UI:

```kotlin
fun initKoin() {
    startKoin { applyModules() }
}

fun initNapier() {
    Napier.base(DebugAntilog())
}
```

Call order in Swift: `initNapier()` → `initKoin()` → `MainViewController()`

---

## Platform-Specific Koin Module

`appModulePlatform` uses `expect/actual`. iOS has no platform-specific bindings:

```kotlin
// iosMain — AppModule.ios.kt
internal actual val appModulePlatform: Module = module {
    // nothing
}

// androidMain — AppModule.android.kt
internal actual val appModulePlatform: Module = module {
    viewModelOf(::MainViewModel)  // Android-only ViewModel for ads SDK init state
}
```

---

## expect/actual for Platform Detection

```kotlin
// core:model/commonMain
enum class Platform { Android, IOS }
expect val currentPlatform: Platform

// core:model/androidMain
actual val currentPlatform = Platform.Android

// core:model/iosMain
actual val currentPlatform = Platform.IOS
```

Used in `AppModule.kt` to select the correct AdMob IDs per platform:

```kotlin
when (currentPlatform) {
    Platform.Android -> { adMobAppId = BuildKonfig.ADMOB_ANDROID_APP_ID; ... }
    Platform.IOS     -> { adMobAppId = BuildKonfig.ADMOB_IOS_APP_ID; ... }
}
```

---

## Android Entry Point (for comparison)

`composeApp/src/androidMain/.../MainActivity.kt`:
- Extends `ComponentActivity`
- Uses `viewModel<MainViewModel>()` (Koin) for ads SDK init state
- Calls `FileKit.init(this)` for file picker dialogs
- Calls `MobileAds.initialize(this)` for AdMob
- Installs splash screen via `installSplashScreen()`
- Sets edge-to-edge with dynamic system bar styling via `DisposableEffect`
