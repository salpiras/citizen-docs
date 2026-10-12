plugins {
    alias(libs.plugins.citizendocs.android.library)
    alias(libs.plugins.citizendocs.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.salpiras.citizendocs.core.drive"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    // await() on the Task that AuthorizationClient returns.
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.auth)

    // The client is tested against a real HTTP server on the JVM, so request shapes,
    // multipart bodies and error mapping are actually exercised rather than mocked.
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.junit4)
}
