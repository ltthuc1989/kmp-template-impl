# Adding a New Feature — Grabee

Complete checklist for adding a new feature module. Every step maps to a real file in the project.

---

## Step 1 — Create the Module Directory

```
feature/
└── xyz/
    └── src/
        └── commonMain/
            └── kotlin/
                └── me/matsumo/grabee/feature/xyz/
```

---

## Step 2 — Create `build.gradle.kts`

Feature modules always use this plugin combo (verified from `feature/home/build.gradle.kts`):

```kotlin
plugins {
    id("matsumo.primitive.kmp.common")
    id("matsumo.primitive.android.library")
    id("matsumo.primitive.kmp.compose")
    id("matsumo.primitive.kmp.android")
    id("matsumo.primitive.kmp.ios")
    id("matsumo.primitive.detekt")
}

android {
    namespace = "me.matsumo.grabee.feature.xyz"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:repository"))
            implementation(project(":core:datasource"))
            implementation(project(":core:ui"))
            implementation(project(":core:resource"))
        }
    }
}
```

---

## Step 3 — Register in `settings.gradle.kts`

File: `settings.gradle.kts` (root)

```kotlin
include(":feature:xyz")
```

---

## Step 4 — Add Destination

File: `core/ui/src/commonMain/.../screen/Destination.kt`

```kotlin
@Immutable
@Serializable
sealed interface Destination : NavKey {
    // ... existing destinations ...

    @Serializable
    data object Xyz : Destination           // no arguments

    @Serializable
    data class Xyz(val id: String) : Destination  // with arguments
}
```

Also add to the `polymorphic` block in `Destination.config`:
```kotlin
subclass(Xyz::class, Xyz.serializer())
```

---

## Step 5 — Write the ViewModel

File: `feature/xyz/src/commonMain/.../XyzViewModel.kt`

Choose **Pattern A** (simple pass-through) or **Pattern B** (async load + action feedback).
See [viewmodel-patterns.md](viewmodel-patterns.md) for full examples.

```kotlin
class XyzViewModel(private val repository: XyzRepository) : ViewModel() {
    // Pattern A or B here
}
```

---

## Step 6 — Write the Screen

File: `feature/xyz/src/commonMain/.../XyzScreen.kt`

```kotlin
@Composable
internal fun XyzScreen(
    modifier: Modifier = Modifier,
    viewModel: XyzViewModel = koinViewModel(),
) {
    val navBackStack = LocalNavBackStack.current
    // collect state, render UI
}
```

---

## Step 7 — Write the Navigation Entry

File: `feature/xyz/src/commonMain/.../XyzNavigation.kt`

```kotlin
fun EntryProviderScope<NavKey>.xyzEntry() {
    entry<Destination.Xyz> {
        XyzScreen(modifier = Modifier.fillMaxSize())
    }
}

// With argument:
fun EntryProviderScope<NavKey>.xyzEntry() {
    entry<Destination.Xyz> { destination ->
        XyzScreen(
            id = destination.id,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

---

## Step 8 — Create Koin Module

File: `feature/xyz/src/commonMain/.../di/XyzModule.kt`

```kotlin
val xyzModule = module {
    viewModelOf(::XyzViewModel)
}
```

---

## Step 9 — Register in `Koin.kt`

File: `composeApp/src/commonMain/.../di/Koin.kt`

```kotlin
fun KoinApplication.applyModules() {
    modules(appModule)
    modules(commonModule)
    modules(billingModule)
    modules(dataSourceModule)
    modules(repositoryModule)
    modules(homeModule)
    modules(settingModule)
    modules(billingFeatureModule)
    modules(xyzModule)   // ← add here
}
```

---

## Step 10 — Register in `AppNavHost`

File: `composeApp/src/commonMain/.../AppNavHost.kt`

```kotlin
entryProvider = entryProvider {
    homeEntry()
    paywallEntry()
    settingEntry()
    settingLicenseEntry()
    xyzEntry()   // ← add here
}
```

---

## Step 11 — Add to `composeApp` Dependencies

File: `composeApp/build.gradle.kts`

```kotlin
commonMain.dependencies {
    // ... existing ...
    implementation(project(":feature:xyz"))
}
```

---

## Checklist Summary

- [ ] Module directory created
- [ ] `build.gradle.kts` with correct plugin combo
- [ ] Registered in `settings.gradle.kts`
- [ ] `Destination.Xyz` added + registered in `Destination.config`
- [ ] `XyzViewModel.kt` written
- [ ] `XyzScreen.kt` written
- [ ] `XyzNavigation.kt` written
- [ ] `di/XyzModule.kt` with `viewModelOf()`
- [ ] `xyzModule` added to `Koin.kt`
- [ ] `xyzEntry()` added to `AppNavHost.kt`
- [ ] `project(":feature:xyz")` added to `composeApp/build.gradle.kts`
