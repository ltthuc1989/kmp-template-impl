plugins {
    id("ltthuc.primitive.kmp.common")
    id("ltthuc.primitive.android.library")
    id("ltthuc.primitive.kmp.android")
    id("ltthuc.primitive.kmp.ios")
    id("ltthuc.primitive.detekt")
}

android {
    namespace = "me.ltthuc.kmp.core.audio"
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.common)
            implementation(libs.ktor.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.darwin)
        }

        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))

            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.core)

            api(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
