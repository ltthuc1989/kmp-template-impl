@file:Suppress("UnusedPrivateProperty")

import com.android.build.api.variant.ResValue
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("ltthuc.primitive.kmp.common")
    id("ltthuc.primitive.android.application")
    id("ltthuc.primitive.kmp.compose")
    id("ltthuc.primitive.kmp.android")
    id("ltthuc.primitive.kmp.ios")
    id("ltthuc.primitive.detekt")
}

val localProperties = Properties().apply {
    project.rootDir.resolve("local.properties").also {
        if (it.exists()) load(it.inputStream())
    }
}

val admobTestAppId = "ca-app-pub-0000000000000000~0000000000"
val bannerAdTestId = "ca-app-pub-3940256099942544/6300978111"
val interstitialAdTestId = "ca-app-pub-3940256099942544/1033173712"
val nativeAdTestId = "ca-app-pub-3940256099942544/2247696110"
val rewardAdTestId = "ca-app-pub-3940256099942544/5224354917"
val appOpenAdTestId = "ca-app-pub-3940256099942544/9257395921"

android {
    namespace = "me.ltthuc.kmp"

    signingConfigs {
        getByName("debug") {
            storeFile = file("${project.rootDir}/gradle/keystore/debug.keystore")
        }
        create("release") {
            storeFile = file("${project.rootDir}/gradle/keystore/release.keystore")
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS")
        }
        create("billing") {
            storeFile = file("${project.rootDir}/gradle/keystore/release.keystore")
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            versionNameSuffix = ".D"
            applicationIdSuffix = ".debug"
        }
        create("billing") {
            signingConfig = signingConfigs.getByName("billing")
            isDebuggable = true
            matchingFallbacks.add("debug")
        }
    }

    androidComponents {
        onVariants {
            val appName = when (it.buildType) {
                "debug" -> "Grabee Debug"
                "billing" -> "Grabee Billing"
                else -> null
            }

            it.manifestPlaceholders.apply {
                put("ADMOB_ANDROID_APP_ID", localProperties.getProperty("ADMOB_ANDROID_APP_ID") ?: admobTestAppId)
                put("ADMOB_IOS_APP_ID", localProperties.getProperty("ADMOB_IOS_APP_ID") ?: admobTestAppId)
            }

            if (appName != null) {
                it.resValues.apply {
                    put(it.makeResValueKey("string", "app_name"), ResValue(appName, null))
                }
            }

            if (it.buildType == "release") {
                it.packaging.resources.excludes.add("META-INF/**")
            }
        }
    }
}

tasks.register<Exec>("installAndLaunchDebug") {
    group = "install"
    description = "Install debug APK on the running emulator and launch the app."
    dependsOn("installDebug")
    val adbPath = "${android.sdkDirectory}/platform-tools/adb"
    // -e = target the running emulator only (ignore physical USB devices)
    commandLine(adbPath, "-e", "shell", "am", "start", "-n", "me.ltthuc.kmp.debug/me.ltthuc.kmp.MainActivity")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:datasource"))
            implementation(project(":core:repository"))
            implementation(project(":core:billing"))
            implementation(project(":core:audio"))
            implementation(project(":core:ui"))
            implementation(project(":core:resource"))

            implementation(project(":feature:home"))
            implementation(project(":feature:setting"))
            implementation(project(":feature:billing"))
            implementation(project(":feature:learningpath"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:review"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.play.review)
            implementation(libs.play.update)
            implementation(libs.koin.androidx.startup)
            implementation(libs.androidx.lifecycle.process)
        }
    }
}

buildkonfig {
    packageName = "me.ltthuc.kmp"

    defaultConfigs {
        fun setField(name: String, defaultValue: String = "") {
            val envValue = System.getenv(name)
            val propertyValue = localProperties.getProperty(name)

            buildConfigField(FieldSpec.Type.STRING, name, propertyValue ?: envValue ?: defaultValue)
        }

        setField("VERSION_NAME", libs.versions.versionName.get())
        setField("VERSION_CODE", libs.versions.versionCode.get())

        setField("DEVELOPER_PIN", "1234")
        setField("PURCHASE_ANDROID_API_KEY")
        setField("PURCHASE_IOS_API_KEY")

        setField("ADMOB_ANDROID_APP_ID", admobTestAppId)
        setField("ADMOB_ANDROID_BANNER_AD_UNIT_ID", bannerAdTestId)
        setField("ADMOB_ANDROID_INTERSTITIAL_AD_UNIT_ID", interstitialAdTestId)
        setField("ADMOB_ANDROID_NATIVE_AD_UNIT_ID", nativeAdTestId)
        setField("ADMOB_ANDROID_REWARDED_AD_UNIT_ID", rewardAdTestId)
        setField("ADMOB_ANDROID_APP_OPEN_AD_UNIT_ID", appOpenAdTestId)

        setField("ADMOB_IOS_APP_ID", admobTestAppId)
        setField("ADMOB_IOS_BANNER_AD_UNIT_ID", bannerAdTestId)
        setField("ADMOB_IOS_INTERSTITIAL_AD_UNIT_ID", interstitialAdTestId)
        setField("ADMOB_IOS_NATIVE_AD_UNIT_ID", nativeAdTestId)
        setField("ADMOB_IOS_REWARDED_AD_UNIT_ID", rewardAdTestId)
        setField("ADMOB_IOS_APP_OPEN_AD_UNIT_ID", appOpenAdTestId)

        setField("APPLOVIN_SDK_KEY")

        setField("FIREBASE_STORAGE_BUCKET", "grabee-368d2.firebasestorage.app")
    }
}
