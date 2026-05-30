plugins {
    id("ltthuc.primitive.kmp.common")
    id("ltthuc.primitive.android.library")
    id("ltthuc.primitive.kmp.compose")
    id("ltthuc.primitive.kmp.android")
    id("ltthuc.primitive.kmp.ios")
    id("ltthuc.primitive.detekt")
}

android {
    namespace = "me.ltthuc.kmp.core.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(project(":core:repository"))
            implementation(project(":core:datasource"))
            implementation(project(":core:resource"))

            api(libs.bundles.ui.common)
            api(libs.bundles.compose)
            api(libs.bundles.calf)

            api(libs.adaptive)
            api(libs.adaptive.layout)
        }

        androidMain.dependencies {
            api(libs.bundles.ui.android)
        }
    }
}
