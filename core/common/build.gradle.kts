plugins {
    alias(libs.plugins.citizendocs.jvm.library)
    alias(libs.plugins.citizendocs.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
}
