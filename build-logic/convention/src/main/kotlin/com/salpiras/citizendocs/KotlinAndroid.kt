package com.salpiras.citizendocs

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Shared Android + Kotlin configuration for every Android module in the build. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.versionInt("compileSdk")

        defaultConfig.apply {
            minSdk = libs.versionInt("minSdk")
        }

        // minSdk 26 gives us java.time natively, so no core library desugaring is needed.
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        testOptions.unitTests.apply {
            // Robolectric and Roborazzi both need the merged resources on the unit test classpath.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()
}

/** Shared configuration for the pure-JVM modules (`:core:model`, `:core:common`). */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Opt in per-developer with `warningsAsErrors=true` in ~/.gradle/gradle.properties, and in CI.
    val warningsAsErrors =
        providers
            .gradleProperty("warningsAsErrors")
            .map(String::toBoolean)
            .orElse(false)

    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> error("Unsupported project extension $this ${T::class}")
    }.apply {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = warningsAsErrors
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}
