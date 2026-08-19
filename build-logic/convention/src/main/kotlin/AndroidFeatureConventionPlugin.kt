import com.salpiras.citizendocs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Everything a screen module needs, and nothing more.
 *
 * Note what is *absent*: `:core:database`, `:core:storage` and `:core:scanner`. A feature
 * module physically cannot import a Room entity or a `Uri` — the dependency graph enforces
 * the layering rather than relying on review to catch it.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "citizendocs.android.library")
            apply(plugin = "citizendocs.android.library.compose")
            apply(plugin = "citizendocs.android.screenshot")
            apply(plugin = "citizendocs.hilt")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "implementation"(project(":core:model"))
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:data"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:ui"))

                "implementation"(libs.findLibrary("androidx-compose-material3").get())
                "implementation"(libs.findLibrary("androidx-compose-material-iconsExtended").get())
                "implementation"(libs.findLibrary("androidx-hilt-navigation-compose").get())
                "implementation"(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-navigation-compose").get())
                "implementation"(libs.findLibrary("kotlinx-collections-immutable").get())
                "implementation"(libs.findLibrary("kotlinx-datetime").get())
                "implementation"(libs.findLibrary("kotlinx-serialization-json").get())

                "testImplementation"(project(":core:testing"))
            }
        }
    }
}
