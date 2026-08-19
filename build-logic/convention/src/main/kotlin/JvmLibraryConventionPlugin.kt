import com.salpiras.citizendocs.configureKotlinJvm
import com.salpiras.citizendocs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * For modules with no Android dependency at all (`:core:model`, `:core:common`).
 * Keeping them off the Android plugin makes their tests plain, fast JVM tests.
 */
abstract class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")

            configureKotlinJvm()

            tasks.withType<Test>().configureEach {
                failOnNoDiscoveredTests.set(false)
            }

            dependencies {
                "testImplementation"(libs.findLibrary("junit4").get())
                "testImplementation"(libs.findLibrary("truth").get())
                "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
