plugins {
    id("ltthuc.primitive.kmp.common")
    id("ltthuc.primitive.android.library")
    id("ltthuc.primitive.kmp.android")
    id("ltthuc.primitive.kmp.ios")
    id("ltthuc.primitive.detekt")
}

android {
    namespace = "me.ltthuc.kmp.core.repository"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(project(":core:datasource"))
            implementation(project(":core:billing"))
            implementation(project(":core:audio"))
            api(project(":core:content"))
            implementation(project(":core:resource"))

            implementation(libs.bundles.ktor)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            api(libs.ktor.okhttp)
        }

        iosMain.dependencies {
            api(libs.ktor.darwin)
        }
    }
}
