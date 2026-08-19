plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.android.library.compose)
    alias(libs.plugins.citizendocs.android.screenshot)
}

android {
    namespace = "com.salpiras.citizendocs.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
}
