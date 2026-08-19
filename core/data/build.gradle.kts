plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.hilt)
}

android {
    namespace = "com.salpiras.citizendocs.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:storage"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    testImplementation(project(":core:testing"))
}
