# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project: ABC Phonics Kids

App học phonics tiếng Anh cho trẻ 3-8 tuổi, build trên template KMP + Compose Multiplatform.

**Product docs**: [docs/abc-phonics-kids/README.md](docs/abc-phonics-kids/README.md) — đọc trước khi code.

**App display name**: "ABC Phonics Kids" (qua `app_name` string). **Code namespace giữ nguyên** `me.matsumo.grabee` để tránh rebrand 100+ file.

## Grabee Skill

A project-specific skill with full architecture knowledge, patterns, COPPA rules, voice pipeline, and UI-from-screenshot guidance:

```
/grabee <your request>
```

**Auto-load rules** (skill tự load reference phù hợp):
- Audio/voice/microphone/parental → `references/coppa.md` + `references/voice-recognition.md`
- Level/unit/word/step/phonics → `references/phonics-domain.md`
- **User attach UI mockup image** → `references/ui-from-screenshot.md` (đọc image qua Read tool trước khi viết code)

See [.claude/skills/grabee/README.md](.claude/skills/grabee/README.md) for full reference table.

## Project Commands

```
/scaffold-unit <level_id> <unit_number>
```
Scaffold 1 phonics unit (8 step screens + Room seed). Optional: attach UI mockup. See [.claude/commands/scaffold-unit.md](.claude/commands/scaffold-unit.md).

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
