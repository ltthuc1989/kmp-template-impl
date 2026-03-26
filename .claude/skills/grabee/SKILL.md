# Grabee — Kotlin Multiplatform Skill

Expert guidance for the **Grabee** KMP + Compose Multiplatform codebase. Use this skill when adding features, writing ViewModels, setting up navigation, or configuring modules.

## Core Principles

1. All business logic lives in `commonMain` — minimize `androidMain`/`iosMain`
2. **MVVM, no domain/use-case layer** — ViewModels call Repositories directly
3. **Unidirectional Data Flow** — state flows down via `StateFlow`, events flow up via callbacks
4. Feature modules are vertical slices; they never depend on each other
5. Only `core:datasource` owns Room + KSP — never add KSP to feature modules

## Module Graph

Verified from actual `build.gradle.kts` files:

| Module | Depends on (internal) |
|---|---|
| `core:common` | none — external libs only (Koin, Napier, Firebase) |
| `core:resource` | none — Compose runtime/resources only |
| `core:model` | `core:common`, `core:resource` |
| `core:datasource` | `core:common`, `core:model`, `core:resource` |
| `core:billing` | `core:common`, `core:model` |
| `core:repository` | `core:common`, `core:model`, `core:datasource`, `core:billing`, `core:resource` |
| `core:ui` | `core:common`, `core:model`, `core:repository`, `core:datasource`, `core:resource` |
| `feature:*` | `core:ui`, `core:repository`, `core:datasource`, `core:model`, `core:common`, `core:resource` |
| `composeApp` | all `feature:*` + all `core:*` |

**The one hard rule:**
- `feature:*` → never depends on another `feature:*`
- `core:datasource` is the only module that declares Room KSP

## ScreenState Pattern

All async screens use `ScreenState<T>` from `core:ui`:

```kotlin
sealed class ScreenState<out T> {
    data class Loading(val message: StringResource? = null) : ScreenState<Nothing>()
    data class Error(
        val message: StringResource,
        val retryTitle: StringResource? = null,
        val throwable: Throwable? = null,
    ) : ScreenState<Nothing>()
    data class Idle<T>(var data: T) : ScreenState<T>()
}
```

Wrap screens with `AsyncLoadContents` from `core:ui`:

```kotlin
AsyncLoadContents(
    screenState = screenState,
    retryAction = viewModel::fetch,
) { uiState ->
    // called only when Idle
    MyContent(uiState)
}
```

## ViewModel Patterns

### Pattern A — Simple (pass-through)
Use when the ViewModel just forwards repository state and triggers writes. See `SettingViewModel`.

```kotlin
class XyzViewModel(private val repository: XyzRepository) : ViewModel() {
    val data = repository.data  // already a StateFlow

    fun updateSomething(value: String) {
        viewModelScope.launch { repository.updateSomething(value) }
    }
}
```

### Pattern B — Complex (async load + action feedback)
Use when the screen loads async data AND has actions with their own feedback states. See `PaywallViewModel`.

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
data class XyzUiState(val items: ImmutableList<Item>)

@Stable
sealed interface XyzActionState {
    data object Idle : XyzActionState
    data object Loading : XyzActionState
    data object Success : XyzActionState
    data class Error(val message: String) : XyzActionState
}
```

UiState rules:
- Always annotate with `@Stable`
- Use `ImmutableList<T>` (from `kotlinx-collections-immutable`) for list fields
- Never expose `MutableStateFlow` — always `.asStateFlow()`

## Navigation3 Pattern

Destinations are defined in `core:ui/screen/Destination.kt`:

```kotlin
@Immutable
@Serializable
sealed interface Destination : NavKey {
    @Serializable data object Home : Destination
    @Serializable data class Download(val url: String) : Destination
    @Serializable data class Paywall(val source: String) : Destination
    @Serializable sealed interface Setting : Destination {
        @Serializable data object Root : Setting
        @Serializable data object License : Setting
    }
}
```

When adding a new destination, also register it in the `Destination.config` polymorphic serializers block.

**Navigate from a composable:**
```kotlin
val navBackStack = LocalNavBackStack.current
navBackStack.add(Destination.Setting.Root)  // push
navBackStack.removeLastOrNull()             // pop
```

**Register a screen** — create `XyzNavigation.kt` in your feature:
```kotlin
fun EntryProviderScope<NavKey>.xyzEntry() {
    entry<Destination.Xyz> {
        XyzScreen(modifier = Modifier.fillMaxSize())
    }
}
```
Then call `xyzEntry()` inside `AppNavHost`'s `entryProvider { }` block.

## Koin DI

Each feature registers a `val xyzModule = module { }` in `di/XyzModule.kt`:

```kotlin
val xyzModule = module {
    viewModelOf(::XyzViewModel)
    singleOf(::XyzRepository)
}
```

Add to `Koin.kt` in `composeApp`:
```kotlin
fun KoinApplication.applyModules() {
    // ...
    modules(xyzModule)
}
```

Access in composables: `val viewModel: XyzViewModel = koinViewModel()`

## Critical Dos and Don'ts

✅ Use `suspendRunCatching { }` for error handling in ViewModels (`core:common`)
✅ Use `collectAsStateWithLifecycle()` to collect `StateFlow` in composables
✅ Use `Napier` for logging — `Napier.d(...)`, `Napier.e(...)`
✅ Annotate UiState classes with `@Stable`, use `ImmutableList` for list fields
✅ Use `stringResource(Res.string.*)` for all user-facing strings
✅ Every composable accepts `modifier: Modifier = Modifier` as last non-lambda param

❌ Never expose `MutableStateFlow` from a ViewModel
❌ Never use `println()` — use Napier
❌ Never add Room/KSP to a feature module — only `core:datasource`
❌ Never depend on another `feature:*` from a feature module
❌ Never hardcode user-facing strings — use `core:resource`

## References

- [Architecture & module rules](references/architecture.md)
- [ViewModel patterns (full examples)](references/viewmodel-patterns.md)
- [Navigation3 (adding screens, arguments, back stack)](references/navigation.md)
- [Compose conventions (GrabeeTheme, stateless, Material3)](references/compose.md)
- [Build system (plugins, Room KSP, BuildKonfig)](references/build-system.md)
- [Billing & Ads (RevenueCat, AdMob, AppLovin)](references/billing-ads.md)
- [Adding a new feature (step-by-step checklist)](references/adding-feature.md)
