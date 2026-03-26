# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Grabee Skill

A project-specific skill is available with full architecture knowledge, patterns, and feature guides:

```
/grabee <your request>
```

See `.claude/skills/grabee/README.md` for examples and full documentation.

---

## Commands

```bash
# Build
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease
./gradlew :composeApp:assembleBilling   # billing test build (release signing)

# Install
./gradlew :composeApp:installDebug

# Test
./gradlew test
./gradlew :core:datasource:testDebugUnitTest

# Lint
./gradlew detekt --auto-correct --continue
```

---

## Local configuration (`local.properties`)

Git-ignored. Keys read from here or environment variables:

| Key | Purpose |
|---|---|
| `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD`, `RELEASE_KEY_ALIAS` | Signing |
| `ADMOB_ANDROID_APP_ID`, `ADMOB_IOS_APP_ID` | AdMob (falls back to test IDs) |
| `PURCHASE_ANDROID_API_KEY`, `PURCHASE_IOS_API_KEY` | RevenueCat |
| `APPLOVIN_SDK_KEY` | AppLovin |

All injected at compile time via **BuildKonfig** → `me.matsumo.grabee.BuildKonfig`.

---

## Architecture

**Grabee** is a Kotlin Multiplatform app (Android + iOS) using Compose Multiplatform.

### Stack
- **UI** — Compose Multiplatform + Material3 Expressive + kolor
- **Navigation** — Navigation3 (`LocalNavBackStack`, `Destination` sealed interface)
- **DI** — Koin (`viewModelOf`, `singleOf`, per-module `module { }`)
- **State** — `ScreenState<T>` sealed class + `AsyncLoadContents` composable
- **DB** — Room KMP (KSP, all 4 targets in `core:datasource` only)
- **Network** — Ktor (OkHttp on Android, Darwin on iOS)
- **Billing** — RevenueCat KMP (`core:billing`)
- **Logging** — Napier (never `println()`)

### Module graph

```
composeApp → feature:* → core:ui, core:repository, core:datasource, core:model, core:common, core:resource
                         core:repository → core:datasource, core:billing
                         core:ui → core:repository, core:datasource
```

**Rule: `feature:*` never depends on another `feature:*`.**

### ViewModel patterns

**Pattern A** — expose repository `StateFlow` directly (simple screens like `SettingViewModel`).

**Pattern B** — `MutableStateFlow<ScreenState<UiState>>` + separate action StateFlow (async load + user actions like `PaywallViewModel`).

### Navigation

Destinations are `@Serializable` objects/data classes inside `Destination` sealed interface in `core:ui`. Push/pop via `LocalNavBackStack.current`. Features register via `EntryProviderScope` extension functions called in `AppNavHost`.

### Build conventions

Custom plugins in `build-logic/src/main/kotlin/primitive/`. Feature modules use `kmp.common + android.library + kmp.compose + kmp.android + kmp.ios + detekt`. All versions in `gradle/libs.versions.toml`.

### Room (KSP)

Only in `core:datasource`. Must target all 4 KMP platforms:

```kotlin
dependencies {
    listOf("kspAndroid", "kspIosX64", "kspIosArm64", "kspIosSimulatorArm64").forEach {
        add(it, libs.androidx.room.compiler)
    }
}
```
