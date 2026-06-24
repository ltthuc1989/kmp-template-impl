package me.ltthuc.kmp

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

fun Project.androidApplication(action: BaseAppModuleExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.androidLibrary(action: LibraryExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.android(action: TestedExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.setupAndroid() {
    android {
        defaultConfig {
            targetSdk = libs.version("targetSdk").toInt()
            minSdk = libs.version("minSdk").toInt()

            javaCompileOptions {
                annotationProcessorOptions {
                    arguments += mapOf(
                        "room.schemaLocation" to "$projectDir/schemas",
                        "room.incremental" to "true"
                    )
                }
            }

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        // ABI splits produce multiple APKs, which conflicts with building an App Bundle
        // (https://issuetracker.google.com/402800800). Disable splits for bundle tasks so
        // `bundleRelease` works; APK builds (`assemble*`) keep per-ABI + universal APKs.
        val buildingBundle = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }

        splits {
            abi {
                isEnable = !buildingBundle
                isUniversalApk = !buildingBundle

                reset()
                include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        dependencies {
            add("coreLibraryDesugaring", libs.library("desugar"))
        }
    }
}
