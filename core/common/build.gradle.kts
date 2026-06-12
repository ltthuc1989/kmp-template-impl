plugins {
    id("ltthuc.primitive.kmp.common")
    id("ltthuc.primitive.android.library")
    id("ltthuc.primitive.kmp.android")
    id("ltthuc.primitive.kmp.ios")
    id("ltthuc.primitive.detekt")
}

android {
    namespace = "me.ltthuc.kmp.core.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project.dependencies.platform(libs.koin.bom))

            api(libs.bundles.infra)
            api(libs.bundles.koin)
        }

        androidMain.dependencies {
            api(project.dependencies.platform(libs.firebase.bom))

            api(libs.bundles.firebase)
            api(libs.koin.android)
            implementation(libs.play.review)
        }
    }
}
