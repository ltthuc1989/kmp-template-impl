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
            isShrinkResources = true
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
    commandLine(adbPath, "-e", "shell", "am", "start", "-n", "com.beely.phonicskids.debug/me.ltthuc.kmp.MainActivity")
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

        // Must match composeApp/google-services.json — the Firebase SDK is configured for the
        // abc-phonics-kids project, so pointing content at any other bucket publishes into a
        // project the app does not use. (Was grabee-368d2, left over from the template.)
        val storageBucket = localProperties.getProperty("FIREBASE_STORAGE_BUCKET")
            ?: "abc-phonics-kids.firebasestorage.app"
        setField("FIREBASE_STORAGE_BUCKET", storageBucket)

        // Root of the downloadable content tree. Paths under it are `<hash>/<logical path>`
        // and never change once published, so any CDN works — point this at Cloudflare R2 or
        // a Cloud CDN host and nothing in the app has to change. The bucket/prefix must
        // allow public reads; see scripts/publish_content.py.
        setField("CONTENT_CDN_BASE_URL", "https://storage.googleapis.com/$storageBucket/content")
    }
}
