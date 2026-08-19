plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.android.library.compose)
    alias(libs.plugins.citizendocs.hilt)
}

android {
    namespace = "com.salpiras.citizendocs.core.scanner"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.document.scanner)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext)
}
