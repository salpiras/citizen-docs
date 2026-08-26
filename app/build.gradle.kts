plugins {
    alias(libs.plugins.citizendocs.android.application)
    alias(libs.plugins.citizendocs.android.application.compose)
    alias(libs.plugins.citizendocs.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.salpiras.citizendocs"

    defaultConfig {
        applicationId = "com.salpiras.citizendocs"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// The Baseline Profile plugin derives `nonMinifiedRelease` and `benchmarkRelease` from
// `release`, which has no signing config — and an unsigned APK cannot be installed on the
// device that has to run the profiling. Sign just those two with the debug key. `release`
// itself is deliberately left alone: a real release must not be debug-signed by accident.
afterEvaluate {
    val debugSigning = android.signingConfigs.getByName("debug")
    listOf("nonMinifiedRelease", "benchmarkRelease").forEach { name ->
        android.buildTypes.findByName(name)?.signingConfig = debugSigning
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:scanner"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":feature:documents"))
    implementation(project(":feature:scan"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)

    // Where the committed baseline profile comes from. This does not make :benchmarks part
    // of the app — it only tells the plugin which module regenerates the profile.
    baselineProfile(project(":benchmarks"))

    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}
