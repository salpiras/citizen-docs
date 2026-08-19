import com.salpiras.citizendocs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt via KSP — the old build used kapt for Hilt and KSP for Room, which meant paying for
 * two annotation processing backends on every compile.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                "ksp"(libs.findLibrary("hilt-compiler").get())
                "ksp"(libs.findLibrary("kotlin-metadata").get())
            }

            // Pure-JVM modules get hilt-core: the annotations, without the Android runtime.
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(libs.findLibrary("hilt-core").get())
                }
            }

            // Android modules additionally get the Hilt Gradle plugin and the Android runtime.
            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "dagger.hilt.android.plugin")
                dependencies {
                    "implementation"(libs.findLibrary("hilt-android").get())
                }
            }
        }
    }
}
