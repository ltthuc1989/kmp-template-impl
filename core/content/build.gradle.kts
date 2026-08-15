plugins {
    id("ltthuc.primitive.kmp.common")
    id("ltthuc.primitive.android.library")
    id("ltthuc.primitive.kmp.android")
    id("ltthuc.primitive.kmp.ios")
    id("ltthuc.primitive.detekt")
}

android {
    namespace = "me.ltthuc.kmp.core.content"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:resource"))

            implementation(libs.bundles.ktor)

            api(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
