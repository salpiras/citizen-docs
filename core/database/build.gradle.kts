plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.android.room)
    alias(libs.plugins.citizendocs.hilt)
}

android {
    namespace = "com.salpiras.citizendocs.core.database"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    // The DAO is tested against real SQLite via Robolectric rather than a fake, so the
    // schema, converters and unique index are actually exercised.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext)
}
