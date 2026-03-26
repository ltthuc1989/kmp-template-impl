# Build System Reference — Grabee

## Convention Plugin Combinations

Every module applies a specific set of plugins from `build-logic/`. Use these exact combos:

### feature:* (has UI/Compose)
```kotlin
id("matsumo.primitive.kmp.common")
id("matsumo.primitive.android.library")
id("matsumo.primitive.kmp.compose")
id("matsumo.primitive.kmp.android")
id("matsumo.primitive.kmp.ios")
id("matsumo.primitive.detekt")
```

### core:ui, core:resource (Compose, no KSP)
```kotlin
id("matsumo.primitive.kmp.common")
id("matsumo.primitive.android.library")
id("matsumo.primitive.kmp.compose")
id("matsumo.primitive.kmp.android")
id("matsumo.primitive.kmp.ios")
id("matsumo.primitive.detekt")
```

### core:datasource (no Compose, has KSP for Room)
```kotlin
id("matsumo.primitive.kmp.common")
id("matsumo.primitive.android.library")
id("matsumo.primitive.kmp.android")
id("matsumo.primitive.kmp.ios")
id("matsumo.primitive.detekt")
alias(libs.plugins.ksp)
```

### core:repository, core:model, core:billing, core:common (no Compose, no KSP)
```kotlin
id("matsumo.primitive.kmp.common")
id("matsumo.primitive.android.library")
id("matsumo.primitive.kmp.android")
id("matsumo.primitive.kmp.ios")
id("matsumo.primitive.detekt")
```

---

## Room KSP — Critical Rule

Room compiler must be declared for **all 4 KMP targets** in `core:datasource/build.gradle.kts` only:

```kotlin
dependencies {
    listOf("kspAndroid", "kspIosX64", "kspIosArm64", "kspIosSimulatorArm64").forEach { target ->
        add(target, libs.androidx.room.compiler)
    }
}
```

Never add KSP or Room compiler to feature modules or other core modules.

---

## Adding a New Module

1. Create the module directory: `feature/xyz/` or `core/xyz/`
2. Create `build.gradle.kts` with the correct plugin combo above
3. Set the Android namespace:
   ```kotlin
   android {
       namespace = "me.matsumo.grabee.feature.xyz"
   }
   ```
4. Add dependencies under `kotlin { sourceSets { commonMain.dependencies { ... } } }`
5. Register in `settings.gradle.kts`:
   ```kotlin
   include(":feature:xyz")
   ```
6. Add as a dependency in `composeApp/build.gradle.kts`:
   ```kotlin
   commonMain.dependencies {
       implementation(project(":feature:xyz"))
   }
   ```

---

## BuildKonfig — Compile-time Config

All API keys and config are injected via BuildKonfig into `me.matsumo.grabee.BuildKonfig`.

**Flow:** `local.properties` (or env var) → `buildkonfig { }` in `composeApp/build.gradle.kts` → `BuildKonfig.FIELD_NAME`

At runtime, access config through `AppConfig` (assembled in `AppModule.kt`):
```kotlin
val appConfig = LocalAppConfig.current
appConfig.adMobBannerAdUnitId
appConfig.versionName
appConfig.purchaseAndroidApiKey
```

Keys defined in `local.properties` (git-ignored):
- `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD`, `RELEASE_KEY_ALIAS`
- `ADMOB_ANDROID_APP_ID`, `ADMOB_IOS_APP_ID`
- `PURCHASE_ANDROID_API_KEY`, `PURCHASE_IOS_API_KEY`
- `APPLOVIN_SDK_KEY`

---

## Build Variants

| Variant | Signing | App ID suffix | Notes |
|---|---|---|---|
| `debug` | debug keystore | `.debug` | `versionName` suffix `.D` |
| `billing` | release keystore | none | `isDebuggable = true`, for testing IAP |
| `release` | release keystore | none | `isMinifyEnabled = true`, ProGuard |

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleBilling
./gradlew :composeApp:assembleRelease
./gradlew :composeApp:installDebug
```

---

## Version Catalog

All versions and aliases live in `gradle/libs.versions.toml`. Always use aliases:

```kotlin
// GOOD
implementation(libs.ktor.core)
implementation(libs.bundles.compose)

// BAD
implementation("io.ktor:ktor-client-core:3.3.3")
```

Available bundles: `infra`, `ui-android`, `ui-common`, `compose`, `purchase`, `ktor`, `koin`, `calf`, `firebase`, `filekit`
