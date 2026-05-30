# Mốc 1 — Ad Code Archive (for Mốc 2 restoration)

Date: 2026-05-30
Branch: `moc1-no-ads` (split from `audio` at commit `a42ffef`)

This file archives ad-related code REMOVED in Mốc 1 to claim "ad-free" cleanly.
When Mốc 2 ships (Level 2-5 + monetization), restore code via:

1. Re-add dependencies in `core/ui/build.gradle.kts`
2. Re-add AdMob meta-data in `AndroidManifest.xml`
3. Re-add ADMOB BuildKonfig fields in `composeApp/build.gradle.kts`
4. Restore ad code files from this archive + git history
5. Flip `MONETIZATION_ENABLED = true` in `core/model/MonetizationConfig.kt`

Or simpler: `git revert <removal-commit>` to undo bulk removal.

## Dependencies removed

In `core/ui/build.gradle.kts` androidMain dependencies:
```kotlin
api(libs.lexilabs.basic.ads)
api(libs.play.service.ads)
```

In `gradle/libs.versions.toml`:
```toml
[versions]
lexilabsBasicAds = "..."
playServiceAds = "..."

[libraries]
play-service-ads = { module = "com.google.android.gms:play-services-ads", version.ref = "playServiceAds" }
lexilabs-basic-ads = { group = "app.lexilabs.basic", name = "basic-ads", version.ref = "lexilabsBasicAds" }
```

## Manifest entries removed

In `composeApp/src/androidMain/AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${ADMOB_ANDROID_APP_ID}" />
```

## Gradle config removed

In `composeApp/build.gradle.kts`:
```kotlin
val admobTestAppId = "ca-app-pub-0000000000000000~0000000000"

manifestPlaceholders.put("ADMOB_ANDROID_APP_ID", ...)
manifestPlaceholders.put("ADMOB_IOS_APP_ID", ...)

BuildKonfig fields:
  setField("ADMOB_ANDROID_APP_ID", admobTestAppId)
  setField("ADMOB_IOS_APP_ID", admobTestAppId)
  setField("ADMOB_INTERSTITIAL_AD_UNIT_ID", ...)
  setField("ADMOB_BANNER_AD_UNIT_ID", ...)
  setField("ADMOB_REWARDED_AD_UNIT_ID", ...)
  setField("ADMOB_APP_OPEN_AD_UNIT_ID", ...)
```

## Code files deleted (full restoration via git)

| File | Purpose | Lines |
|---|---|---|
| `composeApp/src/androidMain/kotlin/me/ltthuc/kmp/ads/AdsInitializer.kt` | AdMob SDK initializer, kids-safe AdRequest builder | 28 |
| `composeApp/src/androidMain/kotlin/me/ltthuc/kmp/ads/AppOpenAdManager.kt` | App open ad lifecycle manager, 4hr expiry, cold-start suppression | 130 |

Restore via:
```bash
git show a42ffef:composeApp/src/androidMain/kotlin/me/ltthuc/kmp/ads/AdsInitializer.kt > <destination>
git show a42ffef:composeApp/src/androidMain/kotlin/me/ltthuc/kmp/ads/AppOpenAdManager.kt > <destination>
```

## Code files stubbed (kept structure, removed AdMob imports)

| File | Stub strategy |
|---|---|
| `core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/ads/BottomBannerAd.kt` | Empty Composable (returns Unit, no Box, no BannerAd call) |
| `core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/ads/LearningInterstitial.kt` | Empty Composable |
| `core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/ads/RewardedSkipLauncher.kt` | Empty Composable, LaunchedEffect calls onUnavailable() immediately |

Restore original implementation:
```bash
git show a42ffef:core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/ads/BottomBannerAd.kt
git show a42ffef:core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/ads/LearningInterstitial.kt
git show a42ffef:core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/ads/RewardedSkipLauncher.kt
```

## AppConfig field changes

In `core/model/.../AppConfig.kt`:
- Kept `adMob*AdUnitId` fields as String defaults to empty (for compile compat)
- OR removed entirely — choose at restoration time

## Mốc 2 restoration checklist

When restoring monetization for Mốc 2:

- [ ] Restore deleted files from git: `AdsInitializer.kt`, `AppOpenAdManager.kt`
- [ ] Restore ad composable bodies from git
- [ ] Re-add ad dependencies in `core/ui/build.gradle.kts`
- [ ] Re-add AdMob meta-data in `AndroidManifest.xml`
- [ ] Re-add ADMOB manifestPlaceholders + BuildKonfig fields in `composeApp/build.gradle.kts`
- [ ] Flip `MONETIZATION_ENABLED = true` in `core/model/MonetizationConfig.kt`
- [ ] Add `local.properties` entries: `ADMOB_ANDROID_APP_ID`, `ADMOB_IOS_APP_ID`, etc.
- [ ] Verify ad init runs only on Premium-unlock paths
- [ ] Update Privacy Policy to re-add §4 Advertising + §5 In-App Purchases sections
- [ ] Update 9 listing.md to add Premium tier sections
- [ ] Update Play Console Data Safety form: Contains ads = Yes, IAP = Yes
