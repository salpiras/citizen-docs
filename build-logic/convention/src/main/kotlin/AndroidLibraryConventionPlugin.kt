import com.android.build.api.dsl.LibraryExtension
import com.salpiras.citizendocs.configureKotlinAndroid
import com.salpiras.citizendocs.libs
import com.salpiras.citizendocs.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Baseline for every Android library module: SDK levels, Java/Kotlin target, and the
 * unit-testing stack that all of them share.
 */
abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // No kotlin-android plugin: AGP 9 compiles Kotlin itself (built-in Kotlin), and
            // applying org.jetbrains.kotlin.android alongside it fails the build.
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = libs.versionInt("targetSdk")
                lint.targetSdk = libs.versionInt("targetSdk")
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
            }

            // Modules like :core:designsystem and :core:testing carry no unit tests of their own;
            // Gradle 9 would otherwise fail their test task for discovering none.
            tasks.withType<Test>().configureEach {
                failOnNoDiscoveredTests.set(false)
            }

            dependencies {
                "testImplementation"(libs.findLibrary("junit4").get())
                "testImplementation"(libs.findLibrary("truth").get())
                "testImplementation"(libs.findLibrary("turbine").get())
                "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
