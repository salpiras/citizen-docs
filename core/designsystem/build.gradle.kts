plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.android.library.compose)
    alias(libs.plugins.citizendocs.android.screenshot)
}

android {
    namespace = "com.salpiras.citizendocs.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.runtime)
}
