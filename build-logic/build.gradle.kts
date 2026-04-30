plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.secret.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.build.konfig.gradlePlugin)
    implementation(libs.gms.services)
}

gradlePlugin {
    plugins {
        register("AndroidApplicationPlugin") {
            id = "ltthuc.primitive.android.application"
            implementationClass = "primitive.AndroidApplicationPlugin"
        }
        register("AndroidLibraryPlugin") {
            id = "ltthuc.primitive.android.library"
            implementationClass = "primitive.AndroidLibraryPlugin"
        }
        register("KmpPlugin") {
            id = "ltthuc.primitive.kmp.common"
            implementationClass = "primitive.KmpCommonPlugin"
        }
        register("KmpAndroidPlugin") {
            id = "ltthuc.primitive.kmp.android"
            implementationClass = "primitive.KmpAndroidPlugin"
        }
        register("KmpAndroidCompose") {
            id = "ltthuc.primitive.kmp.compose"
            implementationClass = "primitive.KmpComposePlugin"
        }
        register("KmpIosPlugin") {
            id = "ltthuc.primitive.kmp.ios"
            implementationClass = "primitive.KmpIosPlugin"
        }
        register("DetektPlugin") {
            id = "ltthuc.primitive.detekt"
            implementationClass = "primitive.DetektPlugin"
        }
    }
}
