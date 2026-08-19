import com.salpiras.citizendocs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose behaviour tests and Roborazzi screenshots, both on the JVM via Robolectric —
 * so `./gradlew testDebugUnitTest` covers the UI without an emulator.
 *
 * Goldens are written to `<module>/src/test/screenshots` by the tests themselves (they pass
 * an explicit path to `captureRoboImage`), which keeps them next to the code under test.
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "io.github.takahirom.roborazzi")

            dependencies {
                "testImplementation"(libs.findLibrary("robolectric").get())
                "testImplementation"(libs.findLibrary("roborazzi").get())
                "testImplementation"(libs.findLibrary("roborazzi-compose").get())
                "testImplementation"(libs.findLibrary("roborazzi-rule").get())
                "testImplementation"(libs.findLibrary("androidx-compose-ui-test").get())
                "testImplementation"(libs.findLibrary("androidx-test-ext").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-testManifest").get())
            }
        }
    }
}
