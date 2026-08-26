package com.salpiras.citizendocs

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Turns on Compose and wires the BOM so no module ever pins a Compose version by hand. */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.compose = true

        dependencies {
            val bom = libs.findLibrary("androidx-compose-bom").get()
            "implementation"(platform(bom))
            "testImplementation"(platform(bom))
            "androidTestImplementation"(platform(bom))

            "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }

    configureComposeMetrics()
}

/**
 * Opt-in Compose compiler metrics: `./gradlew assembleDebug -PcomposeMetrics=true`.
 *
 * The reports land in `<module>/build/compose-metrics` and `<module>/build/compose-reports`,
 * and say for each composable whether it is skippable and restartable, and which of its
 * parameters the compiler considers unstable.
 *
 * Off by default because generating them slows every Compose compilation down, and because
 * the answer only matters when something is actually janking. But it is the difference
 * between knowing that a composable skips and assuming it does — which, in a UI that now
 * animates, is the difference worth having.
 */
private fun Project.configureComposeMetrics() {
    val enabled =
        providers
            .gradleProperty("composeMetrics")
            .map(String::toBoolean)
            .orElse(false)

    if (!enabled.get()) return

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        metricsDestination = layout.buildDirectory.dir("compose-metrics")
        reportsDestination = layout.buildDirectory.dir("compose-reports")
    }
}
