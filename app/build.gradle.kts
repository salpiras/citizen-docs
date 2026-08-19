plugins {
    alias(libs.plugins.citizendocs.android.application)
    alias(libs.plugins.citizendocs.android.application.compose)
    alias(libs.plugins.citizendocs.hilt)
    alias(libs.plugins.kotlin.serialization)
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

    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}
