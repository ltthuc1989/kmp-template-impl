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
            implementation(project(":feature:download"))
            implementation(project(":feature:learningpath"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:review"))
        }

        androidMain.dependencies {
            // Pulled in transitively at 1.0.1, whose native library is not 16 KB compatible:
            // its RELRO segment is 1-byte aligned, so Android 15+ runs the whole app in page
            // size compatible mode and Play will reject the update. 1.1.0 is the fixed build.
            implementation(libs.androidx.graphics.path)
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

        // Root of the downloadable content tree, served straight off Cloud Storage. Paths under
        // it are `<hash>/<logical path>` and never change once published, so any host works —
        // point this at Cloudflare R2 or a Cloud CDN edge and nothing in the app changes.
        //
        // Bucket `abc-phonics-kids-content` (project abc-phonics-kids, asia-southeast1, closest
        // region to the launch markets) is public-read and holds nothing but lesson media. Deploy
        // with scripts/publish_content.py. Override in local.properties to test against a local
        // server: CONTENT_CDN_BASE_URL=http://10.0.2.2:8000/content
        setField(
            "CONTENT_CDN_BASE_URL",
            "https://storage.googleapis.com/abc-phonics-kids-content/content",
        )
    }
}
