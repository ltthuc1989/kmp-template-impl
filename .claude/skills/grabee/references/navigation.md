# Navigation Reference — Grabee

Uses **Navigation3** (`androidx.navigation3`), not the traditional `NavController`/`NavHost` pattern.

## Key Files

- `core/ui/src/commonMain/.../screen/Destination.kt` — all route definitions
- `core/ui/src/commonMain/.../theme/NavController.kt` — `LocalNavBackStack` CompositionLocal
- `composeApp/src/commonMain/.../AppNavHost.kt` — root NavDisplay
- `feature/*/src/commonMain/.../*Navigation.kt` — per-feature entry registration

---

## Destination Sealed Interface

All screens are defined as objects/data classes inside `Destination` in `core:ui`:

```kotlin
@Immutable
@Serializable
sealed interface Destination : NavKey {
    @Serializable data object Home : Destination
    @Serializable data class Download(val url: String) : Destination
    @Serializable data class Paywall(val source: String) : Destination

    @Serializable
    sealed interface Setting : Destination {
        @Serializable data object Root : Setting
        @Serializable data object License : Setting
    }

    companion object {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Home::class, Home.serializer())
                    subclass(Download::class, Download.serializer())
                    subclass(Paywall::class, Paywall.serializer())
                    subclass(Setting.Root::class, Setting.Root.serializer())
                    subclass(Setting.License::class, Setting.License.serializer())
                }
            }
        }
    }
}
```

**When adding a new destination:**
1. Add `@Serializable data object/class Xyz : Destination` inside the sealed interface
2. Add `subclass(Xyz::class, Xyz.serializer())` in the `polymorphic` block in `Destination.config`

---

## LocalNavBackStack

Defined in `core/ui/src/commonMain/.../theme/NavController.kt`:

```kotlin
val LocalNavBackStack = staticCompositionLocalOf<MutableList<NavKey>> {
    error("No NavBackStack provided")
}
```

**Navigate (push):**
```kotlin
val navBackStack = LocalNavBackStack.current
navBackStack.add(Destination.Setting.Root)
navBackStack.add(Destination.Paywall(source = "home"))  // with argument
```

**Go back (pop):**
```kotlin
navBackStack.removeLastOrNull()
```

---

## AppNavHost

Located in `composeApp/src/commonMain/.../AppNavHost.kt`. Wire new feature entries here:

```kotlin
@Composable
internal fun AppNavHost(modifier: Modifier = Modifier) {
    val navBackStack = rememberNavBackStack(Destination.config, Destination.Home)

    CompositionLocalProvider(LocalNavBackStack provides navBackStack) {
        NavDisplay(
            modifier = modifier,
            backStack = navBackStack,
            entryProvider = entryProvider {
                homeEntry()
                paywallEntry()
                settingEntry()
                settingLicenseEntry()
                // ADD NEW ENTRIES HERE
            },
            transitionSpec = { NavigationTransitions.forwardTransition },
            popTransitionSpec = { NavigationTransitions.backwardTransition },
            predictivePopTransitionSpec = { NavigationTransitions.backwardTransition },
        )
    }
}
```

---

## Registering a New Screen

Create `XyzNavigation.kt` in your feature module:

```kotlin
// No arguments (e.g. settingEntry pattern)
fun EntryProviderScope<NavKey>.xyzEntry() {
    entry<Destination.Xyz> {
        XyzScreen(modifier = Modifier.fillMaxSize())
    }
}

// With arguments from Destination data class (e.g. paywallEntry pattern)
fun EntryProviderScope<NavKey>.xyzEntry() {
    entry<Destination.Xyz> { destination ->
        XyzScreen(
            source = destination.source,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

Then call `xyzEntry()` inside `AppNavHost`'s `entryProvider { }` block.

---

## Nested / Grouped Destinations

Group related screens under a nested sealed interface (see `Destination.Setting`):

```kotlin
@Serializable
sealed interface Setting : Destination {
    @Serializable data object Root : Setting
    @Serializable data object License : Setting
}
```

Each subclass still needs its own `subclass(...)` entry in `Destination.config`.
