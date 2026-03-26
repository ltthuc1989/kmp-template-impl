# Grabee Claude Code Skill

A project-specific Claude Code skill that gives Claude expert knowledge of the **Grabee** KMP + Compose Multiplatform codebase — architecture, patterns, conventions, and step-by-step feature guides.

---

## What It Does

When you invoke this skill, Claude instantly knows your exact codebase without re-exploring it:

- Module structure, dependency rules, and layer responsibilities
- Both ViewModel patterns (A and B) with real code from your project
- Navigation3 patterns — `LocalNavBackStack`, `Destination`, `EntryProviderScope`
- `ScreenState<T>` + `AsyncLoadContents` async UI pattern
- Koin DI wiring — `viewModelOf`, module hierarchy, `applyModules()`
- Build conventions — plugin combos, Room KSP multi-target rule, BuildKonfig
- The 11-step checklist for adding a new feature module from scratch

---

## Installation

### Already installed (this project)
The skill lives at `.claude/skills/grabee/` — Claude Code picks it up automatically when working in this repo.

### Share with your team
```bash
git add .claude/skills/grabee/
git commit -m "Add Grabee Claude Code skill"
```
Teammates get the skill automatically on pull.

### Install globally
```bash
cp -r .claude/skills/grabee/ ~/.claude/skills/grabee/
```

---

## How to Invoke

```
/grabee <your request>
```

Or inline:
```
Using the grabee skill, <your request>
```

---

## Architecture Overview

### Tech Stack
| Area | Library |
|---|---|
| UI | Compose Multiplatform 1.10.2 |
| Navigation | Navigation3 (`androidx.navigation3`) |
| DI | Koin 4.1.1 |
| Database | Room KMP 2.7.1 |
| Networking | Ktor 3.3.3 |
| Preferences | DataStore KMP 1.2.0 |
| Images | Coil3 3.3.0 + FileKit |
| Billing | RevenueCat KMP 2.8.0 |
| Ads | AdMob + AppLovin via BuildKonfig |
| Logging | Napier 2.7.1 |
| Theme | Material3 Expressive + kolor (Material You) |

### Module Graph
```
composeApp
  ├── feature:home
  ├── feature:setting
  ├── feature:billing
  └── feature:download
        ↓
  core:ui          ← Composables, GrabeeTheme, ScreenState, Destination, LocalNavBackStack
  core:repository  ← Business logic, BillingRepository, AppSettingRepository
  core:datasource  ← Room DB, Ktor HTTP, DataStore, FileKit  [only module with KSP]
  core:billing     ← RevenueCat KMP wrapper, PurchaseResult, SubscriptionPlan
  core:model       ← AppSetting, AppConfig, Platform, Theme
  core:common      ← suspendRunCatching, Napier, Koin BOM, Firebase
  core:resource    ← strings.xml (EN + JA), fonts, images
```

**Only hard rule: `feature:*` never depends on another `feature:*`.**

### Architecture Pattern
**MVVM — no domain/use-case layer.** ViewModels call Repositories directly.

```
Screen (Composable)
  → ViewModel (StateFlow)
    → Repository (suspend / Flow)
      → DataSource (Room DAO / Ktor / DataStore)
```

---

## ViewModel Patterns

### Pattern A — Simple Pass-Through
Use when the screen just displays repository state and dispatches writes. Example: `SettingViewModel`.

```kotlin
class XyzViewModel(private val repository: XyzRepository) : ViewModel() {
    val data = repository.data   // expose repo StateFlow directly

    fun update(value: String) {
        viewModelScope.launch { repository.update(value) }
    }
}
```

### Pattern B — Async Load + Action Feedback
Use when the screen loads async data (Loading → Idle/Error) AND has user actions with feedback. Example: `PaywallViewModel`.

```kotlin
class XyzViewModel(private val repository: XyzRepository) : ViewModel() {
    private val _screenState = MutableStateFlow<ScreenState<XyzUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<XyzUiState>> = _screenState.asStateFlow()

    private val _actionState = MutableStateFlow<XyzActionState>(XyzActionState.Idle)
    val actionState: StateFlow<XyzActionState> = _actionState.asStateFlow()

    init { fetch() }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading()
            _screenState.value = suspendRunCatching {
                XyzUiState(items = repository.getItems().toImmutableList())
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }
}

@Stable
data class XyzUiState(val items: ImmutableList<Item>)   // always @Stable + ImmutableList

@Stable
sealed interface XyzActionState {
    data object Idle : XyzActionState
    data object Loading : XyzActionState
    data object Success : XyzActionState
    data class Error(val message: String) : XyzActionState
}
```

---

## Navigation

Uses **Navigation3** — not the traditional `NavController`/`NavHost`.

### Destination Sealed Interface
All screens are defined in `core/ui/.../screen/Destination.kt`:

```kotlin
@Immutable @Serializable
sealed interface Destination : NavKey {
    @Serializable data object Home : Destination
    @Serializable data class Paywall(val source: String) : Destination
    @Serializable sealed interface Setting : Destination {
        @Serializable data object Root : Setting
        @Serializable data object License : Setting
    }
}
```

### Navigate in Composables
```kotlin
val navBackStack = LocalNavBackStack.current
navBackStack.add(Destination.Paywall("home"))   // push
navBackStack.removeLastOrNull()                  // pop
```

### Register a Screen
```kotlin
// XyzNavigation.kt
fun EntryProviderScope<NavKey>.xyzEntry() {
    entry<Destination.Xyz> { destination ->
        XyzScreen(id = destination.id, modifier = Modifier.fillMaxSize())
    }
}
// Then add xyzEntry() inside AppNavHost's entryProvider { } block
```

---

## ScreenState + AsyncLoadContents

All async screens use `ScreenState<T>` from `core:ui`:

```kotlin
// In ViewModel
_screenState.value = ScreenState.Loading()
_screenState.value = ScreenState.Idle(data)
_screenState.value = ScreenState.Error(Res.string.error_network)

// In Screen
AsyncLoadContents(
    screenState = screenState,
    retryAction = viewModel::fetch,
) { uiState ->
    MyContent(uiState)   // only called when Idle
}
```

---

## Koin DI

```kotlin
// di/XyzModule.kt
val xyzModule = module {
    viewModelOf(::XyzViewModel)
    singleOf(::XyzRepository)
}

// composeApp/.../di/Koin.kt — register here
fun KoinApplication.applyModules() {
    modules(appModule, commonModule, billingModule, dataSourceModule, repositoryModule)
    modules(homeModule, settingModule, billingFeatureModule)
    modules(xyzModule)  // add new modules here
}

// In composable
val viewModel: XyzViewModel = koinViewModel()
```

---

## Build Conventions

### Plugin Combo by Module Type
| Module type | Plugins |
|---|---|
| `feature:*` (has Compose) | `kmp.common` + `android.library` + `kmp.compose` + `kmp.android` + `kmp.ios` + `detekt` |
| `core:ui`, `core:resource` | Same as feature |
| `core:datasource` (Room/KSP) | `kmp.common` + `android.library` + `kmp.android` + `kmp.ios` + `detekt` + `ksp` |
| `core:repository`, `core:model`, `core:billing`, `core:common` | `kmp.common` + `android.library` + `kmp.android` + `kmp.ios` + `detekt` |

### Room KSP — Critical Rule
Only in `core:datasource/build.gradle.kts`, always all 4 targets:
```kotlin
dependencies {
    listOf("kspAndroid", "kspIosX64", "kspIosArm64", "kspIosSimulatorArm64").forEach {
        add(it, libs.androidx.room.compiler)
    }
}
```

### Build Variants
```bash
./gradlew :composeApp:assembleDebug      # development
./gradlew :composeApp:assembleBilling    # test IAP with release signing
./gradlew :composeApp:assembleRelease    # production
```

---

## GrabeeTheme

```kotlin
GrabeeTheme(appSetting = setting) {
    // provides LocalAppSetting, LocalAppConfig, MaterialExpressiveTheme
}

// Access anywhere in the tree
val appSetting = LocalAppSetting.current   // theme, seedColor, plusMode…
val appConfig  = LocalAppConfig.current    // AdMob IDs, API keys, versionName
```

---

## Error Handling

```kotlin
// Always use suspendRunCatching — never bare try/catch in ViewModels
_screenState.value = suspendRunCatching {
    repository.fetchData()
}.fold(
    onSuccess = { ScreenState.Idle(it) },
    onFailure = { ScreenState.Error(Res.string.error_network) },
)

// Always Napier for logging — never println()
Napier.d("message")
Napier.e("error", throwable)
```

---

## Adding a New Feature — Checklist

1. Create `feature/xyz/` with correct `build.gradle.kts` plugin combo
2. Add `include(":feature:xyz")` to `settings.gradle.kts`
3. Add `Destination.Xyz` to `Destination.kt` + register in `Destination.config`
4. Write `XyzViewModel` (choose Pattern A or B)
5. Write `XyzScreen` composable
6. Write `xyzEntry()` in `XyzNavigation.kt`
7. Register `xyzEntry()` in `AppNavHost.kt`
8. Create `di/XyzModule.kt` with `viewModelOf()`
9. Add `xyzModule` to `Koin.kt` `applyModules()`
10. Add `project(":feature:xyz")` to `composeApp/build.gradle.kts`
11. Add all new strings to `core/resource/.../values/strings.xml`

---

## Skill Reference Files

| File | Covers |
|---|---|
| `SKILL.md` | Core principles, module graph, quick patterns, dos & don'ts |
| `references/architecture.md` | Verified dependency graph, layer rules, end-to-end data flow |
| `references/viewmodel-patterns.md` | Pattern A + B with full real code, UiState rules, suspendRunCatching |
| `references/navigation.md` | Destination sealed interface, LocalNavBackStack, EntryProviderScope |
| `references/compose.md` | GrabeeTheme, stateless composables, AsyncLoadContents, shared components |
| `references/build-system.md` | Plugin combos, Room KSP rule, BuildKonfig flow, build variants |
| `references/error-handling.md` | suspendRunCatching, ScreenState.Error, error string keys, Napier |
| `references/testing.md` | Fakes over mocks, ViewModel tests, Ktor mock client |
| `references/i18n.md` | Strings in core:resource, EN + JA locales, format args |
| `references/ios-interop.md` | MainViewController, InitHelper, expect/actual, Android vs iOS entry |
| `references/adding-feature.md` | Full 11-step checklist with exact file paths |

---

## Example Prompts

### Add a complete new feature
```
/grabee Add a new feature module called "profile". Pattern B ViewModel,
loads user data from repository, AsyncLoadContents screen,
Destination.Profile (no args). Complete all 11 steps.
```

### Add a screen with arguments
```
/grabee Add a DownloadDetail screen to feature:download.
Destination.DownloadDetail(val url: String). Pattern B ViewModel.
Wire navigation, Koin, and AppNavHost.
```

### Generate a ViewModel only
```
/grabee Generate a Pattern B ViewModel for a screen that loads
a paginated list of images and has a "save to gallery" action
with Idle/Loading/Success/Error states.
```

### Architecture question
```
/grabee Where should file download logic go — datasource or repository?
```

### Build question
```
/grabee What plugin combo does a new core module without Compose need?
```

---

## Tips

- **Specify the pattern** — "Pattern A" or "Pattern B" so Claude picks the right ViewModel shape
- **Specify destination type** — `data object` (no args) or `data class(val x: Type)` (with args)
- **Ask for the full checklist** — add "complete all 11 steps" to get everything wired end-to-end
- **One task at a time** — ViewModel → Screen → Navigation gives cleaner results than all at once
