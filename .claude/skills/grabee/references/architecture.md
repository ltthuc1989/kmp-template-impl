# Architecture Reference — Grabee

## Layer Responsibilities

```
feature:*  (Presentation)
  ├── *Screen.kt       — stateless Composable, receives state + callbacks
  ├── *ViewModel.kt    — holds StateFlow, calls repository
  ├── *Navigation.kt   — EntryProviderScope extension, registers entry<Destination>
  └── di/*Module.kt    — Koin module with viewModelOf()

core:repository  (Business logic)
  └── *Repository.kt   — suspend functions + Flow, orchestrates datasource + billing

core:datasource  (Data access — Room, Ktor, DataStore, FileKit)
core:billing     (RevenueCat KMP wrapper)
core:ui          (Composables, GrabeeTheme, ScreenState, Destination, LocalNavBackStack)
core:model       (AppSetting, AppConfig, Platform, Theme — plain Kotlin)
core:common      (suspendRunCatching, Koin BOM, Napier, Firebase)
core:resource    (strings.xml, images — Compose Multiplatform resources)
```

## Verified Dependency Graph

From actual `build.gradle.kts`:

```
core:common      ← external only
core:resource    ← external only
core:model       ← core:common, core:resource
core:datasource  ← core:common, core:model, core:resource
core:billing     ← core:common, core:model
core:repository  ← core:common, core:model, core:datasource, core:billing, core:resource
core:ui          ← core:common, core:model, core:repository, core:datasource, core:resource
feature:*        ← core:ui, core:repository, core:datasource, core:model, core:common, core:resource
composeApp       ← all feature:* + all core:*
```

**Only absolute rule: `feature:*` never depends on another `feature:*`**

## ScreenState Sealed Class

Location: `core/ui/src/commonMain/kotlin/me/matsumo/grabee/core/ui/screen/ScreenState.kt`

```kotlin
@Stable
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

## End-to-End Data Flow

**Example: User changes theme in SettingScreen**

```
SettingScreen → onThemeChanged(theme)
  → SettingViewModel.setTheme(theme)    [viewModelScope.launch]
    → AppSettingRepository.setTheme()   [suspend]
      → AppSettingDataSource (DataStore.edit)
        → DataStore emits new value
          → repository.setting Flow updates
            → SettingViewModel.setting (direct repo StateFlow)
              → SettingScreen recomposes [collectAsStateWithLifecycle()]
```

## Platform Differentiation (expect/actual)

```kotlin
// core:model/commonMain
enum class Platform { Android, IOS }
expect val currentPlatform: Platform

// core:model/androidMain
actual val currentPlatform = Platform.Android

// core:model/iosMain
actual val currentPlatform = Platform.IOS
```

Koin platform modules follow the same pattern in `composeApp`:
```kotlin
// commonMain — AppModule.kt
internal expect val appModulePlatform: Module

// androidMain
internal actual val appModulePlatform = module { /* Android-specific bindings */ }

// iosMain
internal actual val appModulePlatform = module { /* iOS-specific bindings */ }
```
