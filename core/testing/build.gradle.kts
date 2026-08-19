plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.android.library.compose)
}

android {
    namespace = "com.salpiras.citizendocs.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:data"))
    api(project(":core:database"))
    api(project(":core:storage"))
    api(libs.junit4)
    api(libs.truth)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.compose.ui.test)
}
