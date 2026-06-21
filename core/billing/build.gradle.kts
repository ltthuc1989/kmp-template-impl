plugins {
    id("ltthuc.primitive.kmp.common")
    id("ltthuc.primitive.android.library")
    id("ltthuc.primitive.kmp.android")
    id("ltthuc.primitive.kmp.ios")
    id("ltthuc.primitive.detekt")
}

android {
    namespace = "me.ltthuc.kmp.core.billing"
    // Needed for BuildConfig.DEBUG (drives fake-vs-real billing per build type).
    buildFeatures.buildConfig = true
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))

            api(libs.bundles.purchase)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
