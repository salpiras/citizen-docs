// An Android library rather than a JVM one only because :core:data transitively pulls in
// Room. The use cases themselves touch no Android APIs, so their tests are plain JVM tests
// with no Robolectric.
plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.hilt)
}

android {
    namespace = "com.salpiras.citizendocs.core.domain"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:data"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    testImplementation(project(":core:testing"))
}
