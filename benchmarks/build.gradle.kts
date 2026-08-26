plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.salpiras.citizendocs.benchmarks"

    compileSdk = libs.versions.compileSdk.get().toInt()

    // Macrobenchmark needs a real ART to measure, so this is the one module in the build
    // that cannot run on the JVM. 28 is the floor for baseline profile generation.
    defaultConfig {
        minSdk = 28
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The app under test. The Baseline Profile plugin uses this to work out which variant to
    // build, install and profile.
    targetProjectPath = ":app"
}

baselineProfile {
    // Emulators report timings that are far too noisy to *benchmark* on, but they are fine
    // for *recording* which classes and methods get used, which is all a baseline profile is.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.junit4)
}
