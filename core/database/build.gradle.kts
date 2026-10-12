plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.android.room)
    alias(libs.plugins.citizendocs.hilt)
}

android {
    namespace = "com.salpiras.citizendocs.core.database"
}

// MigrationTestHelper loads the committed schema JSONs through the instrumentation context's
// assets, so `schemas/` has to be an asset source for the unit tests — otherwise every
// migration test fails with "Cannot find the schema file" rather than with anything about
// the migration itself.
//
// Configured through the new `com.android.build.api.dsl.LibraryExtension` rather than inside
// the `android { }` block: under AGP 9 the Kotlin DSL accessor for `sourceSets` still
// resolves against the removed legacy source-set interface, and touching it fails
// configuration with a bare ClassCastException that names neither source sets nor this file.
extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
    sourceSets.named("test") {
        assets.directories += "$projectDir/schemas"
    }
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
