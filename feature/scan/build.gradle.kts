plugins {
    alias(libs.plugins.citizendocs.android.feature)
}

android {
    namespace = "com.salpiras.citizendocs.feature.scan"
}

dependencies {
    implementation(project(":core:scanner"))
}
