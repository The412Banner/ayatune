plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "banner.tune"
    compileSdk = 35

    defaultConfig {
        applicationId = "banner.tune"
        minSdk = 31              // matches AYANEO Settings minSdk; lets us use modern overlay APIs
        targetSdk = 34           // matches the Pocket FIT system image
        versionCode = 1
        versionName = "0.0.1"
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    // Single source of truth for the AYANEO AIDL stubs: the top-level /aidl
    // directory at the repo root. Re-exposed here so the AGP AIDL compiler
    // picks them up.
    sourceSets {
        getByName("main") {
            aidl.srcDirs("src/main/aidl", "../aidl")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // No signing config yet — CI assembles unsigned release; we sign
            // separately or use a debug keystore for early test installs.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.libsu.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
